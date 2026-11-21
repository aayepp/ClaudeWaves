//! Audio stream URLs for YouTube Music tracks come from yt-dlp, run as a
//! separate program. The app downloads the official yt-dlp release on first
//! use into its data folder and keeps it updated, because YouTube changes its
//! player often and yt-dlp is where those changes get handled.

use std::collections::HashMap;
use std::path::PathBuf;
use std::process::Command;
use std::sync::Mutex;
use std::time::{Duration, Instant, SystemTime};

use serde::{Deserialize, Serialize};

#[cfg(windows)]
const ASSET: &str = "yt-dlp.exe";
#[cfg(target_os = "macos")]
const ASSET: &str = "yt-dlp_macos";
#[cfg(all(unix, not(target_os = "macos")))]
const ASSET: &str = "yt-dlp_linux";

const RELEASE_URL: &str = "https://github.com/yt-dlp/yt-dlp/releases/latest/download/";

/// yt-dlp needs a JavaScript runtime to solve YouTube's player challenges;
/// Deno is the one it recommends and ships as a single executable.
#[cfg(windows)]
const DENO_ASSET: &str = "deno-x86_64-pc-windows-msvc.zip";
#[cfg(all(target_os = "macos", target_arch = "aarch64"))]
const DENO_ASSET: &str = "deno-aarch64-apple-darwin.zip";
#[cfg(all(target_os = "macos", not(target_arch = "aarch64")))]
const DENO_ASSET: &str = "deno-x86_64-apple-darwin.zip";
#[cfg(all(unix, not(target_os = "macos")))]
const DENO_ASSET: &str = "deno-x86_64-unknown-linux-gnu.zip";
const DENO_URL: &str = "https://github.com/denoland/deno/releases/latest/download/";
/// Stream URLs from YouTube stay valid for about six hours; reuse for five.
const URL_TTL: Duration = Duration::from_secs(5 * 3600);
/// Run `yt-dlp -U` at most this often.
const UPDATE_EVERY: Duration = Duration::from_secs(3 * 24 * 3600);

#[derive(Clone, Debug)]
pub struct Resolved {
    pub url: String,
    pub headers: Vec<(String, String)>,
    pub ext: String,
    pub size: Option<u64>,
    pub codec: String,
    pub bitrate: Option<f64>,
    pub sample_rate: Option<u32>,
    pub duration: Option<f64>,
}

#[derive(Serialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct Status {
    pub installed: bool,
    pub version: Option<String>,
    pub path: String,
    pub js_runtime: bool,
}

#[derive(Deserialize)]
struct Info {
    url: Option<String>,
    ext: Option<String>,
    filesize: Option<u64>,
    filesize_approx: Option<u64>,
    acodec: Option<String>,
    abr: Option<f64>,
    asr: Option<u32>,
    duration: Option<f64>,
    http_headers: Option<HashMap<String, String>>,
    requested_formats: Option<Vec<Info>>,
}

pub struct YtDlp {
    dir: PathBuf,
    cache: Mutex<HashMap<String, (Instant, Resolved)>>,
    install_lock: Mutex<()>,
}

impl YtDlp {
    pub fn new(data_dir: PathBuf) -> Self {
        let dir = data_dir.join("bin");
        let _ = std::fs::create_dir_all(&dir);
        Self { dir, cache: Mutex::new(HashMap::new()), install_lock: Mutex::new(()) }
    }

    fn deno_path(&self) -> PathBuf {
        self.dir.join(if cfg!(windows) { "deno.exe" } else { "deno" })
    }

    /// The JS runtime for yt-dlp: the app's own Deno, else one on PATH.
    fn js_runtime(&self) -> Option<PathBuf> {
        let own = self.deno_path();
        if own.is_file() {
            return Some(own);
        }
        let which = if cfg!(windows) { "where" } else { "which" };
        let out = command(&PathBuf::from(which)).arg("deno").output().ok()?;
        let p = String::from_utf8_lossy(&out.stdout).lines().next()?.trim().to_string();
        (out.status.success() && !p.is_empty()).then(|| PathBuf::from(p))
    }

