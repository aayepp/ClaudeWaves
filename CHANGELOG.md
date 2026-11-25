# Changelog

## [1.0.40] - 2026-11-25

### Added
- **Liquid Glass interactive motion:** added tilt-driven specular glint reflections across Liquid Glass surfaces, finger-tracking specular spotlights with rim flares on press/drag, spring-animated translucent Liquid Glass droplet thumbs while scrubbing seekbars and the in-app volume slider, and a traveling specular edge sweep during the mini-player to full-player transition (with matching Comic Pop and No-Glass fallbacks).

### Changed
- **Discord Rich Presence compact status:** updated the Discord member-list activity name across both IPC and Gateway transports (plus the Discord Settings preview card) to display `ClaudeWaves · Artist - Title` within Discord's 128-character limit.
- **Rich in-app notice cards & storage hardening:** upgraded notice dialogs with 16:9 banner headers, Markdown formatting, and full Light/Dark/Comic Pop/Liquid Glass styling, encrypted local credentials with Android Keystore AES-256-GCM, excluded private DataStore/secret files from cloud backup and device transfers, and scoped `FileProvider` sharing paths.

## [1.0.39] - 2026-11-25

### Removed
- **Liquid Studio Style:** removed the optional Liquid Studio Style setting and custom Now Playing/Library alternate layouts while keeping Floating Liquid Glass Toast Pills and the seamless Fullmode open/close transition to MiniPlayer.

### Fixed
- **Oversaturated Liquid Glass buttons in Fullmode:** restored the Fullmode readability gradient inside `liquidGlassSource(playerBackdrop)` so frosted buttons sample properly darkened artwork across all themes, and gated the video overlay scrim by `canvasCover` to avoid double darkening on static covers.
- **Animated Album Canvas in Fullmode:** matched deluxe/edition album variants and single-titled YouTube Music tracks in `CanvasArtwork`, retried clean Apple Music artist+title queries when noisy album tags miss, removed the 8-second `waitUntilAudible` stall on already-loaded tracks, prevented Android 14–16/OriginOS `TextureView` zero-alpha `SurfaceTexture` deadlock (`MIN_ACTIVE_TEXTURE_ALPHA`), and switched canvas HLS playback to `OkHttpDataSource` capped at `1200x1600` / `6 Mbps`.

## [1.0.38] - 2026-11-25

### Added
- **Liquid Studio Style (optional toggle in Settings > Appearance & Visuals):** full-screen proportioned glass Now Playing with RGB squircle liquid ripple on track switch, floating frosted Title Pill that glides to the header on Lyrics/Queue, morphing thick-to-slim Liquid Glass seekbar, interactive 3D Cover Flow Queue with tactile rotary Click-Wheel dial, Library 3D Cover Flow carousel with 180° card-flip quick preview, elastic synchronized bottom dock, and silky Fullmode crossfade across all themes (Dark/Light Liquid Glass, AMOLED, non-glass, and Comic Pop Light/Dark). Turning it off keeps the classic ClaudeWaves UI unchanged.
- **Bottom Floating Liquid Glass Toast Pills:** replaced plain system toasts for player actions (Downloads completed, Liked songs, Add to Playlist, EQ/download feedback) with swipe-to-dismiss floating glass pills across all themes, with tap-to-open Downloads on download completion.

### Changed
- **Seamless Fullmode open/close transition to MiniPlayer:** unified the Fullmode artwork card to shrink straight down center axis into the resting MiniPlayer pill with early chrome fade while keeping normal sleeve ArtworkFlight untouched.

### Fixed
- **Animated Album Canvas in Fullmode:** mounted the hardware `TextureView` outside offscreen `liquidGlassSource` capture, preserved square canvas `fallbackUrl` when `tallUrl` fails, refreshed ExoPlayer media sources cleanly on track switch, and guarded first-frame rendering so animated canvases play reliably in Fullmode.

## [1.0.37] - 2026-11-24

