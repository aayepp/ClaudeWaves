mod audio;
mod library;
mod lyrics;
mod media;
mod stream;
mod yt;
mod ytdlp;

use std::path::PathBuf;
use std::sync::atomic::{AtomicBool, AtomicU64, Ordering};
use std::sync::{Arc, Mutex};

use tauri::menu::{Menu, MenuItem, PredefinedMenuItem};
use tauri::tray::{MouseButton, MouseButtonState, TrayIconBuilder, TrayIconEvent};
use tauri::{AppHandle, Emitter, Manager, State, WindowEvent};

use audio::{Cmd, OutputInfo, Player};
use library::{Library, LibraryState, Track};
use media::{Media, MediaMsg, NowPlaying};

/// Whether the close button hides to the tray instead of quitting.
struct CloseToTray(AtomicBool);

/// Streaming state: the audio cache folder, the download in flight, and a
/// counter so a slow lookup cannot start playing after a newer click.
struct Streaming {
    cache_dir: PathBuf,
    current: Mutex<Option<Arc<stream::Progress>>>,
    generation: AtomicU64,
}

// ---------------------------------------------------------------------------
// Playback commands
// ---------------------------------------------------------------------------

#[tauri::command]
fn player_load(player: State<Player>, path: String, autoplay: bool) {
    player.send(Cmd::Load { path, autoplay });
}

#[tauri::command]
fn player_play(player: State<Player>) {
    player.send(Cmd::Play);
}

#[tauri::command]
fn player_pause(player: State<Player>) {
    player.send(Cmd::Pause);
}

#[tauri::command]
fn player_seek(player: State<Player>, seconds: f64) {
    player.send(Cmd::Seek(seconds));
}

#[tauri::command]
fn player_volume(player: State<Player>, volume: f32) {
    player.send(Cmd::Volume(volume));
}

#[tauri::command]
fn player_stop(player: State<Player>) {
    player.send(Cmd::Stop);
}

#[tauri::command]
fn output_info(player: State<Player>) -> OutputInfo {
    player.output_info()
}

#[tauri::command]
fn output_devices() -> Vec<String> {
    audio::list_output_devices()
}

#[tauri::command]
fn set_output_device(player: State<Player>, name: Option<String>) {
    player.send(Cmd::SetDevice(name));
}

// ---------------------------------------------------------------------------
// Library commands
// ---------------------------------------------------------------------------

#[tauri::command]
fn library_get(lib: State<LibraryState>) -> Result<Library, String> {
    lib.data.lock().map(|d| d.clone()).map_err(|e| e.to_string())
}

#[tauri::command]
async fn library_add_folder(app: AppHandle, folder: String) -> Result<Library, String> {
    let folders = {
        let lib = app.state::<LibraryState>();
        let mut data = lib.data.lock().map_err(|e| e.to_string())?;
        if !data.folders.contains(&folder) {
            data.folders.push(folder);
        }
        data.folders.clone()
    };
    rescan(app, folders).await
}

#[tauri::command]
async fn library_remove_folder(app: AppHandle, folder: String) -> Result<Library, String> {
    let folders = {
        let lib = app.state::<LibraryState>();
        let mut data = lib.data.lock().map_err(|e| e.to_string())?;
        data.folders.retain(|f| f != &folder);
        data.folders.clone()
    };
    rescan(app, folders).await
}

#[tauri::command]
async fn library_rescan(app: AppHandle) -> Result<Library, String> {
    let folders = {
        let lib = app.state::<LibraryState>();
        let data = lib.data.lock().map_err(|e| e.to_string())?;
        data.folders.clone()
    };
    rescan(app, folders).await
}

async fn rescan(app: AppHandle, folders: Vec<String>) -> Result<Library, String> {
    let app2 = app.clone();
    let scan_folders = folders.clone();
    let tracks: Vec<Track> = tauri::async_runtime::spawn_blocking(move || library::scan(&app2, &scan_folders))
        .await
        .map_err(|e| e.to_string())?;
    let lib = app.state::<LibraryState>();
    let snapshot = {
        let mut data = lib.data.lock().map_err(|e| e.to_string())?;
        data.folders = folders;
        data.tracks = tracks;
        data.clone()
    };
    lib.save()?;
    Ok(snapshot)
}