    /// Downloads Deno next to yt-dlp. Failure is not fatal: yt-dlp can still
    /// work for some videos without it.
    pub fn ensure_js_runtime(&self) -> Result<PathBuf, String> {
        if let Some(p) = self.js_runtime() {
            return Ok(p);
        }
        let client = reqwest::blocking::Client::builder()
            .timeout(Duration::from_secs(600))
            .build()
            .map_err(|e| e.to_string())?;
        let bytes = client
            .get(format!("{DENO_URL}{DENO_ASSET}"))
            .send()
            .and_then(|r| r.error_for_status())
            .and_then(|r| r.bytes())
            .map_err(|e| format!("Could not download Deno: {e}"))?;
        let mut zip = zip::ZipArchive::new(std::io::Cursor::new(bytes)).map_err(|e| e.to_string())?;
        let name = if cfg!(windows) { "deno.exe" } else { "deno" };
        let mut entry = zip.by_name(name).map_err(|e| format!("Deno archive: {e}"))?;
        let target = self.deno_path();
        let tmp = target.with_extension("download");
        let mut out = std::fs::File::create(&tmp).map_err(|e| e.to_string())?;
        std::io::copy(&mut entry, &mut out).map_err(|e| e.to_string())?;
        drop(out);
        #[cfg(unix)]
        {
            use std::os::unix::fs::PermissionsExt;
            let _ = std::fs::set_permissions(&tmp, std::fs::Permissions::from_mode(0o755));
        }
        std::fs::rename(&tmp, &target).map_err(|e| e.to_string())?;
        Ok(target)
    }

    fn bundled(&self) -> PathBuf {
        self.dir.join(if cfg!(windows) { "yt-dlp.exe" } else { "yt-dlp" })
    }

    /// The yt-dlp to run: the app's own copy, else one on PATH (Linux users
    /// often have it from their package manager).
    fn program(&self) -> Option<PathBuf> {
        let own = self.bundled();
        if own.is_file() {
            return Some(own);
        }
        if cfg!(unix) {
            if let Ok(out) = Command::new("which").arg("yt-dlp").output() {
                let p = String::from_utf8_lossy(&out.stdout).trim().to_string();
                if out.status.success() && !p.is_empty() {
                    return Some(PathBuf::from(p));
                }
            }
        }
        None
    }

    pub fn status(&self) -> Status {
        let program = self.program();
        let version = program.as_ref().and_then(|p| {
            let out = command(p).arg("--version").output().ok()?;
            out.status.success().then(|| String::from_utf8_lossy(&out.stdout).trim().to_string())
        });
        Status {
            installed: version.is_some(),
            version,
            path: program.unwrap_or_else(|| self.bundled()).to_string_lossy().into_owned(),
            js_runtime: self.js_runtime().is_some(),
        }
    }

    /// Downloads the yt-dlp release binary if there is none yet.
    pub fn ensure_installed(&self) -> Result<PathBuf, String> {
        if let Some(p) = self.program() {
            return Ok(p);
        }
        let _guard = self.install_lock.lock().map_err(|e| e.to_string())?;
        if let Some(p) = self.program() {
            return Ok(p);
        }
        let target = self.bundled();
        let tmp = target.with_extension("download");
        let client = reqwest::blocking::Client::builder()
            .timeout(Duration::from_secs(300))
            .build()
            .map_err(|e| e.to_string())?;
        let mut res = client
            .get(format!("{RELEASE_URL}{ASSET}"))
            .send()
            .and_then(|r| r.error_for_status())
            .map_err(|e| format!("Could not download yt-dlp: {e}"))?;
        let mut file = std::fs::File::create(&tmp).map_err(|e| e.to_string())?;
        std::io::copy(&mut res, &mut file).map_err(|e| format!("Could not save yt-dlp: {e}"))?;
        drop(file);
        #[cfg(unix)]
        {
            use std::os::unix::fs::PermissionsExt;
            let _ = std::fs::set_permissions(&tmp, std::fs::Permissions::from_mode(0o755));
        }
        std::fs::rename(&tmp, &target).map_err(|e| e.to_string())?;
        let _ = std::fs::write(self.dir.join("yt-dlp.updated"), b"");
        Ok(target)
    }

