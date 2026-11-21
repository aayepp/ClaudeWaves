//! Local music library: scans folders, reads tags with lofty, keeps a JSON
//! cache in the app data folder so start-up does not rescan.

use std::collections::HashSet;
use std::fs;
use std::path::{Path, PathBuf};
use std::sync::Mutex;
use std::time::UNIX_EPOCH;

use base64::Engine as _;
use lofty::file::{AudioFile, TaggedFileExt};
use lofty::prelude::{Accessor, ItemKey};
use lofty::tag::Tag;
use serde::{Deserialize, Serialize};
use tauri::{AppHandle, Emitter, Manager};
use walkdir::WalkDir;

pub const AUDIO_EXTENSIONS: &[&str] = &["mp3", "flac", "wav", "ogg", "oga", "m4a", "aac", "aif", "aiff", "alac"];

#[derive(Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Track {
    pub path: String,
    pub title: String,
    pub artist: String,
    pub album: String,
    pub album_artist: String,
    pub duration: f64,
    pub track_no: Option<u32>,
    pub disc_no: Option<u32>,
    pub year: Option<u32>,
    pub format: String,
    pub sample_rate: Option<u32>,
    pub bit_depth: Option<u8>,
    pub bitrate: Option<u32>,
    pub has_cover: bool,
    pub added: u64,
}

#[derive(Clone, Serialize, Deserialize, Default)]
#[serde(rename_all = "camelCase")]
pub struct Library {
    pub folders: Vec<String>,
    pub tracks: Vec<Track>,
}

pub struct LibraryState {
    file: PathBuf,
    pub data: Mutex<Library>,
}

impl LibraryState {
    pub fn load(app: &AppHandle) -> Self {
        let dir = app.path().app_data_dir().unwrap_or_else(|_| std::env::temp_dir().join("claudewaves"));
        let _ = fs::create_dir_all(&dir);
        let file = dir.join("library.json");
        let data = fs::read(&file)
            .ok()
            .and_then(|b| serde_json::from_slice::<Library>(&b).ok())
            .unwrap_or_default();
        Self { file, data: Mutex::new(data) }
    }

    pub fn save(&self) -> Result<(), String> {
        let data = self.data.lock().map_err(|e| e.to_string())?;
        let json = serde_json::to_vec(&*data).map_err(|e| e.to_string())?;
        let tmp = self.file.with_extension("json.tmp");
        fs::write(&tmp, json).map_err(|e| e.to_string())?;
        fs::rename(&tmp, &self.file).map_err(|e| e.to_string())
    }
}

fn is_audio(path: &Path) -> bool {
    path.extension()
        .and_then(|e| e.to_str())
        .map(|e| AUDIO_EXTENSIONS.contains(&e.to_ascii_lowercase().as_str()))
        .unwrap_or(false)
}

fn file_stem(path: &Path) -> String {
    path.file_stem().and_then(|s| s.to_str()).unwrap_or("Unknown").to_string()
}

fn primary_tag(file: &lofty::file::TaggedFile) -> Option<&Tag> {
    file.primary_tag().or_else(|| file.first_tag())
}

pub fn read_track(path: &Path) -> Option<Track> {
    let added = fs::metadata(path)
        .and_then(|m| m.modified())
        .ok()
        .and_then(|t| t.duration_since(UNIX_EPOCH).ok())
        .map(|d| d.as_secs())
        .unwrap_or(0);
    let ext = path.extension().and_then(|e| e.to_str()).unwrap_or("").to_ascii_lowercase();
    let format = match ext.as_str() {
        "oga" | "ogg" => "OGG".to_string(),
        "aif" | "aiff" => "AIFF".to_string(),
        other => other.to_ascii_uppercase(),
    };

    let tagged = match lofty::read_from_path(path) {
        Ok(t) => t,
        Err(_) => {
            // Unreadable tags: keep the file with its name so it still plays.
            return Some(Track {
                path: path.to_string_lossy().into_owned(),
                title: file_stem(path),
                artist: "Unknown artist".into(),
                album: "Unknown album".into(),
                album_artist: String::new(),
                duration: 0.0,
                track_no: None,
                disc_no: None,
                year: None,
                format,
                sample_rate: None,
                bit_depth: None,
                bitrate: None,
                has_cover: false,
                added,
            });
        }
    };
    let props = tagged.properties();
    let tag = primary_tag(&tagged);
    let get = |f: fn(&Tag) -> Option<std::borrow::Cow<'_, str>>| tag.and_then(|t| f(t)).map(|s| s.trim().to_string()).filter(|s| !s.is_empty());

    let title = get(|t| t.title()).unwrap_or_else(|| file_stem(path));
    let artist = get(|t| t.artist()).unwrap_or_else(|| "Unknown artist".into());
    let album = get(|t| t.album()).unwrap_or_else(|| "Unknown album".into());
    let album_artist = tag
        .and_then(|t| t.get_string(&ItemKey::AlbumArtist))
        .map(|s| s.trim().to_string())
        .unwrap_or_default();
    let has_cover = tag.map(|t| !t.pictures().is_empty()).unwrap_or(false) || folder_cover(path).is_some();

    Some(Track {
        path: path.to_string_lossy().into_owned(),
        title,
        artist,
        album,
        album_artist,
        duration: props.duration().as_secs_f64(),
        track_no: tag.and_then(|t| t.track()),
        disc_no: tag.and_then(|t| t.disk()),
        year: tag.and_then(|t| t.year()),
        format,
        sample_rate: props.sample_rate(),
        bit_depth: props.bit_depth(),
        bitrate: props.audio_bitrate(),
        has_cover,
        added,
    })
}

