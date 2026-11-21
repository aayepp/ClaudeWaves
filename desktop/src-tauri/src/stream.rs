//! Progressive download of a stream into the audio cache, with a reader that
//! lets the decoder start before the download finishes. Finished files stay in
//! the cache, so a song played twice is read from disk the second time.

use std::fs::{self, File, OpenOptions};
use std::io::{self, Read, Seek, SeekFrom, Write};
use std::path::{Path, PathBuf};
use std::sync::atomic::{AtomicBool, AtomicU64, Ordering};
use std::sync::{Arc, Mutex};
use std::thread;
use std::time::Duration;

use symphonia::core::io::MediaSource;

use crate::ytdlp::Resolved;

/// Range requests of this size avoid the throttling YouTube applies to a
/// single long download.
const CHUNK: u64 = 4 * 1024 * 1024;
/// Keep the cache under this size; oldest files go first.
const CACHE_LIMIT: u64 = 2 * 1024 * 1024 * 1024;

#[derive(Default)]
pub struct Progress {
    pub downloaded: AtomicU64,
    pub total: AtomicU64,
    pub done: AtomicBool,
    pub cancelled: AtomicBool,
    pub error: Mutex<Option<String>>,
}

pub struct Download {
    pub path: PathBuf,
    pub progress: Arc<Progress>,
}

pub fn cache_paths(dir: &Path, video_id: &str, ext: &str) -> (PathBuf, PathBuf) {
    (dir.join(format!("{video_id}.{ext}")), dir.join(format!("{video_id}.done")))
}

/// A finished cached file for this video, if there is one.
pub fn cached(dir: &Path, video_id: &str) -> Option<PathBuf> {
    if !dir.join(format!("{video_id}.done")).is_file() {
        return None;
    }
    ["m4a", "mp4", "webm", "opus"]
        .iter()
        .map(|e| dir.join(format!("{video_id}.{e}")))
        .find(|p| p.is_file())
}

/// Starts downloading `r` into the cache on a background thread.
pub fn start(dir: &Path, video_id: &str, r: &Resolved) -> Result<Download, String> {
    fs::create_dir_all(dir).map_err(|e| e.to_string())?;
    let (path, done_marker) = cache_paths(dir, video_id, &r.ext);
    let _ = fs::remove_file(&done_marker);
    File::create(&path).map_err(|e| format!("Cache write failed: {e}"))?;
    let progress = Arc::new(Progress::default());
    if let Some(size) = r.size {
        progress.total.store(size, Ordering::SeqCst);
    }
    let p2 = progress.clone();
    let path2 = path.clone();
    let url = r.url.clone();
    let headers = r.headers.clone();
    let dir2 = dir.to_path_buf();
    thread::Builder::new()
        .name("stream-download".into())
        .spawn(move || {
            let res = download(&url, &headers, &path2, &p2);
            match res {
                Ok(()) => {
                    let _ = fs::write(&done_marker, b"");
                    p2.done.store(true, Ordering::SeqCst);
                    trim_cache(&dir2);
                }
                Err(e) => {
                    if let Ok(mut g) = p2.error.lock() {
                        *g = Some(e);
                    }
                    p2.done.store(true, Ordering::SeqCst);
                }
            }
        })
        .map_err(|e| e.to_string())?;
    Ok(Download { path, progress })
}

fn download(url: &str, headers: &[(String, String)], path: &Path, p: &Progress) -> Result<(), String> {
    let client = reqwest::blocking::Client::builder()
        .timeout(Duration::from_secs(30))
        .build()
        .map_err(|e| e.to_string())?;
    let mut file = OpenOptions::new().write(true).open(path).map_err(|e| e.to_string())?;
    let mut offset = 0u64;
    let mut buf = vec![0u8; 64 * 1024];
    loop {
        if p.cancelled.load(Ordering::Relaxed) {
            return Err("cancelled".into());
        }
        let end = offset + CHUNK - 1;
        let mut req = client.get(url).header("Range", format!("bytes={offset}-{end}"));
        for (k, v) in headers {
            req = req.header(k.as_str(), v.as_str());
        }
        let mut res = req.send().map_err(|e| format!("Stream error: {e}"))?;
        let status = res.status();
        if status == reqwest::StatusCode::RANGE_NOT_SATISFIABLE {
            break;
        }
        if !status.is_success() {
            return Err(format!("Stream refused ({status})"));
        }
        if let Some(total) = res
            .headers()
            .get("content-range")
            .and_then(|v| v.to_str().ok())
            .and_then(|v| v.rsplit('/').next())
            .and_then(|t| t.parse::<u64>().ok())
        {
            p.total.store(total, Ordering::SeqCst);
        }
        let mut got = 0u64;
        loop {
            let n = res.read(&mut buf).map_err(|e| format!("Stream error: {e}"))?;
            if n == 0 {
                break;
            }
            file.write_all(&buf[..n]).map_err(|e| e.to_string())?;
            got += n as u64;
            offset += n as u64;
            p.downloaded.store(offset, Ordering::Release);
            if p.cancelled.load(Ordering::Relaxed) {
                return Err("cancelled".into());
            }
        }
        let total = p.total.load(Ordering::SeqCst);
        if got == 0 || (total > 0 && offset >= total) || status == reqwest::StatusCode::OK {
            break;
        }
    }
    file.flush().map_err(|e| e.to_string())?;
    let total = p.total.load(Ordering::SeqCst);
    if total == 0 || offset < total {
        p.total.store(offset, Ordering::SeqCst);
    }
    Ok(())
}