#[tauri::command]
async fn track_cover(path: String) -> Option<String> {
    tauri::async_runtime::spawn_blocking(move || library::cover_data_url(&path)).await.ok().flatten()
}

/// Reads tags for files opened directly (drag and drop, "Open with").
#[tauri::command]
async fn read_tracks(paths: Vec<String>) -> Vec<Track> {
    tauri::async_runtime::spawn_blocking(move || {
        paths.iter().filter_map(|p| library::read_track(std::path::Path::new(p))).collect()
    })
    .await
    .unwrap_or_default()
}


// ---------------------------------------------------------------------------
// YouTube Music
// ---------------------------------------------------------------------------

#[tauri::command]
async fn yt_home(yt: State<'_, yt::YtClient>, params: Option<String>) -> Result<yt::Page, String> {
    yt.home(params.as_deref()).await
}

#[tauri::command]
async fn yt_search(yt: State<'_, yt::YtClient>, query: String, filter: Option<String>) -> Result<yt::Page, String> {
    yt.search(&query, filter.as_deref()).await
}

#[tauri::command]
async fn yt_suggest(yt: State<'_, yt::YtClient>, query: String) -> Result<Vec<String>, String> {
    yt.suggestions(&query).await
}

#[tauri::command]
async fn yt_browse(yt: State<'_, yt::YtClient>, browse_id: String, params: Option<String>) -> Result<yt::Page, String> {
    yt.browse(&browse_id, params.as_deref()).await
}

#[tauri::command]
async fn yt_next(yt: State<'_, yt::YtClient>, video_id: String, playlist_id: Option<String>) -> Result<Vec<yt::Item>, String> {
    yt.next(&video_id, playlist_id.as_deref()).await
}

#[tauri::command]
fn yt_region(yt: State<yt::YtClient>, hl: String, gl: String) {
    if let Ok(mut g) = yt.hl.lock() {
        *g = hl;
    }
    if let Ok(mut g) = yt.gl.lock() {
        *g = gl;
    }
}

#[derive(Clone, serde::Serialize)]
#[serde(rename_all = "camelCase")]
struct Buffering {
    key: String,
    stage: &'static str,
}

#[derive(Clone, serde::Serialize)]
#[serde(rename_all = "camelCase")]
struct StreamInfo {
    key: String,
    codec: String,
    bitrate: Option<u32>,
    sample_rate: Option<u32>,
}

