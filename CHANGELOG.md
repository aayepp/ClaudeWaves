# Changelog

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