### Changed
- **Cleaner Last.fm settings:** removed the duplicate Discord Rich Presence card inside the Last.fm settings tab (keeping the dedicated top-level Settings card) and routed Settings search results for Discord Rich Presence directly to the Discord settings screen.

## [1.0.36] - 2026-11-24

### Fixed
- **Fast lossless startup failover on broken CDN edges:** restored the 16 KB startup range probe (`STARTUP_PROBE_BYTES`) with a 2.5 s per-candidate probe cap in `MusicPlayer` so truncated 1-byte Akamai/Qobuz streams fail fast during startup instead of stalling ExoPlayer for ~24 s before switching to a working lossless candidate.
- **Stuck tilted quality upgrade stamp on fast artwork swipe:** cleared `TierUpgradeBurst` state immediately on cancellation, track change, or `null` tier and waited for a resolved codec before setting the baseline tier in `QualityUpgradeBurst` and `NowPlayingComponents`, preventing frozen `"LOSSLESS!"` / `"HI-RES!"` stamps across all themes.

## [1.0.35] - 2026-11-24

### Fixed
- **Dead lossless CDN URL exclusion & recovery:** cached broken lossless stream URLs that fail startup probe or ExoPlayer transport open (such as `EOFException` / `unexpected end of stream` on a broken edge asset) in a 5-minute TTL exclusion map across `findPreparedStreamFor`, retries, and background upgrades, and armed one-shot `Best quality first` recovery in `onPlayerError` so playback skips the dead URL and tries the next addon candidate or provider instead of staying stuck on YouTube AAC.

## [1.0.34] - 2026-11-24

### Changed
- **Higher, evenly spaced Now Playing controls:** raised song title, `Best part` pill, seekbar, and transport controls with wider airy spacing in both Minimal and Standard styles while keeping the bottom action dock anchored cleanly above the gesture bar and matching Fullmode scrim stops across all themes.

### Fixed
- **Hi-Res tracks dropping to YouTube AAC on valid `HTTP 206` streams:** updated `MusicPlayer.isProviderReadyForStartup` to sniff audio container magic headers (`fLaC`, `ftyp`, `OggS`, `RIFF`, `ID3`, ADTS/MPEG) on the initial range chunk instead of demanding a full 16 KB read or rejecting `text/plain` CDN responses, added detailed probe diagnostics, retried the next addon candidate when a URL fails probe, and enabled one-shot mid-song Hi-Res recovery when `Best quality first` temporarily falls back to YouTube AAC.

## [1.0.33] - 2026-11-24

### Changed
- **Fullmode center balance & evenly spaced controls:** aligned the Fullmode square cover horizon near `50%` screen height while clearing the status bar, adjusted the adaptive readability scrim stops to match across all themes, and redistributed vertical spacing evenly across Now Playing Minimal and Standard control rows (including clearer `Best part` pill separation above the seekbar) while keeping the bottom action dock anchored cleanly above the gesture bar.

## [1.0.32] - 2026-11-24

### Changed
- **Frameless Fullmode lyric preview:** removed the dark horizontal rounded container box behind the 3-line Now Playing lyrics preview in Fullmode, replacing it with a dual-layer glyph-hugging blurred halo (`BlurredEdgeTreatment.Unbounded` on Android 12+ and layered shadow fallback on older Android versions) that adapts to theme ink/shade so lyrics stay crisp and legible across pure-white, bright, colorful, and dark artwork in every theme.

## [1.0.31] - 2026-11-24

### Fixed
- **Seamless Fullmode mirror artwork:** aligned top and bottom vertical reflections flush (`0dp` seam) and replaced abrupt boxed blur edges with multi-tier progressive `BlurredEdgeTreatment.Unbounded` blur across Android 12+ and smooth pre-blurred fallbacks on older Android versions across all themes.
- **Artist & Album page crash on Android 13+:** removed invalid `h` float literal suffixes from the 2-ripple AGSL `RuntimeShader` and added guarded initialization/uniform error handling with automatic Canvas fallback so opening artist and album pages never crashes.
- **Offline local ALAC `.m4a` misdetected as AAC:** replaced the 16 KB header-prefix scan in `AudioHeaderInspector` with a seekable/streaming MP4 atom walker (`moov -> trak -> mdia -> minf -> stbl -> stsd -> alac` and `mvhd`) and added `audio/alac` + `ALACSpecificConfig` bit-depth extraction in `MusicPlayer.onAudioInputFormatChanged` so local ALAC files accurately report `ALAC` (e.g., `24/44.1kHz` Hi-Res Lossless) in badges and Signal Path.