/// Plays a YouTube Music track: from the cache when it was played before,
/// otherwise resolves the stream with yt-dlp and starts as soon as the first
/// bytes are on disk.
#[tauri::command]
async fn yt_play(app: AppHandle, video_id: String, duration: Option<f64>, autoplay: bool) -> Result<(), String> {
    let key = format!("yt:{video_id}");
    let streaming = app.state::<Streaming>();
    let gen = streaming.generation.fetch_add(1, Ordering::SeqCst) + 1;
    if let Ok(mut cur) = streaming.current.lock() {
        if let Some(p) = cur.take() {
            if !p.done.load(Ordering::SeqCst) {
                p.cancelled.store(true, Ordering::SeqCst);
            }
        }
    }
    app.state::<Player>().send(Cmd::Pause);
    let _ = app.emit("player://buffering", Buffering { key: key.clone(), stage: "resolving" });

    let app2 = app.clone();
    let result = tauri::async_runtime::spawn_blocking(move || -> Result<(), String> {
        let streaming = app2.state::<Streaming>();
        let latest = || streaming.generation.load(Ordering::SeqCst) == gen;

        if let Some(path) = stream::cached(&streaming.cache_dir, &video_id) {
            let file = std::fs::File::open(&path).map_err(|e| e.to_string())?;
            let ext = path.extension().and_then(|e| e.to_str()).unwrap_or("m4a").to_string();
            if latest() {
                app2.state::<Player>().send(Cmd::LoadStream { key: key.clone(), reader: Box::new(file), ext, duration, autoplay });
            }
            return Ok(());
        }

        let ytdlp = app2.state::<ytdlp::YtDlp>();
        let resolved = ytdlp.resolve(&video_id)?;
        if !latest() {
            return Ok(());
        }
        let _ = app2.emit("player://buffering", Buffering { key: key.clone(), stage: "buffering" });
        let _ = app2.emit(
            "player://stream",
            StreamInfo {
                key: key.clone(),
                codec: resolved.codec.split('.').next().unwrap_or("").to_uppercase().replace("MP4A", "AAC"),
                bitrate: resolved.bitrate.map(|b| b.round() as u32),
                sample_rate: resolved.sample_rate,
            },
        );
        let dl = stream::start(&streaming.cache_dir, &video_id, &resolved).map_err(|e| {
            ytdlp.forget(&video_id);
            e
        })?;
        if let Ok(mut cur) = streaming.current.lock() {
            *cur = Some(dl.progress.clone());
        }
        // Wait for enough data to read the container header.
        let started = std::time::Instant::now();
        loop {
            let got = dl.progress.downloaded.load(Ordering::Acquire);
            if got >= 256 * 1024 || dl.progress.done.load(Ordering::Acquire) {
                break;
            }
            if !latest() {
                dl.progress.cancelled.store(true, Ordering::SeqCst);
                return Ok(());
            }
            if started.elapsed() > std::time::Duration::from_secs(30) {
                dl.progress.cancelled.store(true, Ordering::SeqCst);
                ytdlp.forget(&video_id);
                return Err("The stream did not start. Check your connection and try again.".into());
            }
            std::thread::sleep(std::time::Duration::from_millis(25));
        }
        if let Some(e) = dl.progress.error.lock().ok().and_then(|g| g.clone()) {
            ytdlp.forget(&video_id);
            return Err(e);
        }
        if !latest() {
            return Ok(());
        }
        let reader = stream::GrowingReader::new(&dl.path, dl.progress.clone()).map_err(|e| e.to_string())?;
        let dur = duration.or(resolved.duration);
        app2.state::<Player>().send(Cmd::LoadStream { key: key.clone(), reader: Box::new(reader), ext: resolved.ext.clone(), duration: dur, autoplay });
        Ok(())
    })
    .await
    .map_err(|e| e.to_string())?;

    let _ = app.emit("player://buffering", Buffering { key: String::new(), stage: "idle" });
    if let Err(e) = &result {
        let _ = app.emit("player://error", e.clone());
    }
    result
}

#[tauri::command]
async fn ytdlp_status(app: AppHandle) -> ytdlp::Status {
    tauri::async_runtime::spawn_blocking(move || app.state::<ytdlp::YtDlp>().status()).await.unwrap_or(ytdlp::Status {
        installed: false,
        version: None,
        path: String::new(),
        js_runtime: false,
    })
}

#[tauri::command]
async fn ytdlp_install(app: AppHandle) -> Result<ytdlp::Status, String> {
    tauri::async_runtime::spawn_blocking(move || {
        let y = app.state::<ytdlp::YtDlp>();
        y.ensure_installed()?;
        y.ensure_js_runtime()?;
        Ok(y.status())
    })
    .await
    .map_err(|e| e.to_string())?
}

#[tauri::command]
async fn lyrics_get(title: String, artist: String, album: String, duration: f64) -> Result<lyrics::Lyrics, String> {
    lyrics::fetch(&title, &artist, &album, duration).await
}

// ---------------------------------------------------------------------------
// System integration
// ---------------------------------------------------------------------------

#[tauri::command]
fn media_now_playing(media: State<Media>, track: NowPlaying) {
    media.send(MediaMsg::Metadata(track));
}

#[tauri::command]
fn media_playback(media: State<Media>, playing: bool, position: f64) {
    media.send(MediaMsg::Playback { playing, position });
}

#[tauri::command]
fn media_stopped(media: State<Media>) {
    media.send(MediaMsg::Stopped);
}

#[tauri::command]
fn set_close_to_tray(flag: State<CloseToTray>, enabled: bool) {
    flag.0.store(enabled, Ordering::Relaxed);
}

#[tauri::command]
fn quit_app(app: AppHandle) {
    app.exit(0);
}

fn show_main(app: &AppHandle) {
    if let Some(w) = app.get_webview_window("main") {
        let _ = w.unminimize();
        let _ = w.show();
        let _ = w.set_focus();
    }
}

