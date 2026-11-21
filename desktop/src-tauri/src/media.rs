//! System media controls: the Windows media flyout and hardware media keys
//! (SMTC), MPRIS on Linux. Runs on its own thread; the UI pushes metadata in
//! and key presses come back out as `media://key` events.

use std::thread;
use std::time::Duration;

use crossbeam_channel::{unbounded, Sender};
use serde::Deserialize;
use souvlaki::{MediaControlEvent, MediaControls, MediaMetadata, MediaPlayback, MediaPosition, PlatformConfig};
use tauri::{AppHandle, Emitter};

#[derive(Deserialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct NowPlaying {
    pub title: String,
    pub artist: String,
    pub album: String,
    pub duration: f64,
}

pub enum MediaMsg {
    Metadata(NowPlaying),
    Playback { playing: bool, position: f64 },
    Stopped,
}

pub struct Media {
    tx: Sender<MediaMsg>,
}

impl Media {
    pub fn send(&self, msg: MediaMsg) {
        let _ = self.tx.send(msg);
    }
}

/// `hwnd` is the main window handle on Windows (as usize so it can cross threads).
pub fn spawn(app: AppHandle, hwnd: Option<usize>) -> Media {
    let (tx, rx) = unbounded::<MediaMsg>();
    thread::Builder::new()
        .name("media-controls".into())
        .spawn(move || {
            // souvlaki's MPRIS backend panics on its own thread when there is no
            // D-Bus session (headless Linux); skip media controls there.
            if cfg!(target_os = "linux") && std::env::var_os("DBUS_SESSION_BUS_ADDRESS").is_none() {
                while rx.recv().is_ok() {}
                return;
            }
            let config = PlatformConfig {
                dbus_name: "claudewaves",
                display_name: "ClaudeWaves",
                hwnd: hwnd.map(|h| h as *mut std::ffi::c_void),
            };
            let mut controls = match MediaControls::new(config) {
                Ok(c) => c,
                Err(e) => {
                    log::warn!("media controls unavailable: {e:?}");
                    // Drain messages so senders never block on a dead thread.
                    while rx.recv().is_ok() {}
                    return;
                }
            };
            let app2 = app.clone();
            let attached = controls.attach(move |event| {
                let key = match event {
                    MediaControlEvent::Play => "play",
                    MediaControlEvent::Pause => "pause",
                    MediaControlEvent::Toggle => "toggle",
                    MediaControlEvent::Next => "next",
                    MediaControlEvent::Previous => "previous",
                    MediaControlEvent::Stop => "stop",
                    _ => return,
                };
                let _ = app2.emit("media://key", key);
            });
            if let Err(e) = attached {
                log::warn!("media controls attach failed: {e:?}");
            }
            while let Ok(msg) = rx.recv() {
                let res = match msg {
                    MediaMsg::Metadata(np) => controls.set_metadata(MediaMetadata {
                        title: Some(&np.title),
                        artist: Some(&np.artist),
                        album: Some(&np.album),
                        duration: (np.duration > 0.0).then(|| Duration::from_secs_f64(np.duration)),
                        cover_url: None,
                    }),
                    MediaMsg::Playback { playing, position } => {
                        let progress = Some(MediaPosition(Duration::from_secs_f64(position.max(0.0))));
                        controls.set_playback(if playing {
                            MediaPlayback::Playing { progress }
                        } else {
                            MediaPlayback::Paused { progress }
                        })
                    }
                    MediaMsg::Stopped => controls.set_playback(MediaPlayback::Stopped),
                };
                if let Err(e) = res {
                    log::debug!("media controls update failed: {e:?}");
                }
            }
        })
        .expect("spawn media thread");
    Media { tx }
}