## [1.0.30] - 2026-11-24

### Changed
- **Mirror-extended Fullmode artwork:** uncropped `1:1` cover seamlessly extended top and bottom with vertically-mirrored blurred reflections, `420ms` track crossfades, automatic `ytimg.com` 4:3 letterbox bar cropping, and a lighter readability veil so cover colors shine through behind controls across all themes.
- **2-ripple AGSL liquid refraction loading:** refined Artist and Album detail loading to two calm decelerating water waves using an Android 13+ AGSL refraction shader that gently ripples across the centered title (with smooth 2-wave fallbacks on older Android versions and Comic Pop).
- **Snappier Hold-Home 2nd shortcut:** shortened hold activation from `400ms` to `150ms`, tightened the inward slide threshold to `12dp`, sped up springs, and synchronized MiniPlayer width shifting with zero frame lag.

## [1.0.29] - 2026-11-24

### Changed
- **Balanced Minimal quality badge size:** added a `medium` tier (`10.5.sp` text, `12.dp` icon, `6.dp × 2.dp` padding) in `LosslessTrackBadge` matching seekbar timestamps across Dark, Light, AMOLED, and Comic Pop themes, replacing the downscaled `0.90f` badge in `MinimalInlineQualityLabel`.
- **Fullmode artwork framing & overlapping 3-line lyrics:** balanced `StaticArtworkHero` below the top bar (`statusBarsPadding().padding(top = 22.dp)`, `aspectRatio(0.92f)`) with subtle top/bottom feathering and lighter scrim gradients, and added overlapping 3-line `NowPlayingLyricsPreview` (`overArtwork`) with an adaptive soft gradient veil and text-shadow halo so lyrics stay crisp on any cover color.

### Fixed
- **Zero-stutter artwork positioning on slow lyrics loads:** locked artwork sleeve size and vertical slot whenever `lyricsPreview` is enabled (removing the `1.4s` hold timeout) so covers never drop down and snap back up while lyrics finish fetching.

## [1.0.28] - 2026-11-24

### Changed
- **Minimal Design Now Playing polish:** replaced plain stream quality text with a scaled `CompactStreamQualityBadge`, refreshed the Best part action into a frosted hairline pill (`CircleShape` border, press scale/ripple, trailing chevron, no timestamp digits), tightened Minimal vertical spacing, and compacted the Now Playing top header and `⋮` menu button across all styles.
- **Compact Now Playing menu sheet:** redesigned the Now Playing `⋮` bottom sheet into a compact action layout with quick action tiles, streamlined track header, and compact cast row across all styles.
- **Centered 3-line lyrics preview:** focused the Now Playing lyrics preview to 3 single lines (`prev`, `current`, `next`) centered between the artwork and track info/seek bar.

### Fixed
- **3D Cover-Flow swipe & lyrics transition glitch:** stabilized the lyrics slot during horizontal artwork drags, cleared stale lyric lines across track switches, layered the artwork container cleanly, and smoothly collapsed floor reflection spacing when lyrics are visible.

## [1.0.27] - 2026-11-23

### Fixed
- **Discord login native Vulkan WebView crash (Android 17 / WebView 157):** switched the Discord login WebView to software layer rendering (`LAYER_TYPE_SOFTWARE`) and removed offscreen transition/predictive-back layers on the Discord login route, eliminating the `WebViewFunctor::drawVk` `SIGTRAP` crash on RenderThread.
- **ClaudeWaves diagnostics branding:** renamed exported diagnostics filenames (`claudewaves-diagnostics-*.txt`), crash report headers, and operational log headers from `LastWave` to `ClaudeWaves`.