/// Scans `folders` and returns every playable file found.
pub fn scan(app: &AppHandle, folders: &[String]) -> Vec<Track> {
    let mut seen = HashSet::new();
    let mut out = Vec::new();
    for folder in folders {
        for entry in WalkDir::new(folder).follow_links(true).into_iter().filter_map(Result::ok) {
            let p = entry.path();
            if !entry.file_type().is_file() || !is_audio(p) {
                continue;
            }
            let key = p.to_string_lossy().into_owned();
            if !seen.insert(key) {
                continue;
            }
            if let Some(t) = read_track(p) {
                out.push(t);
                if out.len() % 50 == 0 {
                    let _ = app.emit("library://progress", out.len());
                }
            }
        }
    }
    let _ = app.emit("library://progress", out.len());
    out.sort_by(|a, b| {
        (a.album_artist.to_lowercase(), a.album.to_lowercase(), a.disc_no, a.track_no, a.title.to_lowercase())
            .cmp(&(b.album_artist.to_lowercase(), b.album.to_lowercase(), b.disc_no, b.track_no, b.title.to_lowercase()))
    });
    out
}

const FOLDER_COVERS: &[&str] = &["cover.jpg", "cover.png", "folder.jpg", "folder.png", "front.jpg", "front.png", "album.jpg"];

fn folder_cover(track: &Path) -> Option<PathBuf> {
    let dir = track.parent()?;
    FOLDER_COVERS.iter().map(|n| dir.join(n)).find(|p| p.is_file())
}

/// Embedded picture first, then a cover image next to the file. Returns a data URL.
pub fn cover_data_url(path: &str) -> Option<String> {
    let p = Path::new(path);
    if let Ok(tagged) = lofty::read_from_path(p) {
        if let Some(tag) = primary_tag(&tagged) {
            if let Some(pic) = tag.pictures().first() {
                let mime = pic.mime_type().map(|m| m.as_str().to_string()).unwrap_or_else(|| "image/jpeg".into());
                let b64 = base64::engine::general_purpose::STANDARD.encode(pic.data());
                return Some(format!("data:{mime};base64,{b64}"));
            }
        }
    }
    let cover = folder_cover(p)?;
    let bytes = fs::read(&cover).ok()?;
    let mime = if cover.extension().and_then(|e| e.to_str()) == Some("png") { "image/png" } else { "image/jpeg" };
    Some(format!("data:{mime};base64,{}", base64::engine::general_purpose::STANDARD.encode(bytes)))
}

#[cfg(test)]
mod tests {
    #[test]
    fn untagged_wav_falls_back_to_file_name() {
        let path = std::env::temp_dir().join("cw_lib_test.wav");
        // Minimal valid 16-bit stereo WAV with 0.1 s of silence.
        let frames = 4410u32;
        let mut b = Vec::new();
        b.extend_from_slice(b"RIFF");
        b.extend_from_slice(&(36 + frames * 4).to_le_bytes());
        b.extend_from_slice(b"WAVEfmt ");
        b.extend_from_slice(&16u32.to_le_bytes());
        b.extend_from_slice(&1u16.to_le_bytes());
        b.extend_from_slice(&2u16.to_le_bytes());
        b.extend_from_slice(&44100u32.to_le_bytes());
        b.extend_from_slice(&(44100u32 * 4).to_le_bytes());
        b.extend_from_slice(&4u16.to_le_bytes());
        b.extend_from_slice(&16u16.to_le_bytes());
        b.extend_from_slice(b"data");
        b.extend_from_slice(&(frames * 4).to_le_bytes());
        b.resize(b.len() + (frames * 4) as usize, 0);
        std::fs::write(&path, b).unwrap();
        let t = super::read_track(&path).unwrap();
        assert_eq!(t.title, "cw_lib_test");
        assert_eq!(t.format, "WAV");
        assert_eq!(t.sample_rate, Some(44100));
        assert_eq!(t.bit_depth, Some(16));
        assert!((t.duration - 0.1).abs() < 0.01);
    }
}
