mod audio;
mod library;
mod media;

use std::sync::atomic::{AtomicBool, Ordering};

use tauri::menu::{Menu, MenuItem, PredefinedMenuItem};
use tauri::tray::{MouseButton, MouseButtonState, TrayIconBuilder, TrayIconEvent};
use tauri::{AppHandle, Emitter, Manager, State, WindowEvent};

use audio::{Cmd, OutputInfo, Player};
use library::{Library, LibraryState, Track};
use media::{Media, MediaMsg, NowPlaying};

/// Whether the close button hides to the tray instead of quitting.
struct CloseToTray(AtomicBool);

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
        ])
        .run(tauri::generate_context!())
        .expect("error while running ClaudeWaves");
}