## [1.0.26] - 2026-11-23

### Added
- **In-app Discord token guide & helpers:** added step-by-step instructions (Browser DevTools Network & Console one-liner with one-tap copy), clipboard paste button, and show/hide toggle inside the Discord token dialog and login header.

### Fixed
- **Discord login WebView crash:** hardened `DiscordLoginScreen` with nullable WebView callbacks, detach-before-destroy lifecycle cleanup, `WebView`/`CookieManager` fallback handling, direct `evaluateJavascript` token harvesting (replacing `alert()`), non-HTTP scheme blocking (`discord://`/`intent://`), and stable keyboard inset clearance.

## [1.0.25] - 2026-11-23

### Fixed
- **3D Cover-Flow artwork swipe bounce-back & lock:** fixed a gesture release race where lifting a finger cancelled the swipe commit animation before advancing to the next/previous track and left horizontal artwork swiping locked until reopening Now Playing. Also fixed swipe handling under Shuffle and when restarting tracks past 5 seconds.

## [1.0.24] - 2026-11-23

### Added
- **Discord Rich Presence with YouTube Music link:** sign in to Discord (or paste a token) in Settings → Discord Rich Presence to broadcast playing tracks, Hi-Res audio quality, album cover art, speed-adjusted timestamps, and a direct **Listen on YouTube Music** button live on your Discord profile. Includes a live card preview, activity label (`Listening`, `Playing`, `Watching`), and status (`Online`, `Idle`, `DND`) controls.
- **Sonar-pulse detail loading reveal:** animated sonar-pulse loading transition on Artist and Album detail screens while tracklists load.

### Changed
- **Smoother 3D Cover-Flow swipe:** refined Now Playing artwork drag physics and perspective motion when swiping between tracks.

## [1.0.23] - 2026-11-23

### Added
- **Expandable Hold-Home shortcut pill:** press and hold the docked Home pill to slide open a customizable second shortcut (`Library`, `Stats`, `Downloads`, `Search`, `Discover`, `Listen Together`, `Playlist Generator`, or `Settings`) configured in Settings → Appearance.
- **Now Playing & Library motion upgrades:** chromatic Liquid Ripple shockwave and cover pop on track changes, 3D Cover-Flow perspective tilt with neighbor peek and subtle floor reflection while dragging artwork, smooth horizontal `Lyrics ↔ Queue` transitions, polished curved-arc artwork flight, and long-press **Playlist Quick-Peek** cards in Library.
- **Interactive animated onboarding tutorial:** 3 dedicated mini-phone gesture tutorial pages demonstrating player gestures, hold-home dock & stats filter, and Quick-Peek / Listen Together / audio tools, plus a `Minimal Design` style toggle in setup.

### Changed
- **Integrated Update & What's New hero banner:** replaced the cropped strip with an uncropped `16:9` top hero header that fades seamlessly into the dialog surface with overlaid version info, keeping release notes spacious and readable.
- **Onboarding copy & spacing polish:** updated remaining LastWave onboarding copy to ClaudeWaves and refined page spacing and step progression (`10` steps).

## [1.0.22] - 2026-11-23

### Changed
- **Minimal Design Now Playing realignment:** renamed the setting cleanly to **Minimal Design**, balanced top artwork sizing and vertical control spacing so controls no longer feel crowded at the bottom, and restored the original bottom button order (`Video` left, `Queue` center, `Lyrics` right, plus `Download` when enabled).
- **Compact Update & What's New banner:** slimmed the top update banner to a compact panorama strip (`84dp`) and expanded the scrollable release notes area (`340dp`) so longer changelogs are much easier to read with less scrolling.

## [1.0.21] - 2026-11-23