fn trim_cache(dir: &Path) {
    let Ok(entries) = fs::read_dir(dir) else { return };
    let mut files: Vec<(std::time::SystemTime, u64, PathBuf)> = entries
        .filter_map(Result::ok)
        .filter_map(|e| {
            let m = e.metadata().ok()?;
            let p = e.path();
            (m.is_file() && p.extension().map(|x| x != "done").unwrap_or(false))
                .then(|| (m.modified().ok().unwrap_or(std::time::UNIX_EPOCH), m.len(), p))
        })
        .collect();
    let mut total: u64 = files.iter().map(|f| f.1).sum();
    files.sort_by_key(|f| f.0);
    for (_, len, path) in files {
        if total <= CACHE_LIMIT {
            break;
        }
        if let Some(stem) = path.file_stem() {
            let _ = fs::remove_file(path.with_file_name(format!("{}.done", stem.to_string_lossy())));
        }
        if fs::remove_file(&path).is_ok() {
            total = total.saturating_sub(len);
        }
    }
}

/// Reads a file that another thread is still writing. Blocks (briefly
/// sleeping) when the decoder catches up with the download.
pub struct GrowingReader {
    file: File,
    pos: u64,
    progress: Arc<Progress>,
}

impl GrowingReader {
    pub fn new(path: &Path, progress: Arc<Progress>) -> io::Result<Self> {
        Ok(Self { file: File::open(path)?, pos: 0, progress })
    }

    fn wait_for(&self, until: u64) -> io::Result<()> {
        loop {
            if self.progress.downloaded.load(Ordering::Acquire) >= until {
                return Ok(());
            }
            if self.progress.done.load(Ordering::Acquire) {
                if let Some(e) = self.progress.error.lock().ok().and_then(|g| g.clone()) {
                    return Err(io::Error::new(io::ErrorKind::Other, e));
                }
                return Ok(());
            }
            thread::sleep(Duration::from_millis(15));
        }
    }
}

impl Read for GrowingReader {
    fn read(&mut self, buf: &mut [u8]) -> io::Result<usize> {
        if buf.is_empty() {
            return Ok(0);
        }
        self.wait_for(self.pos + 1)?;
        let avail = self.progress.downloaded.load(Ordering::Acquire).saturating_sub(self.pos);
        if avail == 0 {
            return Ok(0);
        }
        let n = (buf.len() as u64).min(avail) as usize;
        self.file.seek(SeekFrom::Start(self.pos))?;
        let read = self.file.read(&mut buf[..n])?;
        self.pos += read as u64;
        Ok(read)
    }
}

impl Seek for GrowingReader {
    fn seek(&mut self, to: SeekFrom) -> io::Result<u64> {
        let total = self.progress.total.load(Ordering::Acquire);
        let target = match to {
            SeekFrom::Start(n) => n as i64,
            SeekFrom::Current(d) => self.pos as i64 + d,
            SeekFrom::End(d) => {
                if total == 0 {
                    self.wait_for(u64::MAX)?;
                }
                self.progress.total.load(Ordering::Acquire) as i64 + d
            }
        };
        if target < 0 {
            return Err(io::Error::new(io::ErrorKind::InvalidInput, "seek before start"));
        }
        self.pos = target as u64;
        Ok(self.pos)
    }
}

impl MediaSource for GrowingReader {
    fn is_seekable(&self) -> bool {
        true
    }

    fn byte_len(&self) -> Option<u64> {
        match self.progress.total.load(Ordering::Acquire) {
            0 => None,
            n => Some(n),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn growing_reader_waits_for_writer() {
        let path = std::env::temp_dir().join("cw_growing_test.bin");
        let mut f = File::create(&path).unwrap();
        let progress = Arc::new(Progress::default());
        progress.total.store(8, Ordering::SeqCst);
        let mut reader = GrowingReader::new(&path, progress.clone()).unwrap();

        let p2 = progress.clone();
        let writer = thread::spawn(move || {
            f.write_all(b"abcd").unwrap();
            f.flush().unwrap();
            p2.downloaded.store(4, Ordering::SeqCst);
            thread::sleep(Duration::from_millis(60));
            f.write_all(b"efgh").unwrap();
            f.flush().unwrap();
            p2.downloaded.store(8, Ordering::SeqCst);
            p2.done.store(true, Ordering::SeqCst);
        });

        let mut out = Vec::new();
        reader.read_to_end(&mut out).unwrap();
        writer.join().unwrap();
        assert_eq!(out, b"abcdefgh");
        assert_eq!(reader.seek(SeekFrom::End(-2)).unwrap(), 6);
        let mut two = [0u8; 2];
        reader.read_exact(&mut two).unwrap();
        assert_eq!(&two, b"gh");
    }
}