fn build_tray(app: &AppHandle) -> tauri::Result<()> {
    let show = MenuItem::with_id(app, "show", "Show ClaudeWaves", true, None::<&str>)?;
    let toggle = MenuItem::with_id(app, "toggle", "Play / Pause", true, None::<&str>)?;
    let next = MenuItem::with_id(app, "next", "Next", true, None::<&str>)?;
    let prev = MenuItem::with_id(app, "previous", "Previous", true, None::<&str>)?;
    let sep = PredefinedMenuItem::separator(app)?;
    let quit = MenuItem::with_id(app, "quit", "Quit", true, None::<&str>)?;
    let menu = Menu::with_items(app, &[&show, &toggle, &next, &prev, &sep, &quit])?;

    let mut builder = TrayIconBuilder::with_id("main")
        .tooltip("ClaudeWaves")
        .menu(&menu)
        .show_menu_on_left_click(false)
        .on_menu_event(|app, event| match event.id.as_ref() {
            "show" => show_main(app),
            "quit" => app.exit(0),
            key @ ("toggle" | "next" | "previous") => {
                let _ = app.emit("media://key", key);
            }
            _ => {}
        })
        .on_tray_icon_event(|tray, event| {
            if let TrayIconEvent::Click { button: MouseButton::Left, button_state: MouseButtonState::Up, .. } = event {
                show_main(tray.app_handle());
            }
        });
    if let Some(icon) = app.default_window_icon() {
        builder = builder.icon(icon.clone());
    }
    builder.build(app)?;
    Ok(())
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_single_instance::init(|app, _args, _cwd| show_main(app)))
        .plugin(tauri_plugin_dialog::init())
        .setup(|app| {
            let handle = app.handle().clone();
            app.manage(Player::spawn(handle.clone()));
            app.manage(LibraryState::load(&handle));
            app.manage(CloseToTray(AtomicBool::new(true)));
            app.manage(yt::YtClient::new());
            let data_dir = handle.path().app_data_dir().unwrap_or_else(|_| std::env::temp_dir().join("claudewaves"));
            let cache_dir = handle.path().app_cache_dir().unwrap_or_else(|_| data_dir.clone()).join("audio");
            app.manage(ytdlp::YtDlp::new(data_dir));
            app.manage(Streaming { cache_dir, current: Mutex::new(None), generation: AtomicU64::new(0) });
            // Fetch or refresh yt-dlp in the background so the first song starts quickly.
            let h2 = handle.clone();
            std::thread::spawn(move || {
                let y = h2.state::<ytdlp::YtDlp>();
                if y.ensure_installed().is_ok() {
                    y.maybe_update();
                }
                if let Err(e) = y.ensure_js_runtime() {
                    log::warn!("{e}");
                }
            });

            #[cfg(windows)]
            let hwnd = app
                .get_webview_window("main")
                .and_then(|w| w.hwnd().ok())
                .map(|h| h.0 as usize);
            #[cfg(not(windows))]
            let hwnd: Option<usize> = None;
            app.manage(media::spawn(handle.clone(), hwnd));

            if let Err(e) = build_tray(&handle) {
                log::warn!("tray icon unavailable: {e}");
            }
            Ok(())
        })
        .on_window_event(|window, event| {
            if let WindowEvent::CloseRequested { api, .. } = event {
                let to_tray = window.app_handle().state::<CloseToTray>().0.load(Ordering::Relaxed);
                if to_tray && window.app_handle().tray_by_id("main").is_some() {
                    api.prevent_close();
                    let _ = window.hide();
                } else {
                    window.app_handle().exit(0);
                }
            }
        })
        .invoke_handler(tauri::generate_handler![
            player_load,
            player_play,
            player_pause,
            player_seek,
            player_volume,
            player_stop,
            output_info,
            output_devices,
            set_output_device,
            library_get,
            library_add_folder,
            library_remove_folder,
            library_rescan,
            track_cover,
            read_tracks,
            media_now_playing,
            media_playback,
            media_stopped,
            set_close_to_tray,
            quit_app,
            yt_home,
            yt_search,
            yt_suggest,
            yt_browse,
            yt_next,
            yt_region,
            yt_play,
            ytdlp_status,
            ytdlp_install,
            lyrics_get,
        ])
        .run(tauri::generate_context!())
        .expect("error while running ClaudeWaves");
}