### Added
- **Interactive 7-day Stats chart filter:** tap any bar in the 7-day listening activity graph to highlight that day with its play count and estimated listening time, filtering Tracks, Artists, Albums, and History below with a quick `Top` / `Time` sort switch and one-tap `Clear`.
- **Minimal Now Playing (BitChord) style:** new toggle in Settings → Appearance offering an uncrowded, frameless Now Playing layout with bare floating transport icons (`Shuffle`, `Previous`, `Play/Pause`, `Next`, `Repeat`), bare Heart and Volume/Audio buttons, a thin hairline scrubber with inline quality text, and bare icon-only bottom actions while keeping lyrics preview, full lyrics, and download behaviour unchanged across all themes.

## [1.0.20] - 2026-11-23

### Changed
- **Stats Telemetry Strip redesign:** rebuilt the Stats tab around a compact live duration top bar, 4-column hairline metric grid, top genre link, 7-day listening activity chart, ranked horizontal tabs (`Tracks`, `Artists`, `Albums`, `History`), and frameless high-density ranked rows across Light, Dark, AMOLED, and Comic Pop themes.
- **Library Segmented Category Hub redesign:** rebuilt the Library tab with a compact header (inline Search toggle, Generator pill, and `+`), a Liquid Glass 4-way segment switcher (`All`, `Playlists`, `Offline`, `Local`), a 2×2 Quick-Vault grid (`Liked Songs`, `Downloads`, `Local Songs`, `Smart Mix`), a frameless 3-column cover wall (4 columns on wide screens), and inline playable track lists for Offline and Local segments.
- **Settings Duotone Custom & Brand-Mark Icons:** replaced loud pastel blocks and flat square icon badges with unified hairline glass tiles and frosted squircle medallions featuring custom duotone glyphs and minimalist YouTube, Spotify, Last.fm, and ClaudeWaves marks across the Settings hub, Audio & Playback, and sub-sections in every theme.

## [1.0.19] - 2026-11-23

### Added
- **Draggable action dock setting:** switch between the unified draggable Now Playing dock and the classic separate bottom buttons anytime in Settings → Appearance.
- **Update & What's New banner:** added a widescreen showcase banner inside the Update Arrived and What's New dialogs while keeping the existing layout intact.

### Changed
- **Cleaner Now Playing action dock:** removed the resting glass pill so the dock stays completely clean at rest, lifting the refracting lens only while you touch or drag across slots.

## [1.0.18] - 2026-11-23

### Added
- **Draggable Now Playing action dock:** Video, Queue, Lyrics and Download now live inside one unified Liquid Glass capsule with a resting lens that lifts, magnifies and follows your thumb as you drag across slots (pull up to cancel). When Download or Video is hidden, the remaining slots center cleanly.
- **A-B section loop on the seekbar:** tap the `A–B` pill between the timestamps to mark point A, tap again to mark point B and loop that passage seamlessly (long-press or tap again to clear). Works on portrait, landscape, capsule/wavy seekbars and USB Exclusive output.
- **Local audio format filters:** filter your Local Songs by `Lossless`, `FLAC`, `DSD`, `WAV / AIFF` or `MP3 / AAC` with live track counts, hiding unused categories automatically.
- **One-tap remove duplicates:** clean repeated tracks from any local playlist from the playlist menu.
- **Sleep timer End of Track & 15s fade-out:** stop cleanly right when the current song finishes (pausing without crossfade), and enjoy a gentle 15-second volume fade-out on timed sleep timers.

### Changed
- **Consistent Liquid Glass controls:** updated remaining sliders and switches across Audio Controls, Lyrics settings, Generate and Jam to match the Liquid Glass style on Light, Dark and AMOLED themes while keeping Comic Pop unchanged.

## [1.0.17] - 2026-11-22

### Added
- **Hold the cover for Full Artwork:** on Now Playing, press and hold the artwork to switch Full Artwork on or off, without going into Settings.

## [1.0.16] - 2026-11-22

### Fixed
- **Full artwork no longer cropped:** with Full Artwork on, the cover now shows full width at its real square shape and fades into the blurred cover below, instead of being zoomed to fill the screen and cutting off its sides.
- **No sound from a paused song after allowing your USB DAC:** granting ClaudeWaves access to a USB DAC while a song was paused could make it play while the player still showed pause. A paused song now stays silent until you press play.