    /// Self-update the app's own copy every few days. Quiet on failure.
    pub fn maybe_update(&self) {
        let own = self.bundled();
        if !own.is_file() {
            return;
        }
        let stamp = self.dir.join("yt-dlp.updated");
        let fresh = std::fs::metadata(&stamp)
            .and_then(|m| m.modified())
            .ok()
            .and_then(|t| SystemTime::now().duration_since(t).ok())
            .map(|age| age < UPDATE_EVERY)
            .unwrap_or(false);
        if fresh {
            return;
        }
        let ok = command(&own).arg("-U").output().map(|o| o.status.success()).unwrap_or(false);
        if ok {
            let _ = std::fs::write(&stamp, b"");
        }
    }

    /// Finds an audio stream the engine can decode (AAC in MP4 first).
    pub fn resolve(&self, video_id: &str) -> Result<Resolved, String> {
        if let Ok(cache) = self.cache.lock() {
            if let Some((at, r)) = cache.get(video_id) {
                if at.elapsed() < URL_TTL {
                    return Ok(r.clone());
                }
            }
        }
        let program = self.ensure_installed()?;
        let url = format!("https://music.youtube.com/watch?v={video_id}");
        let mut cmd = command(&program);
        if let Some(js) = self.js_runtime() {
            cmd.arg("--js-runtimes").arg(format!("deno:{}", js.to_string_lossy()));
        }
        let out = cmd
            .args([
                "--no-playlist",
                "--no-warnings",
                "--no-progress",
                "-f",
                "bestaudio[ext=m4a]/bestaudio[acodec^=mp4a]/bestaudio[acodec=opus]/bestaudio",
                "-J",
                &url,
            ])
            .output()
            .map_err(|e| format!("Could not run yt-dlp: {e}"))?;
        if !out.status.success() {
            let err = String::from_utf8_lossy(&out.stderr);
            let line = err.lines().rev().find(|l| l.contains("ERROR")).unwrap_or(err.trim());
            return Err(format!("yt-dlp: {}", line.trim()));
        }
        let info: Info = serde_json::from_slice(&out.stdout).map_err(|e| format!("yt-dlp output: {e}"))?;
        let info = match info.requested_formats {
            Some(mut f) if info.url.is_none() && !f.is_empty() => f.remove(0),
            _ => info,
        };
        let resolved = Resolved {
            url: info.url.ok_or("yt-dlp returned no stream URL")?,
            headers: info.http_headers.unwrap_or_default().into_iter().collect(),
            ext: info.ext.unwrap_or_else(|| "m4a".into()),
            size: info.filesize.or(info.filesize_approx),
            codec: info.acodec.unwrap_or_default(),
            bitrate: info.abr,
            sample_rate: info.asr,
            duration: info.duration,
        };
        if let Ok(mut cache) = self.cache.lock() {
            cache.insert(video_id.to_string(), (Instant::now(), resolved.clone()));
        }
        Ok(resolved)
    }

    pub fn forget(&self, video_id: &str) {
        if let Ok(mut cache) = self.cache.lock() {
            cache.remove(video_id);
        }
    }
}

fn command(program: &PathBuf) -> Command {
    #[allow(unused_mut)]
    let mut c = Command::new(program);
    #[cfg(windows)]
    {
        use std::os::windows::process::CommandExt;
        // CREATE_NO_WINDOW: no console flashing up for each lookup.
        c.creation_flags(0x0800_0000);
    }
    c
}
