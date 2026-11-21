# ClaudeWaves Desktop (Windows)

ClaudeWaves for Windows: a YouTube Music and local music player built with Tauri 2,
React and the [`@zakisheriff/liquid-glass`](https://github.com/zakisheriff/Liquid-Glass)
components.

## YouTube Music (0.2)

- Home feed with mood chips, Explore and Charts, from the same InnerTube API the
  music.youtube.com web client uses (`src-tauri/src/yt.rs`). Content region is set in
  Settings.
- Search with suggestions and filters (songs, videos, albums, artists, playlists).
- Artist, album and playlist pages; Play, Shuffle and Radio.
- Playing a single song fills the queue with its radio, and the queue keeps extending
  with radio when it runs out.
- Synced lyrics from [LRCLIB](https://lrclib.net), with tap-to-seek.
- Audio streams are found by [yt-dlp](https://github.com/yt-dlp/yt-dlp). On first
  start the app downloads the official yt-dlp release and Deno (the JavaScript runtime
  yt-dlp uses for YouTube) into its data folder and lets yt-dlp update itself every few
  days. Streams are saved to the cache folder while they play (2 GB limit, oldest first),
  so a song played again starts from disk.

ClaudeWaves is not affiliated with YouTube or Google. Using it may be against the
YouTube Terms of Service; that is your call.

## Local files

- Library from one or more folders: FLAC, MP3, WAV, OGG Vorbis, M4A (AAC, ALAC), AIFF.
  Tags and embedded covers are read with lofty; a `cover.jpg` or `folder.jpg` next to
  the files is used when a file has no cover. The library is cached in
  `%APPDATA%\com.claudewaves.desktop\library.json`.
- Playback in Rust: symphonia decodes, rubato resamples to the device rate when the
  file and the device differ, cpal plays through WASAPI shared mode. The panel on the
  right shows whether resampling is happening.
- Output device picker in Settings. If the device disappears (a USB DAC unplugged), the
  engine reopens the output and resumes at the same position.
- Queue, shuffle, repeat (all / one), Songs, Albums, album pages and search.
- Windows media flyout and keyboard media keys (SMTC), system tray with play controls,
  close-to-tray, single instance.
- Light, dark or follow Windows.

Keyboard: Space play/pause, Ctrl+K search, Ctrl+Left / Ctrl+Right previous / next.

## Build

You need Node 22, Rust stable and, on Windows, the
[Tauri prerequisites](https://v2.tauri.app/start/prerequisites/) (Microsoft C++ Build
Tools and WebView2, which Windows 11 already has).

```bash
cd desktop
npm install
npm run tauri dev      # run the app with hot reload
npm run tauri build    # installers in src-tauri/target/release/bundle/{msi,nsis}
```

`npm run dev` alone opens the UI in a browser with a demo library and a simulated
clock, which is handy for UI work. Folder access and audio need the desktop app.

The GitHub Actions workflow `.github/workflows/desktop-windows.yml` runs the Rust tests
and builds the `.msi` and `.exe` installers on every push that touches `desktop/`. The
installers are attached to the workflow run as the `ClaudeWaves-Windows` artifact.

## Layout

| Path | Contents |
| --- | --- |
| `src-tauri/src/audio.rs` | Playback engine: decode, resample, ring buffer, output stream |
| `src-tauri/src/library.rs` | Folder scan, tag and cover reading, library cache |
| `src-tauri/src/media.rs` | Windows media controls (SMTC) |
| `src-tauri/src/yt.rs` | YouTube Music pages, search, radio |
| `src-tauri/src/ytdlp.rs` | yt-dlp and Deno download, stream lookup |
| `src-tauri/src/stream.rs` | Progressive download into the cache, growing-file reader |
| `src-tauri/src/lyrics.rs` | LRCLIB lyrics |
| `src/yt.tsx` | YouTube Music screens |
| `src/lyrics.tsx` | Synced lyrics panel |
| `src-tauri/src/lib.rs` | Tauri commands, tray, window handling |
| `src/App.tsx` | Views and layout |
| `src/player.ts` | Queue, shuffle, repeat, media key handling |
| `src/api.ts` | Calls into Rust, plus the browser demo backend |

## Tests

```bash
cd desktop/src-tauri
cargo test
```

The tests decode generated WAV files to check sample counts, 44.1 kHz to 48 kHz
resampling, seeking, and tag fallback for untagged files; they also cover the YouTube
Music item parser, the growing-file reader and title cleanup for lyrics lookups.
`cargo test -- --ignored live` calls music.youtube.com and prints what it parses.