## [1.0.15] - 2026-11-22

### Changed
- **Cleaner What's New:** the in-app changelog now starts at ClaudeWaves 1.0.0.

## [1.0.14] - 2026-11-22

### Fixed
- **Hi-Res from add-ons plays as Hi-Res:** tracks an add-on serves as a DASH stream (Tidal Hi-Res, 24-bit FLAC) were refused by the player and fell back to YouTube AAC right after loading. They now play in Hi-Res.

### Changed
- **Add-on quality tiers:** ClaudeWaves now asks each add-on in its own quality names (for example Maximum / High / Low) instead of fixed ones, so every add-on returns its best stream.

## [1.0.13] - 2026-11-22

### Changed
- **Better diagnostics for add-on playback:** exported diagnostics now include add-on search, the chosen track, the stream it answered with and why a song fell back to YouTube, so quality problems can be traced from one report.

## [1.0.12] - 2026-11-22

### Fixed
- **Hi-Res from add-ons:** Hi-Res tracks that an add-on delivers as a DASH stream (common for Tidal Hi-Res) now play as Hi-Res instead of dropping to CD quality, and Signal Path shows the real bit depth and sample rate when the add-on only states them in its quality label.

## [1.0.11] - 2026-11-21

### Fixed
- **Other apps get sound after ClaudeWaves lets go of your USB DAC:** when ClaudeWaves releases the DAC (swiping it away, or turning USB Exclusive off), it now hands the DAC back to Android properly, so TikTok, YouTube and other apps play through your IEM without replugging.

## [1.0.10] - 2026-11-21

### Fixed
- **USB DAC freed when you swipe the app away:** with USB Exclusive on, swiping ClaudeWaves from recent apps now lets go of your DAC, so other apps play through your IEM right away. Open ClaudeWaves again and it takes the DAC back and continues the same song where it stopped, still in USB Exclusive (Hi-Res / bit-perfect).

## [1.0.9] - 2026-11-21

### Changed
- **Home headline:** now says `Hello, <your name>` from your profile (just `Hello` when no name is set), with your last played track underneath.

## [1.0.8] - 2026-11-21

### Added
- **Expired addon popup:** when the launch health check finds an enabled addon whose link has expired, a dialog names it once per session with a shortcut to **Provider Modules** to add a new link.

### Changed
- **Home headline:** `Good morning` is replaced by a listening line for the time of day (`Start the day`, `Keep the afternoon going`, `Wind down tonight`, `Late night listening`) with your last played track underneath.

## [1.0.7] - 2026-11-20

### Improved
- **iOS 26 Liquid Glass switches:** toggle switches on normal Light, Dark, and AMOLED themes now support both tapping and dragging with rubber-banding, lifting past the track edges into a clear refracting glass lens and settling back with a gentle jelly wobble.
- **Draggable Liquid Glass segmented controls:** `System / Light / Dark`, `Refresh Rate`, and other segmented selectors now feature a floating glass selection lens you can tap or scrub across options with haptic ticks.
- **Liquid Glass lens sliders:** Settings sliders (Liquid Glass intensity, custom accent HSL, Audio, Storage, and AutoEQ) now inflate their thumb into a crystal glass lens while dragging with rubber-banding at the stops.

## [1.0.6] - 2026-11-20

### Added
- **Expanded Settings → About:** added a **Gestures & Shortcuts** guide sheet, **Links & Support** (`GitHub Repository`, pre-filled `Report a Bug`, and `Share ClaudeWaves`), and a **Device & Audio Engine** diagnostics card with one-tap **Copy Info**.

## [1.0.5] - 2026-11-20

### Added
- **3-layer Best-Part detection:** combines YouTube audio & duration-matched official MV heatmaps + chapter markers (`Chorus`, `Reff`, `Drop`, `サビ`, `후렴`, `副歌`), fuzzy/CJK multi-section chorus detection with viral standalone peak override, on-device audio RMS/drop fallback for instrumentals and local tracks, persistent cache, and next-track queue pre-warming.
- **Liquid Glass Droplet toggle switches:** all on/off switches on normal Light, Dark, and AMOLED themes now feature a spring-physics translucent glass droplet morph, specular rim lighting, and haptic feedback (Comic Pop keeps its own switch style).
- **Default tutorial addon websites:** `Palv` and `Radha` are now pre-populated by default in Settings › Addons › Addon websites.

### Improved & Fixed
- **Comic Pop placement & accent memory:** moved the Comic Pop toggle directly beneath the `System / Light / Dark` row in Appearance, and saved the last chosen accent color separately for Comic Pop and normal themes so toggling no longer resets your accent.
- **Library tab top spacing:** cleared the glass fade below the pinned `Search playlists` bar so `Songs from this device` no longer overlaps or clips at the top.

## [1.0.4] - 2026-11-20

### Performance & Motion
- **Shared Liquid Glass backdrop:** `MainShell` now reuses `PlayerHost`'s `LocalLiquidGlassBackdrop` instead of recording a second nested full-screen offscreen layer on every scroll frame.
- **Faster artwork decoding:** `ArtworkImage` now defaults to `384 px` hardware-accelerated inexact decoding with a snappier `120 ms` crossfade across all 61 thumbnail call sites (Full Player stays full resolution).
- **Zero subcomposition on Feed rows:** removed `BoxWithConstraints` from `ChartQuickChipsRow`, `ContinueListeningRow`, and `PinnedChipsBar`.
- **Liquid-flow navigation transitions:** tuned screen slide transitions to a critically-damped `520f` spring with subtle parallax (`1/5` enter, `-1/14` exit).

## [1.0.3] - 2026-11-20

### Fixed
- **Mood chip bar overlap:** moved the pinned mood chips (`All`, `Energize`, `Relax`, `Workout`, `More`) inside `ExpressiveHeader` so the header edge no longer clips the top of the chip pills on any theme.
- **Smoother Feed scrolling & section switching:** removed the full-screen offscreen `LayerBackdrop` capture in chip mode, replaced `BoxWithConstraints` subcomposition in `FeedMediaShelfRow` with direct width calculation + stable pager keys, and capped card/row artwork decode sizes (`360 px` / `160 px`) for faster scrolling.

## [1.0.2] - 2026-11-20

### Improved
- **Cleaner mood chip feed (`Workout`, `Relax`, `Energize`, `Podcasts`, etc.):** removed the dark top corner mask, eliminated duplicate shelf strapline/subtitle text, unified headers with the main Feed (`See All ›` + Shuffle pills and 24 dp section spacing), and switched square shelves to the same 18 dp Overlay Art cards used on the `All` tab across every theme.

## [1.0.1] - 2026-11-20

### Added
- **See All on every Feed section:** Quick Picks, Favourite Artists, Your Mixes, Suggested Playlists, Jump Back In, Country Charts, Local Music, On Repeat, Because You Listen, Hi-Res Picks, Rediscover, New Releases, Downloads, Podcasts, and YouTube Music shelves now open a full-screen grid or track list with Play All and Shuffle actions.

## [1.0.0] - 2026-11-20

### Official Public Release
- **First official public release of ClaudeWaves** for Android (universal `arm64-v8a` & `armeabi-v7a` release APK with R8 optimization and 16 KB page-aligned native audio libraries).
- **Audiophile audio engine:** C++/Oboe native pipeline with Parametric EQ, AutoEQ, Automix transitions, lossless add-ons, and Bit-Perfect USB DAC exclusive output.
- **Liquid Glass & themes:** Adaptive glass ink across Mini Player, navigation dock, Search button, and Home pill, supporting Light, Dark, AMOLED, and Comic Pop themes.
- **Full streaming & library suite:** YouTube Music streaming, offline downloads, local device audio, Spotify imports/Canvas, Last.fm scrobbling & stats, podcasts, Listen Together, Discord Rich Presence, Google Cast, Android Auto, and Glance home-screen widgets.
