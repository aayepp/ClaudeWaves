# Changelog

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

## [5.4.58] - 2026-11-20

### Fixed
- **Tab bar text reads on any background:** each tab (Feed, Stats, Library) now picks white or dark text from what's behind it, instead of one colour for the whole bar, so a tab over dark artwork no longer gets lost. Icons and labels also get a soft halo, and the colour keeps up while you scroll. Works in light, dark and AMOLED.

## [5.4.57] - 2026-11-20

### Fixed
- **Smoother scrolling & navigation:** batched all adaptive glass ink checks (Mini Player, Home dock, Search button, nav pill) into a single shared backdrop readback (~9× fewer GPU stalls), skipped hidden dock/pill sampling, stopped Home Feed from recording an unused full-screen offscreen backdrop pass on every scroll frame, and paused heavy glass refraction/readbacks during route transitions.

## [5.4.56] - 2026-11-20

### Changed
- **Higher-precision adaptive glass ink:** switched backdrop sampling to a center-weighted 16×8 WCAG 2.1 linear luminance calculation focused on the text/icon band (excluding the Mini Player's cover art), and gave the Search button its own independent backdrop sample so even subtle background shifts flip accurately.

## [5.4.55] - 2026-11-20

### Changed
- **Faster adaptive glass ink:** Mini Player and Home bar dock now sample backdrop luminance every 100 ms with a 120 ms ink fade (~2× snappier response while scrolling).

## [5.4.54] - 2026-11-20

### Added
- **Adaptive text & icons on glass Mini Player and Home bar:** with Liquid Glass on, text, icons, and quality tags on the Mini Player, bottom navigation dock, Search button, and Home pill smoothly switch between light and dark ink to stay legible over bright or dark content while scrolling.

## [5.4.53] - 2026-11-19

### Changed
- **New Settings home:** Settings now opens on a grid of large tiles (Audio, Appearance, YouTube Music, Spotify, Last.fm, Library, Add-ons, Data & Backup), each showing its status at a glance, with About below.

### Removed
- **Jam in Settings:** the Collaboration & Social section is gone from Settings. Listen Together still opens from Home and the song menu.

## [5.4.52] - 2026-11-19

### Fixed
- **Glass rim everywhere:** the Home button and the shrunk tab circle now have the same lit glass edge as the mini player and the tab bar.

## [5.4.51] - 2026-11-19

### Added
- **Home button on the right:** Settings › Appearance › Home Button on Right moves the Home button and the docked tab to the right edge, easier to reach with your thumb. Left stays the default.

### Changed
- **Cleaner tutorial animations:** the startup tutorial's demos now use real icons and text, and move more gently.
- **Glass rim on the tab bar:** the tab bar and search button now have the same lit glass edge as the mini player.

### Fixed
- **Streaming Quality shows what's playing:** when the quality you picked isn't available for a song (for example Hi-Res playing as 320 kbps), the quality that's actually playing is marked PLAYING and a note explains why.

## [5.4.50] - 2026-11-19

### Added
- **Animated tutorial:** every page of the startup tutorial now shows a short animated demo of what it sets up, in your theme's colours. Only the page you are on moves, and it stays still when Android's animations are turned off.

### Changed
- **Clearer addon step:** Palv and Radha are websites that make an addon link for you, not addons. The tutorial now opens them in your browser and explains how to generate the link, copy it and paste it in.

## [5.4.49] - 2026-11-19

### Fixed
- **Bottom dock selected tab pill alignment:** moved the 4 dp inset padding onto the dock container `Box` so the selected tab pill fills its full height and aligns symmetrically inside the bottom bar across Comic Pop Light/Dark and all other themes.

## [5.4.48] - 2026-11-19

### Added
- **Live addon Online / Offline / Expired indicators:** configured HTTP addons are now auto-checked in the background on app launch (and on demand via **Check All** or Refresh in Settings › Provider Modules), displaying colour-coded status pills (`Online`, `Offline`, `Expired`, `Checking...`).
- **Colour-coded addon expiry timer:** addons that report an expiry timer show a live highlighted countdown badge — green when plenty of time remains (> 6 h), amber when running low (1–6 h), and red when nearly expired (< 1 h) or expired.

## [5.4.47] - 2026-11-19

### Changed
- **Silky, seamless touch physics:** replaced hard frame-0 jiggle snaps with velocity-driven liquid springs, damped wobble on wide bars (mini player & dock), slowed water ripple rings to 620 ms, and softened Home/Feed card press springs so every interaction feels smooth instead of stiff.

### Fixed
- **Crash when refreshing addons (or showing snackbars):** fixed a Liquid Glass backdrop self-capture crash in `MiniPlayerAwareSnackbarHost` and hardened addon health checks against unexpected errors.

## [5.4.46] - 2026-11-19

### Added
- **Water ripple ring on touch:** tapping the mini player, Home dock, search circle, and Now Playing glass buttons sends a double-wave specular water ripple expanding out from your finger alongside the jelly jiggle.

### Changed
- **Crystal-clear Liquid Glass (Light, Dark & AMOLED):** lower blur and lighter scrim at the clear end, stronger optical lens refraction and contrast, higher vibrancy, a crisp outer dark hairline in Light Mode so white glass pops off bright pages, brighter specular rims in Dark/AMOLED, and default Glass Clarity raised to 82%.

## [5.4.45] - 2026-11-19

### Added
- **Water-drop jelly jiggle:** pressing and releasing the Home dock, search circle, mini player, and Now Playing glass buttons now triggers an elastic X/Y squash-and-stretch wobble like touching a water bubble (works across Liquid Glass, Light, Dark, AMOLED, and Comic Pop).

## [5.4.44] - 2026-11-19

### Added
- **Edit Profile in Settings:** new profile card at the top of Settings so you can change your username or upload/remove your profile photo anytime without repeating the startup tutorial.

### Changed
- **Auris-grade fluid dock & player transitions:** upgraded the bottom tab pill to 3-spring acceleration squash & stretch (stretches wide on launch, squashes tall while braking, sheds glass as it lands), reduced Now Playing open/close GPU load with a nanosecond `GlassMotion` lens relief clock and `derivedStateOf` artwork flight handoff, and tuned the folding dock spring for crisper scroll response.

## [5.4.43] - 2026-11-19

### Added
- **Popular songs indicator on Artist pages:** added a quick **★ Popular** sort pill beside the Songs sort menu (tap to toggle most/least popular or return from another sort) and small star badges on the artist's top 3 hits.

## [5.4.42] - 2026-11-19

### Added
- **Startup Tutorial & Setup:** new 8-step first-time setup wizard covering feature highlights, live theme & accent picker, custom username & profile photo (shown across Home and Settings), YouTube Music & Spotify linking, built-in/custom website addons, audio quality & Automix, permissions, and an interactive gesture sandbox. Existing users get a dismissible invite card on Home, and anyone can repeat the setup anytime from **Settings › About**.
- **Long-press card lift menu:** pressing and holding a Home/Feed card lifts a snapshot of that card above the quick menu (tapping `⋮` still opens the normal sheet).

### Changed
- **Battery-smart fluid backdrop:** Now Playing's fluid artwork background now drifts smoothly for 8 seconds on open or track skip, then eases gently to rest to save GPU and battery.
- **Overlapping duet lyrics:** unfinished duet and backing vocal lines stay at full brightness and scale until their own timestamps end.

### Removed
- **Best part seek-bar tick:** removed the vertical line on the seek bar while keeping the **Best part** pill above it.

### Fixed
- **Queue drag-to-reorder:** fixed a bug where dragging a second song in the queue stopped working after the first drag, enlarged the drag handle hit area to the full 40 dp button, and locked list scrolling while dragging.

## [5.4.41] - 2026-11-19

### Added
- **Live Liquid Glass preview:** Settings › Appearance now shows an interactive glass pill above the Glass Clarity slider so you can preview blur, vibrancy, prism edges, and press refraction live while dragging or holding it.
- **Artwork-tinted glass:** the mini player and Now Playing glass controls reflect a subtle tint from the current song's cover art.

### Changed
- **Liquid Glass snackbars:** toasts and confirmations above the mini player now render as frosted glass pills (or Comic Pop panels in Comic theme).
- **Glass bottom sheets:** track menus, EQ, and Signal Path sheets now follow your Glass Clarity slider with a top specular rim and frosted drag handle.

## [5.4.40] - 2026-11-19

### Changed
- **Richer Liquid Glass:** floating glass (mini player, nav rail, pills and buttons) now boosts colour vibrancy with your Glass Clarity slider, adds subtle chromatic prism edges when clear or pressed, and separates cleanly from light, dark and AMOLED backgrounds with a directional top-left specular rim.
- **More liquid press feel:** tapping glass bends content underneath more strongly and tracks your finger with a two-layer specular highlight.
- **Deeper glass cards:** the background colour wash scales with Glass Clarity and glass cards pick up a diagonal specular rim.

## [5.4.39] - 2026-11-19

### Changed
- **Simpler search bar:** Search's bar is now one field with the Back button at its right end. The extra search circle and the microphone button are gone.

## [5.4.38] - 2026-11-19

### Fixed
- **Search bar is clear again:** the search circle and field at the bottom of Search were washed out by the fade that softens content under the tab bar. They now show in full.

## [5.4.37] - 2026-11-19

### Fixed
- **Search bar sits where the tab bar was:** Search now shows a search circle and the search field at the bottom, under the mini player, instead of a field floating above it with the Home button below. Opening Search shrinks the tabs into the circle and grows the field out from the right.

## [5.4.36] - 2026-11-19

### Changed
- **Glass tab bar glides:** tap a tab and the glass bubble lifts, glides across to it and settles as a pill in your accent colour.
- **Search field at the bottom:** Search opens with its field at the bottom of the screen, growing out from where the search button was. The keyboard only opens when you tap the field, whether you came from the bar or the Home search box.
- **Cover flies into Now Playing:** opening Now Playing flies the song's cover from the mini player up to the big artwork, and closing flies it back.

### Removed
- **Old full-width tab bar:** every setup now uses the floating bar.

## [5.4.35] - 2026-11-19

### Added
- **Moving tab pill:** the selected tab on the bottom bar is now one pill that lifts, glides to the tab you tap and settles, stretching a little as it moves. Slide your finger along the bar and it follows, with a tick for each tab. On glass themes it turns to glass while moving; in Comic Pop it stays your accent colour with an ink outline.
- **Pull up the mini player:** drag the mini player upwards and Now Playing rises under your finger. Let go high or flick up to open it; let go low and it drops back.

### Changed
- **Smoother Now Playing open and close:** it now glides in and out without bouncing, and the song's cover grows from the mini player into the full screen.

## [5.4.34] - 2026-11-19

### Fixed
- **Comic Pop follows your accent everywhere:** the yellow on Home (Discover tile, chips, Play buttons, the selected tab) and in the player now changes to the accent colour you pick. Choose the Comic tile to get the yellow back. Hi-Res badges stay gold.

## [5.4.33] - 2026-11-19

### Added
- **Your own accent in Comic Pop:** with Comic Pop on, the Accent colours below still work. Pick any preset, Mono, a custom colour or Dynamic Color, and Comic Pop keeps its paper, ink and halftone look. A new **Comic** tile brings back Comic Pop's own colours.

## [5.4.32] - 2026-11-19

### Added
- **Next song's lyrics load ahead:** a few seconds into each song, the lyrics for the next song in your queue are fetched in the background, so skipping shows them straight away.
- **Lyrics remembered:** lyrics you have seen are kept on your phone, so replaying a song shows them instantly, even after restarting the app or when offline.

### Changed
- **Faster lyrics:** saved lyrics now appear the moment a song starts instead of after a short wait.

## [5.4.31] - 2026-11-19

### Added
- **Home button beside the mini player:** on any screen outside Home (Settings, albums, artists, playlists and more), a round Home button sits left of the mini player. Tap it to close everything and land back on the Home tab you last used.
- **Popular songs in albums:** an album's biggest hits get a small star next to their title, based on YouTube Music play counts.

### Changed
- **Simpler update popup:** just Ignore and Update.

### Removed
- **GitHub links in About:** the GitHub Releases button, Source Code and Upstream Project cards are gone; the GPL v3 licence notice stays.

## [5.4.30] - 2026-11-18

### Changed
- **Landscape lyrics under the cover:** in landscape (and on car displays), the lyric lines now sit under the album cover, which shrinks a little to make room, centred together on the left.

### Fixed
- **Lyrics no longer overlap the Best part button** in landscape.
- **Long lyric lines show in full:** instead of being cut with "…", a long line wraps onto a second row at a readable size.

## [5.4.29] - 2026-11-18

### Added
- **Animated icons:** every icon button now gives a little bounce when you tap it.
- **Living dock icons:** Home's door swings open, Stats' bars rise, Playlists' rows slide in, and the search lens tilts when you tap them.
- **Card-flip play/pause:** the play/pause icon flips over like a card.
- **Toggle pops:** icons that switch (lock, fullscreen, sort, expand, play/pause and more) pop as they change; saving an album, finishing a download and liking a song also burst little sparkles.
- **Now Playing buttons come alive:** skip icons nudge toward the jump, the 15-second buttons turn, repeat spins, shuffle crosses its arrows, and the Download button draws a tick when the song is saved.
- **Play next confirms:** a tick draws in before the song menu closes.

## [5.4.28] - 2026-11-18

### Added
- **Animated artwork offline:** animated album covers you have watched before now play without internet too.
- **Like sparkles:** liking a song bursts little sparkles out of the heart, in Now Playing and the mini player.
- **Swipe feel:** in Now Playing the cover follows your finger when you swipe to skip, with a light tick when the swipe is far enough.
- **Play/pause morph:** the pause bars fold smoothly into the play triangle and back.
- **Best part glow:** the Best part button glows once when the song reaches its best part.

### Changed
- **Best part is more accurate:** it starts a moment before the chorus so the first word isn't cut, begins at the chorus's first line, and handles songs with back-to-back choruses and ad-libs better.
- **Best part button moved** above the progress bar, with a neater marker on the bar.
- **Smoother song change:** the cover and song title slide further when you skip.

## [5.4.27] - 2026-11-18

### Changed
- **Best part button:** Now Playing shows a "Best part" button under the progress bar, with a small mark on the bar where it starts. Tap it to jump there.

### Fixed
- **Best part finds the chorus:** Play best part no longer starts every song a few seconds in. It now jumps to the chorus (from the lyrics' song sections or repeated lines), picking the chorus people replay most on YouTube.

## [5.4.26] - 2026-11-18

### Added
- **Best part on the seek bar:** Now Playing shows a small dot on the progress bar where the song's best part starts. Tap it to jump straight there.

## [5.4.25] - 2026-11-18

### Added
- **Play songs offline from cache:** songs you have played all the way through are kept and play again without internet. The cache now fills on Wi-Fi and mobile data, holds whole hi-res songs, and defaults to 1 GB (change it in Settings › Storage).
- **Play best part:** a new option in the song menu jumps straight to the part of the song people replay most on YouTube, or to the first chorus from the synced lyrics.

## [5.4.24] - 2026-11-18

### Removed
- **Background image:** the Background image option in Settings › Appearance is gone, and any saved image is deleted. The soft colour wash behind glass cards stays.

## [5.4.23] - 2026-11-18

### Changed
- **Background image looks like a phone's control centre:** your photo is now blurred more strongly so only its colours show through, with a slightly stronger default fade. Pick your image again (Settings › Appearance › Background image › Change) to get the new look.

### Fixed
- **Background image on Home:** your background image (and the colour wash) now shows behind Home, Statistics, Friends and every other screen, instead of being hidden behind a flat colour.

## [5.4.22] - 2026-11-18

### Fixed
- **Header line, for real this time:** the blur under every glass header (Local songs, Liked Songs, Downloads, Settings and more) now melts smoothly into the page instead of ending on a sharp line, in light and dark themes.

## [5.4.21] - 2026-11-18

### Added
- **Background image:** pick your own photo in Settings › Appearance › Background image. It is softly blurred and shows behind every screen, with glass cards and headers letting it through. Use Fade to keep text easy to read, or Remove to go back to the colour wash.

### Changed
- **Glass everywhere:** every card in the app now uses the same glass look, including Statistics, Modules, Storage, Downloads, imports and artist and album pages.

### Removed
- **App icon picker:** the App Icon option is gone; everyone uses the ClaudeWaves icon.

### Fixed
- **Hard lines at the top:** the sharp line under headers in Settings, Statistics and other screens is gone, and album, artist, playlist and podcast artwork now fades smoothly into the page in light and dark themes.

## [5.4.20] - 2026-11-18

### Changed
- **Glass cards:** with Liquid Glass on, Settings rows, playlist and song rows and other grouped cards are now see-through glass with a softly lit edge, over a gentle wash of your theme colours, like a phone's control centre. The Liquid Glass Intensity slider sets how clear they are.
- **Easier to read glass:** buttons, headers and the mini player keep a stronger blur at every intensity, so text stays clear over busy covers.

### Fixed
- **Local songs header line:** the hard line under the top bar in Local songs is gone.

## [5.4.19] - 2026-11-18

### Changed
- **Clearer liquid glass:** the Liquid Glass Intensity slider in Settings › Appearance now really clears the glass. Headers, the mini player, the navigation dock and every Now Playing and lyrics button have a much thinner frost at the default setting, so the colours behind them glow through and the glass edge bends the light. Slide it left for the old frosted look.

## [5.4.18] - 2026-11-18

### Changed
- **Light theme with Dynamic color by default:** the app now starts in Light mode with colours taken from your wallpaper. Existing installs switch over once on the next launch; you can change it again in Settings › Appearance.

## [5.4.17] - 2026-11-18

### Changed
- **Smooth Now Playing cover & lyrics transition:** when synced lyrics appear under the cover in portrait, the album artwork now glides down and resizes smoothly together with the fading lyrics instead of snapping in one frame; landscape lyrics also expand and fade cleanly.

## [5.4.16] - 2026-11-18

### Changed
- **Heart-shaped like nudge:** the like suggestion glow now traces the heart icon itself with a soft jiggle animation instead of a circle around the button, and tapping like gives the heart a brief pop and jiggle.
- **Permanent red heart:** liked songs stay red across every theme (including the mini player, Now Playing, and local music), instead of taking the theme's accent colour.
- **Light theme queue wash:** the Queue screen in light themes now uses a smooth frosted wash over the artwork so every row stays crisp and readable top to bottom while scrolling.

### Fixed
- **Liked heart state mismatch:** playing a downloaded YouTube song (or a song saved under an older local path) no longer shows an empty heart that unlikes the song when tapped.

## [5.4.15] - 2026-11-18

### Changed
- **HI-RES, ATMOS badges:** songs that also have a Dolby Atmos version show HI-RES followed by ATMOS, so the list shows what plays and what else exists. With Dolby Atmos as your streaming quality, ATMOS comes first and HI-RES is outlined.
- **Lyrics in landscape:** the four lyric lines now sit just above the progress bar, centred, and also appear on car displays in a compact size. In portrait the cover and lyrics sit a little lower.
- **Like nudge:** the like button now pulses from the third time you start a song, even if you didn't finish it before.

### Fixed
- **Queue flicker:** queue text no longer changes colour row by row while scrolling; the whole list uses one readable colour.
- **Header line:** the hard line under the header in Liked Music, Liked Songs, Liked on YouTube, Local songs and other screens is gone.

## [5.4.14] - 2026-11-18

### Added
- **Lyrics in Now Playing:** four synced lyric lines follow the song under the cover, with the current line highlighted; tap them to open Lyrics. In landscape they sit above the song title (hidden on short screens such as car displays). Turn off in Settings › Lyrics under the cover.

## [5.4.13] - 2026-11-18

### Added
- **Auto-romanized lyrics:** Korean, Japanese, Chinese and other non-Latin lyrics now show their Latin pronunciation underneath automatically, also offline for downloaded songs. Turn it off in Settings › Auto-romanize lyrics.
- **Like nudge:** a song you have played to the end three times but not liked gets a red glowing ring on its like button in the mini player and Now Playing; it pulses seven times when the song starts.
- **ATMOS + HI-RES:** songs with both a Dolby Atmos and a stereo version show both badges, so ATMOS no longer turns into HI-RES once you play the song.

### Changed
- **Downloads badges:** downloaded songs show the same HI-RES / LOSSLESS / ATMOS badge as other song lists instead of the file spec (MP3, 24-BIT / 96k).

### Fixed
- **Header blur:** headers now blur everything under them down to their bottom edge, so scrolling songs no longer show through titles and the Downloads filter chips.
- **Headers without glass:** with Liquid Glass off or in Comic Pop, the Album, Artist, Playlist and Search headers are solid again instead of see-through.

## [5.4.12] - 2026-11-17

### Added
- **Downloaded mark in song lists:** songs you have downloaded show a small ⬇✓ icon next to their quality badge in albums, artists, search, the queue, Home and other song lists, so you can see at a glance what plays offline.

## [5.4.11] - 2026-11-17

### Changed
- **Glowing change titles:** in the update and changelog dialogs, each change title now glows softly along its letters instead of sitting in a highlight box. Comic Pop glows yellow.

## [5.4.10] - 2026-11-17

### Fixed
- **Queue badges on dark artwork:** HI-RES, LOSSLESS and ATMOS badges in the queue now switch to their light or dark colours with the artwork behind each row, like the text does.
- **Comic Pop badges:** quality badges in Comic Pop are now solid yellow, mint and pink with an ink outline, instead of a faint tint.

## [5.4.9] - 2026-11-17

### Added
- **Swipe to queue:** swipe a song to the right to add it to the queue, in playlists, Liked Songs, YouTube playlists, local music, albums, artists and search.
- **Quality follows output:** a new Audio setting streams Hi-Res and Lossless only on wired IEMs and USB DACs, and 320 kbps on the speaker and Bluetooth. Plugging in or unplugging switches the playing song.

### Changed
- **Now Playing output and quality:** the output button is now an icon for the speaker, earbuds, IEM, USB DAC or Cast (each in its own colour in Comic Pop), and quality uses the same badge as song lists.
- **Release notes:** the update and changelog dialogs highlight each change title and colour the Added, Changed, Fixed and Removed sections.

### Fixed
- **Queue text on dark artwork:** queue text now turns light or dark to match the artwork behind each row, live while you scroll.
- **15 s seek buttons:** the "15" sits centred in the ring at any font size.

## [5.4.8] - 2026-11-17

### Added
- **New Home sections:** "Because you listen to", Hi-Res Picks (songs confirmed in Hi-Res) and Podcasts now show on Home; Rediscover (favourites you have not played in a month) can be switched on in Edit home.
- **Addon websites:** save the sites you get addons from in Provider Modules and open them in one tap. A failing addon shows "Get new addon" that goes straight there.

### Changed
- **Hi-Res Lossless by default:** Wi-Fi and cellular streaming quality start on Hi-Res Lossless for everyone; you can still pick another quality.
- **Listen Together:** the always-on room list keeps only International.

### Fixed
- **Drag to reorder:** moving Home sections and addons no longer jumps or stutters, and the list scrolls when you drag to the top or bottom edge.
- **Home on tablets:** sections shown by default now appear in the wide layout too.

## [5.4.7] - 2026-11-17

### Fixed
- **Header line in Comic Pop:** in Comic Pop light and dark (and any theme with Liquid Glass off), headers and the playlist, album and artist top bars no longer end on a hard line; content scrolls under them and fades out softly.
- **Streaming quality layout:** while a quality switch loads, the selected option (such as CD Lossless) no longer stretches across the whole row and pushes the next option down.

## [5.4.6] - 2026-11-17

### Changed
- **Glass headers everywhere:** with Liquid Glass on, content now scrolls under a blurred header on Liked Songs and playlists, albums, artists, Library, Downloads, Search, Provider Modules, Storage and AutoEq, like the rest of the app. Library, Downloads and Search keep their search, tabs and filters inside that header.

### Fixed
- **Header lines:** the thin divider and the shadow under headers are gone; headers fade into the page.

## [5.4.5] - 2026-11-17

### Fixed
- **5.4.4 build:** 5.4.4 failed to publish because of a broken French and Italian translation. This release ships everything listed under 5.4.4 below.

## [5.4.4] - 2026-11-17

### Added
- **Update notifications:** the app checks for a new release about every 12 hours and posts one notification per version in your notification shade. Tap it to open the update dialog. Turn it off under the "App updates" notification channel.
- **Addon priority:** in Settings › Provider Modules, drag the ≡ handle to rank your streaming addons. All enabled addons are still asked at once, but the highest-ranked one that finds the song wins; a lower one is used only if those above fail or answer more than 2 seconds later.
- **Quality switch progress:** while a new streaming quality is being lined up mid-song, a loading bar runs inside the selected quality.

### Changed
- **Now Playing in light mode:** the player follows the app's light or dark mode on every theme, using the theme's own colours (Comic Pop keeps its comic look in both).

### Fixed
- **Selected streaming quality:** the chosen quality is now clearly highlighted in light and dark mode on every theme.
- **Header line:** headers on detail screens (charts, albums, playlists) fade into the page instead of ending on a hard line.
- **Home top edge:** the status bar area on Home no longer shows a solid strip with a hard edge; content blurs or fades under it like every other screen.

## [5.4.3] - 2026-11-17

### Added
- **Soft screen edges:** content scrolling under the status bar or behind the mini player and bottom bar now blurs away (with Liquid Glass) or fades into the page (without), on every screen.

### Changed
- **One glass recipe:** the bottom bar takes its base blur and saturation from the same values as the mini player, so the two always frost alike.

### Fixed
- **Home card sizes:** Made For You, Country Charts and the other card shelves no longer show huge, mismatched cards outside Comic Pop. Every theme now uses the same fixed-size cards, two per row on phones.

## [5.4.2] - 2026-11-17

### Changed
- **Bottom bar follows Liquid Glass intensity:** the tab rail, search button and collapsed pill now frost and tint with the slider, matching the mini player beside them. The selected tab stays the frostiest part of the bar.

### Fixed
- **Intensity slider direction:** 100% is now the clearest glass (light blur, thin tint, strongest refraction) and 0% the most frosted, as the slider hint says. Glass bottom sheets follow the same direction. If you set the slider before, you may want to adjust it again.

## [5.4.1] - 2026-11-17

### Changed
- **Frosted selected tab:** in the glass bottom bar the active tab is now the frostiest part of the rail instead of a clear window, with its icon and label kept sharp.
- **Signal Path cards:** every section (source → output, streaming quality, checks, health, internal/USB output) sits in the same card size and shape.
- **Liquid Glass default 70%:** new installs start at 70% intensity so the nav bar stays legible over busy artwork. A value you already set is kept.
- **Intensity hint:** the slider now explains what lower and higher values do, in every app language.

### Fixed
- **Signal Path background:** the sheet is solid in light and dark mode and no longer changes with the glass intensity slider.

## [5.4.0] - 2026-11-17

### Changed
- **One Home layout on every theme:** light, dark, AMOLED and every accent now get the same Home as Comic Pop, only in their own colours: six-song Continue Listening grid, symmetric Made For You shelf, single-column community playlists, Your Sound and mood chips that fill the row, paged quick tiles and Quick Picks, and chart chips aligned to the page edges.
- **Glass quality badge:** in Now Playing (outside Comic Pop) the audio quality badge is tinted liquid glass, gold for Hi-Res and teal for Lossless, in both light and dark mode.

### Fixed
- **Home section indent:** shelves on non-Comic themes no longer sit indented inside an extra panel.

## [5.3.9] - 2026-11-17

### Added
- **Liquid Glass intensity:** a 0–100% slider in Settings › Appearance under Liquid Glass, with a live preview. Lower values make the glass softer and more frosted.
- **Glass bottom sheets:** audio, EQ, Signal Path, queue, add-to-playlist and song menus now blur the screen behind them on Android 12+, following the intensity slider.

### Fixed
- **Mini player width:** it is the same floating pill on every screen, instead of stretching edge to edge in song lists, albums, playlists and Downloads.

## [5.3.8] - 2026-11-17

### Fixed
- **Home spacing:** sections have their normal gaps again instead of sitting tight against each other.

### Changed
- **Original motion restored:** the 5.3.7 animations (cover flight, card zoom, bounce scrolling and staggered entrances) are removed; scrolling and navigation feel like before.

## [5.3.7] - 2026-11-17

### Added
- **Fluid player open:** the cover flies from the mini player straight into Now Playing (and back on close, even after a swipe-down), with a softer ~400 ms spring.
- **Card → detail zoom:** album and playlist screens grow out of the card you tapped and shrink back into it on Back, while Home recedes behind.
- **Rubber-band lists:** every list stretches with resistance past its edge and bounces back; flings that hit an edge bounce slightly.
- **Staggered entrances:** Home sections and Playlists rows rise in one after another when the screen opens.

### Changed
- All of the above follow the system: with **Remove animations** or **Battery Saver** on, the player snaps open, cards open without zooming, and lists keep Android's own stretch.

## [5.3.6] - 2026-11-17

### Changed
- **Edit Home by holding a section:** the pencil button is gone. Hold any Home section to **Move up**, **Move down**, **Hide**, or open **Rearrange home**, a bottom sheet with the full drag-and-hide list.
- **Signal Path:** the Source side shows its quality label and badge once, without a repeated PCM line. When playback is **bit-perfect**, the Source → Output card lights up with a verified seal.

### Fixed
- **Rearranging Home sections:** rows now slide smoothly while dragging instead of jumping, in both the Home sheet and Settings › Home sections.

## [5.3.5] - 2026-11-17

### Changed
- **Quality badge in Signal Path:** the Source side now shows the same **HI-RES** / **LOSSLESS** pill used in song lists, so the stream quality is visible at a glance.

## [5.3.4] - 2026-11-16

### Added
- **Library shortcut after a Now Playing download:** when a download started from Now Playing finishes, the Download tile turns into **Library**. One tap opens Downloads scrolled to that song; Back returns to Now Playing and the tile disappears.

### Changed
- **Signal Path redesigned as a bottom sheet:** verdict, `Source → Output`, the quality selector, every measured path check, stream health and internal/USB details. Values that were never measured (tone, preamp, buffers, latency, fidelity/DSP scores, fixed output flags) are gone, and unknown bit depth or sample rate is no longer guessed.

### Removed
- **Karaoke Vocal Reduce:** removed from lyrics, Settings and the audio engine because it did not work reliably.

### Fixed
- **CI build:** the APK signature check no longer fails on every build with a `bad substitution` error.

## [5.3.3] - 2026-11-16

### Fixed
- **Car Android Head Unit (`2000×1200`) & Split-Screen UI Overlap Fixes:**
  - **Now Playing:** Removed upward artwork lift (`landscapeArtLift = 0.dp` on viewports `< 640dp` tall) so the album cover never collides with `NowPlayingHeader`; added responsive vertical spacing and scroll fallback to the right-hand controls pane in `FullPlayerLandscape`, and enabled two-column landscape layout and ultra-compact portrait spacing in split-screen windows.
  - **Lyrics Page:** Kept `NowPlayingHeader` and the Back button visible by default when opening Lyrics on landscape/car displays, scaled down base lyric typography (`0.76x–0.84x`) on short viewports (`< 560dp`), and compacted the floating utility (`Mic`, `Fullscreen`, `More`) and transport buttons in `ModernLyricsControls` so lyrics and controls never overlap.
  - **Home UI & Bottom Chrome:** Auto-enabled collapsible navigation bar docking on short car viewports (`< 560dp`), scaled down the Comic Pop bottom scrim height dynamically with dock progress in `PlayerHost`, top-aligned `hero_split` columns in `FeedScreen`, and balanced `ChartQuickChipsRow` pills in `SoundNestHomeSections`.

## [5.3.2] - 2026-11-16

### Added
- **Live Audio Quality Switcher in Now Playing:** Added `NowPlayingQualitySelectorCard` inside both the Now Playing Quality Badge dialog (`SignalPathDialog`) and the `AudioControlsSheet` so users can switch between `Dolby Atmos`, `Hi-Res Lossless`, `CD Lossless`, `Standard 320k`, `YouTube Music`, `Data Saver`, and `Auto` (Cellular) in one tap with instant mid-song stream hot-swapping (`MusicPlayer.setActiveStreamingQuality`).

### Removed
- **Launcher Icon Long-Press Shortcuts:** Removed the dynamic launcher shortcuts (`Shuffle Mix`, `Search`, `Downloads`) and cleared existing dynamic shortcuts on startup (`LauncherShortcuts.clear`).

## [5.3.1] - 2026-11-16

### Changed
- **Redesigned In-App Update Dialog:** Upgraded `AppUpdateDialog` with a rounded icon badge, version transition pill (`v5.2.9 → v5.3.0`), structured `RELEASE NOTES` card, package size pill, and spaced clickable `"Don't show again for this version"` toggle.
- **Automated Changelog Release Notes & Faster CI Builds:** Updated GitHub Actions (`release.yml` and `build.yml`) to extract release notes directly from `CHANGELOG.md`, cache C++/CMake `.cxx` and Gradle build outputs across tags, and target phone ABIs (`arm64-v8a`, `armeabi-v7a`).

### Fixed
- **Clean Release Notes Markdown Rendering:** Sanitized Markdown table rows, `| :--- |` alignment lines, `> [!NOTE]` callout tags, and nested backtick-bold formatting in `ReleaseNotesText`.

## [5.2.9] - 2026-11-16

### Changed
- **4-Color Aurora Brand Identity Across Icons, Splash & Header:** Updated the `CW` emblem (`ic_splash_logo`, `ic_launcher_wave_foreground`, `ic_launcher_wave_badge`, and adaptive/legacy launcher icons), the 3D Earth Globe continents/soundwave/headphones/notes, the 10 pixel-locked Equalizer Pulse Bars, and the `ClaudeWaves` brand headers (`AudioWavesBrandMark` and `AboutCard`) to the 4-color Aurora palette (`Cyan #0ECAE8`, `Cobalt #267CFF`, `Violet #B05CFF`, `Coral Rose #FA4C6E`) over Midnight Aurora (`#080C1C`).
- **MiniPlayer Quality Badge Unified with Song Lists:** Replaced the double-bordered MiniPlayer quality box with the compact `LosslessTrackBadge` surface + glyph design (`HI-RES` / `LOSSLESS`) used in song rows.

### Fixed
- **Seamless Equalizer-to-`CW` Splash Handoff:** Pixel-locked all 10 Equalizer Pulse Bars (`EqBars`) with 3-stop vertical gradients to the exact columns of `ic_splash_logo.png` and replaced the `-45°` radial wedge with a synchronized left-to-right split curtain (`cwCurtainX`) and exact connected-component `wRect` bounds so bars never double-overlap or slice diagonally.
- **Centered Lyrics Transport Controls:** Anchored the `Previous`, `Play/Pause`, and `Next` buttons in `ModernLyricsControls` inside a full-width `Box` with `Alignment.Center` so wider remaining-time labels (`−3:35`) never push the transport buttons left of center.
- **Symmetrical Right Margin on Home Filter & Chart Chips:** Aligned `ChartQuickChipsRow` (`Indonesia` pill), `PinnedChipsBar`, and `HomeSearchBarPill` flush with the right edge of `QuickAccessRow` (`Favourites`) and `FeaturedPlaylistCarousel`.

## [5.2.8] - 2026-11-16

### Changed
- **Equalizer Pulse Bars & Natural 4.0s Splash Pacing:** Extended the cold-start splash duration (`4000ms`) with a longer hold on the rotating Earth + Studio Headphones + Music Notes scene and final Midnight Carbon Aurora logo, and replaced the transition wedge with 7 rhythmic Equalizer Capsule Bars (`drawEqualizerPulseBars`) that pulse out of the Earth's equator and lock directly into the vertical capsule bars of the `CW` emblem.

## [5.2.7] - 2026-11-16

### Changed
- **Seamless Earth Music Aurora Splash Screen:** Redesigned the cold-start launch animation (`ComicSplash`) to transition smoothly from the indigo launcher palette into a low-glare Slate-Titanium Mist featuring a rotating 3D Earth orb with Studio Headphones, floating music notes, and an equatorial sonic wave, flowing through the crescent swoosh into the Reverse-XOR Midnight Carbon Aurora reveal of `CW` and `ClaudeWaves`.

## [5.2.6] - 2026-11-16

### Changed
- **Inline Lossless Badges Across All Song Lists:** Redesigned lossless, Hi-Res, and Dolby Atmos indicators as clean Apple Music-style inline micro-badges (`TrackSubtitleWithBadge`) next to track metadata across Feed, Home, Discover, Genres, New Releases, Search, Album/Artist/Playlist details, Friend Profile, and Queue.

### Fixed
- **List vs. Now Playing Format Accuracy:** Synchronized list badges with the actual decoded quality played in Now Playing (`MusicPlayer.syncPlayedQualityBadge`), prevented false Hi-Res inference when streams answer at CD quality, and improved downloaded format tier classification (`HI-RES` / `LOSSLESS` / `ATMOS`).

## [5.2.5] - 2026-11-16

### Changed
- **Single Universal APK (Android 9 – 17):** Lowered default `minSdk` to API 28 (Android 9.0) across `:app` and `:audio:decent-usb-audio-driver`, updated `AppUpdateManager` to serve the universal APK directly to Android 9+ devices, and streamlined `.github/workflows/release.yml` to publish one signed `ClaudeWaves.apk` per release.
- **Concise Project Overview:** Simplified `README.md` to focus on ClaudeWaves features, Android 9–17 installation, and GPL v3 attribution.

## [5.2.4] - 2026-11-16

### Added
- **Now Playing Download Button Toggle:** Added an Experimental setting (`Settings -> Appearance -> Experimental -> Now Playing Download Button`, **OFF by default**) to show or hide the quick Download tile in Now Playing while streaming online tracks.

### Changed
- **Now Playing Download Action:** Updated the tile label from `"Hi-Res"` to `"Download"` (with live `%` progress and `"Saved"` state) and made it follow the user's configured `Download quality` preference. Automatically stays hidden when playing local/imported audio files.

## [5.2.3] - 2026-11-16

### Added
- **In-App APK Downloader & Installer:** Download and install new GitHub releases directly inside ClaudeWaves with live progress, cancel/retry support, and Android 8+ "Install unknown apps" permission handling (`AppUpdateManager`, `AppUpdateDialog`).
- **Now Playing Hi-Res Download Button:** Download audio in Hi-Res directly from the Now Playing controls alongside existing quality options.

### Changed
- **ClaudeWaves Rebrand & GPL v3 Attribution Cleanup:** Replaced inherited upstream branding, Telegram/Discord promo dialogs, links, notifications, Share cards, Discord Rich Presence assets, localized strings, and CI release workflows with ClaudeWaves (`aayepp/ClaudeWaves`) while retaining clean GPL v3 attribution to `Clash-Projects/LastWave-native`.
- **Lyrics Synchronization:** Improved per-track lyrics offset persistence and matching stability.
- **Automated Tag-Push Releases:** Updated `.github/workflows/release.yml` to build and publish signed `ClaudeWaves` release APKs on `v*` tag pushes and normalized `.github/workflows/build.yml` fallback behavior when signing secrets are absent.

### Removed
- **Driving Mode Button:** Removed the non-functional Driving Mode action from Now Playing.
- **Upstream Promo & Legacy Assets:** Removed inherited upstream community popups, app promos, and the obsolete `5.2.1` release-assets workflow.

## [5.2.2] - 2026-11-15

### ✨ What's New

* **Viral, Weekly & Monthly Regional Charts:** Switch Home charts and Discover anytime between **Viral Now**, **Weekly Top**, and **Monthly Hits** across regions (Malaysia, Indonesia, Korea, Global, Japan, US, UK, and more) with 1-tap quick chips directly on **Home** and **Discover**, or customize defaults in **Settings → Home Sections**.
* **In-App What's New & Changelog:** Browse release notes inside **Settings → About → What's New & Changelog**, plus a one-time summary popup after an app update.
* **Lossless Badges on Song Lists:** Spot **Hi-Res** and **Lossless** indicators directly on tracks in playlists and song lists before starting playback.
* **Sort by Latest Liked:** Sort imported YouTube Music and Spotify liked playlists by **Latest Liked** to keep your newest liked songs first.

---

### ⚡ Improved

* **Smarter Live Chart & Release Parsing:** Unlocked YouTube Music **Trending 20** viral charts and **New Releases** album browsing (`MPREb_`), skipped "Live Performances" mispicks on Korea/US/Japan/UK charts, and wove regional chart hits directly into **Recommended for You**.

## [5.2.1] - 2026-11-13

### ✨ What's New

* **Downloads in Library:** Find your saved music and download count directly in **Library**.

---

### ⚡ Improved

* **Queue & Navigation Motion:** Smoother drag reordering and release, with swipe-to-remove and undo. Navigation and the mini player now collapse together while scrolling, with matching glass styling.
* **Quality Switching & Wavy Seekbar:** More reliable selected-quality startup, preloading and Bit-Perfect transitions. Restored the lighter wavy seekbar in player and lyrics views (**Experimental; off by default**).

---

### 🛠️ Fixed

* **Downloads & 59-Second Stops:** Restored YouTube downloads, improved interrupted-download recovery, and corrected stream selection behind premature playback stops.
* **Addon Playback & Offline Audio:** Prevented upgrades to a different recording or language. Corrected Atmos download labels and restored seeking/duration for newly downloaded FLAC tracks.
* **Local Library:** Folder scans now follow your selected locations, remove out-of-scope entries and filter recordings while retaining likes and playlists.
* **Likes, Playlists & Mixes:** Fixed guest/YouTube like sync, restored notification like/shuffle controls, corrected playlist sorting/counts, and made Start Mix update the queue correctly.
* **Player, Lyrics & Appearance:** Fixed black screens when reopening the player mid-close, cleaned malformed fallback lyrics, and corrected artwork colours, widget layouts and header seams.
* **Audio & Playback Crashes:** Fixed Parametric EQ and BotGuard crashes, and restored external Dolby/EQ session support.
* **Listen Together:** Updated Jam server and invitation links. Also refreshed Italian translations.

> [!NOTE]
> Previously affected FLAC downloads need to be downloaded again to gain the seeking fix.

## [5.2.0] - 2026-11-12

### ✨ What's New

* **Signature Liquid Glass Navigation Bar:** A new refractive glass selector lets you tap or drag between Feed, Stats and Library, with fluid motion and support for both navigation layouts. Available on **Android 13+** with **Settings → Appearance & Visuals → Liquid Glass** enabled.
* **Local Music Hub & Library:** The Playlists tab is now **Library**, with a dedicated Local Songs hub for browsing songs, albums, artists and folders. Create offline playlists, keep local likes, search your device music, and view embedded or `.lrc` lyrics. Local playback now also supports **DSD (DSF / DFF) and AIFF** files.
* **Adaptive Streaming & Per-Network Quality:** Keep listening on slower mobile connections with **Auto** streaming quality, which adjusts to your connection speed and buffer health to help reduce buffering. Choose separate quality settings for Wi-Fi and mobile data; mobile Auto stays within your Wi-Fi selection. The new **Settings → Audio & Playback → Auto upgrade to Hi-Res** toggle lets you control automatic lossless upgrades.
* **Vietnamese Language Support:** Added Vietnamese to the app's language options.

---

### ⚡ Improved

* **Performance & Background Resource Usage:** Reduced unnecessary redraws, CPU work and playback wakeups throughout the app. Hidden animations and background YouTube library polling now pause when they are not needed, while progress updates follow the visible player to keep playback responsive with less overhead.
* **Smoother Scrolling & Player Opening:** Optimized Home shelves, artwork backgrounds, loading shimmers, downloads lists and mini-player gestures. The full player is prepared before its opening animation, reducing the initial stutter when expanding Now Playing.
* **Faster Startup & Smarter Cache Controls:** Moved artwork cache scans and storage calculations off the main thread. Artwork and streamed audio now share one configurable cache budget, with limits up to **4 GB**, while saved downloads remain separate.
* **Faster, Cleaner Quality Switching:** Improved stream alignment and sample-rate handling for mid-song quality changes, with faster repeat switches and a reliable **"Upgrading to" / "Switching to"** indicator. Refined handoffs also reduce jumps and crackling during lossless upgrades.
* **Glass Headers, Artwork Themes & Synced Videos:** Content now scrolls beneath glass headers across more screens, and artist/album pages use unified full-bleed artwork. Now Playing artwork blends into the wallpaper theme with better color selection for monochrome covers. Synced music videos have improved matching and correct fullscreen landscape proportions.

---

### 🛠️ Fixed

* **YouTube Music Playback Stopping Around 59 Seconds:** Fixed the stream and retry issues that could leave a song stuck around **59 seconds / one minute**. Playback now uses bounded media requests and can refresh expired or rejected stream URLs mid-song. Failed YouTube downloads also retry with a freshly resolved stream.
* **VPN & Age-Restricted Playback:** Improved recovery through VPN/network drops, signed-in age-restricted stream resolution, and YouTube request retries so a failed request does not break later searches or playback.
* **USB DAC, Loudness & Spatial Audio:** Fixed delayed volume changes on exclusive USB output, corrected loudness normalization and Dolby gain, and improved Cayin RU3 clock/feedback handling and spatial-audio routing. Added ALAC DASH playback support and improved selection of full-volume YouTube audio streams.
* **Shuffle, Likes & Playlist Sync:** Shuffle now prioritizes unplayed songs. Fixed duplicate-song removal and saved playlist ordering, restored reliable connected YouTube playlist deletion, and combined local/YouTube likes into one heart button. Spotify playlist counts and live sync are more reliable.
* **Search, Imports & Downloads:** Fixed stale search results and playlist previews, incorrect recording selection from track menus, and imports using a different playlist from the reviewed preview. Custom download folders retain their access permissions.
* **Account & UI Stability:** Fixed false Last.fm logouts, older-Android notification and startup crashes, stale cover art, and several pagination and floating-control issues. Lyrics sharing keeps the selected song consistent, and player volume, lyric offsets and sleep-timer interactions now behave correctly.

## [Unreleased]

### Added & Improved
- **Recommendation Provider Switch:** Settings → Spotify now offers a recommendation provider choice. Pick Spotify to seed Home shelves, Quick Picks, radio, Discover and mixes from your Spotify taste; the switch applies everywhere and invalidates the cached home feed. Spotify only supplies the seeds — tracks are matched back to playable YouTube Music, and any failure or empty answer silently keeps the YouTube Music graph, so playback never breaks.

## [5.0.0] - 2026-10-30

### Added & Improved
- **LastWave Jam (Listen Together & Live Chat):** Host or join real-time synchronized listening rooms with friends, featuring low-latency playback sync, shared queue control, and built-in live room chat.
- **Synced Music & Podcast Videos (With Lossless Audio):** Watch full Official Music Videos and video podcasts directly inside the player via a native high-frame-rate video surface, seamlessly synchronized to your active Hi-Res Lossless FLAC or Opus audio stream without sacrificing audio fidelity.
- **Native Spotify Integration & Canvas:** Connect your Spotify account for native one-tap playlist and liked-track transfers alongside looping high-resolution Spotify Canvas visuals inside Now Playing.
- **Full Podcast Support:** Browse, stream, and follow podcast shows and episodes with dedicated episode queues, chapter navigation, and persistent playback progress.
- **Harmonic Automixing & Enhanced Crossfade:** Added intelligent beat- and tempo-aware automixing with upgraded dual-deck crossfade transitions for seamless, gap-free track blending.
- **Overhauled Homepage & Discovery Algorithm:** Redesigned the Home screen with a dynamic YouTube Music–style feed backed by an upgraded recommendation and taste-matching engine for smarter daily mixes, quick picks, and personalized shelves.

### Changed & Fixed
- **Bit-Perfect & Zero-Distortion Audio Fixes:** Resolved USB DAC Exclusive Mode routing issues, hardware sample rate handshakes, and FLAC/PCM clipping or distortion artifacts across devices for a clean, untouched signal path.
- **Refined Lyrics UI & Sync Stability:** Upgraded the real-time synchronized lyrics interface with smoother scroll physics, cleaner typography, improved word-by-word karaoke timing alignment, and higher overall rendering stability.
- **Performance, Stability & Bug Fixes:** Eliminated idle recomposition overhead, scroll hitches, and UI glitches across the app alongside extensive under-the-hood stability and memory optimizations.
- **Version Bump:** Bumped version to 5.0.0 (versionCode 27).

## [4.3.1] - 2026-10-18

### Added & Improved
- **YouTube Music Cookie Login:** Support for raw `Cookie` headers, Netscape `cookies.txt`, and browser extension JSON exports with clipboard paste helpers and real-time validation (matching Desktop).
- **Poweramp-Style Signal Path Redesign:** Reorganized the Output Detector dialog into Poweramp's 5-stage pipeline flow (Source $\to$ DSP $\to$ Volume Control $\to$ Output $\to$ Output Device) with status cards.
- **Smart Feed Queues & Continuation:** Tapping any track in feed shelves now starts context-aware radio (`startRadio = true`) for dynamic playback, while "Play All" and "Shuffle" continuously auto-extend queues.
- **Discovery Recommendation Quality:** Added strict deduplication against listening history and saved playlists, capped artists to 1 per batch, and removed raw history feed injection.
- **Faster Feed Loading:** Eliminated blocking artist page network fetches, added in-memory artist caching, and bounded discovery graph queries with timeouts for near-instant rendering.

### Changed & Fixed
- **Crossfade Audio Focus:** Fixed pause loop in standby player handoff by preventing ExoPlayer focus manager collisions.
- **USB DAC Bit-Perfect Alignment:** Aligned bit-perfect detection with Poweramp definitions and protected against read-only direct buffer crashes.
- **Version Bump:** Bumped version to 4.3.1 (versionCode 26).

## [4.3.0] - 2026-10-17

### Added & Improved
- **Share Lyrics Card:** Select up to 5 lines from synced or plain lyrics to generate styled lyric cards. Choose between Song Mode (album art header + track metadata + lyrics) and Lyrics Mode (clean typography), customize with dynamic palette extraction (cover tones, dominant, muted, dark, accent, or LastWave Lime), and export via the system share sheet or save directly to Pictures/LastWave with full multilingual font rendering (Latin, Devanagari, Arabic, CJK).
- **Lyrics Romanization & Translation:** Added inline romanization and real-time translation support for non-Latin scripts directly within the lyrics view.
- **Expanded Lyrics Providers:** Integrated additional lyrics sources for broader catalog coverage, higher match rates, and more reliable synced timestamps.
- **Faster Playback & Multi-Provider Addons:** Optimized stream initialization for faster playback startup and introduced simultaneous multi-provider addon support for an expanded, unified library.
- **Motion Lyrics Background:** Added dynamic, fluid motion canvas backgrounds synchronized to the active album art inside the lyrics screen.
- **UAC1 & USB DAC Bit-Perfect Fixes:** Resolved UAC1 compatibility issues, improved hardware sample rate handshakes, and stabilized bit-perfect USB DAC audio routing.
- **Local Device Library Support:** Added full on-device audio scanning, metadata parsing, and seamless local song playback right alongside your streaming library.
- **100% Localization Coverage:** Complete translation across all 12 supported locales.
- **Dynamic APK Variant Selection:** Automatically detects device architecture and system version to download the appropriate APK variant (universal, uncompressed, or android7).
- **Audio Fallback:** Falls back to stereo lossless when Dolby Atmos is unavailable.

### Changed & Fixed
- **Version Bump:** Bumped version to 4.3.0 (versionCode 25).

## [4.2.4] - 2026-10-05

### Added & Improved
- **Resource Usage & Smooth UI:** Optimized rendering pipelines, reduced background resource contention, and smoothed UI interactions.
- **Fast YouTube Search:** Restored instant 2-pass YouTube search resolution, eliminating multi-stage search fallback delays.
- **Dolby Atmos Downloads:** Aligned spatial audio capability detection in `TrackDownloadManager` with `MusicPlayer`'s system `Spatializer` check, resolving Atmos download skips on Android 12L+ devices.
- **DASH Manifest Parser Robustness:** Added resilient XML attribute matching, case-insensitive tag handling, and strict URL scheme checks to prevent malformed segment URLs during segmented downloads.
- **Playback Warmup & Retry Gating:** Implemented first-song OPUS warmup and tap-to-retry gating to prevent premature auto-skips on transient stream hiccups.

### Changed & Fixed
- **Bit-Perfect Audio Issues:** Resolved bit-perfect DAC routing, format lockups, and hardware sample rate mismatches.
- **Lyrics Sync:** Stabilized lyrics synchronization, eliminated drift, and improved word-by-word timing alignment.
- **Stretchy Cover Arts:** Fixed stretchy full-bleed cover art visual artifacts, canvas fade transitions, and fluid backdrop rendering.
- **Version Bump:** Bumped version to 4.2.4 (versionCode 24).

## [4.2.3] - 2026-10-03

### Added
- **Stretchy Artwork Fade:** Implemented responsive stretchy artwork fade on Now Playing full bleed view, vertically stretching and smoothly dissolving into the dark backdrop with an offscreen gradient mask and dark scrim.
- **Addon Bit Depth & Clock Rate Extraction:** Parsed bit depth and clock rate metadata across camelCase and snake_case properties from addons, preserving source-reported 24-bit depth on standard 44.1/48kHz tracks.

### Changed & Fixed
- **Settings Reorganization:** Relocated System Audio Effects from Appearance to Audio → Output & Loudness right beside Equalizer.
- **Version Bump:** Bumped version to 4.2.3 (versionCode 23).

## [4.2.2] - 2026-09-27

### Added
- **System Audio Effects mode (Experimental, default OFF):** publishes the audio session for external equalizer apps and OEM Dolby, flattens in-app DSP airtight on mixer routes, auto-suspends on bit-perfect / USB exclusive.
- **Diagnostics capture:** crash-guard log and startup trail included in the diagnostics export; startup stage breadcrumbs for instant-kill diagnosis.

### Fixed
- **Settings organization:** Equalizer and Studio Clarity moved to Audio → Output & Loudness, liquid glass to Appearance, Experimental reindexed.
- **Playlist duplication races:** serialized add/remove, auto-suffix same-title siblings, monotonic ids.
- **Atmos honesty:** JOC-only capability check plus spatial-manifest veto with step-down cascade to stereo / Opus.
- **Hi-res reporting:** sample-rate driven 24-bit depth inference across resolve, quality pill, signal path, and track details.
- **Lyrics readability:** lyrics-tab blur boost plus readability veil; adaptive transport row no longer clips Next on small screens.
- **Build:** missing YouTubeMusicTrack import, composable-safe backdrop guard.

## [4.2.1] - 2026-09-26

### Added
- **True Bit-Perfect Audio Pipeline:** Introduced true bit-perfect hardware output with native C++ round-to-nearest-even conversion (`lrintf` / `llround`) in `usb-audio-output.cpp`, eliminating 1-LSB negative sample truncation on 24-bit FLAC streams. Added direct C++ integer bit-depth packing to bypass JVM conversion overhead.
- **Dual-Crystal UAC2 Hardware Clock Switching:** Integrated automatic parsing of UAC2 `CLOCK_SOURCE` and `CLOCK_SELECTOR` descriptors to switch dynamically between 44.1 kHz and 48 kHz oscillator families on supported DACs. Added smart Soxr resampling fallback for single-crystal DACs.
- **Expanded Lyrics Ecosystem:** Added support for Apple Music, Better Lyrics, Bini, Musixmatch, and SimpMusic lyrics providers, accompanied by TTML Rich Sync syllable parsing and word-by-word karaoke line splitting.
- **Fluid Artwork Background & Large Widget:** Added animated fluid gradient mesh backgrounds to the player sheet and introduced a new Large Now Playing home screen widget.

### Changed & Improved
- **Audio Download Matching Precision:** Overhauled the download matching engine to apply strict artist penalties, expected duration validation (within 5 seconds), and direct video ID routing from track menus, preventing cross-artist mismatches during downloads.
- **Signal Path Diagnostics:** Signal path dialog now displays exact DAC resampled status, original source bit depth (preventing 32-bit float decoder representation from masking track depth), active hardware sample rates, and detailed USB exclusive open failure diagnostics.
- **Volume & Output Unity Gain:** Removed the restrictive `ignoreStreamMusicMax` cap in `ExclusiveUsbOutput` so 100% volume consistently achieves true 1.0f unity gain.

### Fixed
- **YouTube Liked Music & Playlist Crash:** Fixed a fatal `LazyColumn` key collision and layout edge-reorder crash when opening connected YouTube playlists (`LM` / `yt_liked`). Replaced unstable keys with synchronized, collision-proof occurrence suffixes and guarded layout info reads during progressive track streaming.
- **USB Audio Timing & Glitches:** Corrected exclusive output timing to eliminate buzzing, forward-seek audio cutoffs, runaway seekbar animations, and pause spinner hangs.
- **Player UI Fixes:** Resolved seekbar buffering clock glitches, repeat-one track skipping, and pill button white-out visual issues.

## [4.2.0] - 2026-09-23

### Added
- **Dolby Atmos Playback:** Added Dolby Atmos playback support for compatible devices and audio configurations, providing an enhanced spatial audio experience.
- **Enhanced Liquid Glass:** Completely reworked the Liquid Glass system. The previous implementation was primarily a translucent blur effect; the new implementation introduces significantly improved depth, translucency, reflections, layering, highlights, and overall visual polish for a more refined glass experience.
- **Playlist Downloads:** Added support for downloading complete playlists for offline listening, making it easier to manage and save large collections.
- **True Bit-Perfect Playback:** Improved bit-perfect playback with untouched DAC routing, preserving the audio output path without unnecessary modification or processing.
- **YouTube Recommendation Algorithm:** The entire recommendation system has been shifted from Last.fm to YouTube, providing a new recommendation backend while making Last.fm completely optional.
- **Guest Login:** Added Guest Login, allowing users to use LastWave without creating or connecting an account.
- **Native Spotify & Apple Music Playlist Import:** Added native playlist importing from Spotify and Apple Music, allowing users to bring their existing playlists directly into LastWave.

### Changed & Improved
- **Playback & Audio Improvements:** Improved playback reliability, audio handling, DAC behavior, and various edge cases across the playback pipeline.
- **UI & Performance Improvements:** Refined multiple parts of the interface with improved responsiveness, animations, visual consistency, and overall performance.
- **Under-the-Hood Improvements:** A large number of architectural, performance, reliability, and quality-of-life improvements have been made throughout the app.

### Fixed
- **Stability & Bug Fixes:** Fixed numerous reported issues across playback, downloads, recommendations, authentication, UI, and background behavior.

## Unreleased

### Fixed
- **Playback continuing after LastWave was swiped from Recents.**
  `MusicPlaybackService.onTaskRemoved()` was a no-op that relied on
  `android:stopWithTask="true"`, but the service deliberately stays a
  media-playback foreground service whenever a track is loaded (even paused),
  and Android does not stop a foreground service on task removal. Task removal
  is now handled explicitly: playback stops and the service, cast session,
  notification and wake/wifi locks are torn down, while the last queue is kept
  restorable for the next launch. A new Audio & Playback toggle — "Stop
  Playback When Swiped Away", on by default — preserves background playback
  for users who want it.

  Files changed:
  `app/src/main/java/com/lastwave/app/playback/MusicPlaybackService.kt`,
  `app/src/main/java/com/lastwave/app/playback/MusicPlayer.kt`,
  `app/src/main/java/com/lastwave/app/data/local/SettingsPreferences.kt`,
  `app/src/main/java/com/lastwave/app/ui/settings/SettingsViewModel.kt`,
  `app/src/main/java/com/lastwave/app/ui/settings/SettingsScreen.kt`,
  `app/src/main/java/com/lastwave/app/ui/settings/SettingsSearchIndex.kt`,
  `app/src/main/res/values/strings.xml`,
  `app/src/main/AndroidManifest.xml`

### Added
- **One-tap "Save album to library" on the album detail screen (#79).**
  Previously an album could only be kept by adding its songs one by one to
  a custom playlist. The album hero now has a save button that stores all
  tracks via `PlaylistRepository`, mirroring the feed playlist detail's
  save flow (saving / saved / error states). Shared-playlist pages already
  had this via the feed detail screen.

  Follow-up: the button now detects an already-saved album on every load
  (reopening shows "Saved to library") and saving is idempotent — a
  same-title library copy with overlapping tracks is reused instead of
  stacking duplicates when the tracklist loads progressively.

  Files changed:
  `app/src/main/java/com/lastwave/app/ui/album/AlbumViewModel.kt`
  `app/src/main/java/com/lastwave/app/ui/album/AlbumDetailScreen.kt`
  `app/src/main/java/com/lastwave/app/data/playlist/PlaylistRepository.kt` (`findByTitle`)

### Fixed
- **Search returning no results for Cyrillic / non-Latin queries (#102).**
  Title/artist matching used a Latin-only word pattern (`[^a-z0-9]+`),
  reducing every non-Latin query to blank, and the Songs-filtered search had
  no fallback when the filter matched nothing. Matching now uses a
  Unicode-aware pattern (letters + numbers from any script) with
  locale-independent case folding, and an empty filtered search retries once
  unfiltered. Matching helpers were extracted to `TextMatch` with unit tests
  covering Cyrillic, CJK and Arabic input.

  Files changed:
  `app/src/main/java/com/lastwave/app/data/music/TextMatch.kt` (new),
  `app/src/main/java/com/lastwave/app/data/music/InnerTubeMusicApi.kt`,
  `app/src/test/java/com/lastwave/app/data/music/TextMatchTest.kt` (new)

### Fixed
- **Home-screen widget stuck on the previous song (#94).**
  Two publishers wrote the same widget snapshot with independent dedup
  guards: the playback service (authoritative, fires on every player-state
  transition) and the scrobble listener (watches all media sessions,
  including our own, on an async binder timeline). During a track change
  the listener could still see the previous track's session metadata and
  overwrite the fresh snapshot last, with nothing re-firing afterwards.
  The listener now yields while our own session is active and otherwise
  only elects external packages, and snapshot write + refresh in
  `WidgetUpdater` is mutex-serialized against interleaved publishers.

  Files changed:
  `app/src/main/java/com/lastwave/app/service/MediaScrobbleListenerService.kt`
  `app/src/main/java/com/lastwave/app/widget/WidgetUpdater.kt`

### Fixed
- **Logged-out YouTube Music playlists collapsing to a dead "Tap Retry" error.**
  `InnerTubeMusicApi.fetchPlaylist()` treated ANY single continuation-page
  failure (rate-limit/offline blip while paging a large playlist) as a total
  failure and returned null — discarding tracks already loaded — so the
  detail screen showed `Error` and every retry re-fetched from scratch into
  the same failure. Continuation failures now keep the collected prefix and
  return it with `isComplete = false` (null is kept only when zero tracks
  loaded, preserving every caller's existing contract); failures are logged
  to logcat under `LastWavePlaylist` with browseId + stage for diagnosis.
  `FeedPlaylistDetailViewModel` shows truncated results with an inline
  "Some tracks couldn't load. Retry." affordance instead of the dead error,
  and `YtMusicLibraryManager` no longer poisons the disk cache / track
  count with truncated snapshots (null count forces a network refresh next
  open).

  Files changed:
  `app/src/main/java/com/lastwave/app/data/music/InnerTubeMusicApi.kt`
  `app/src/main/java/com/lastwave/app/ui/feed/FeedPlaylistDetailViewModel.kt`
  `app/src/main/java/com/lastwave/app/data/ytmusic/YtMusicLibraryManager.kt`

### Author: musaibbhat120605
**Date:** September 16, 2026

###Musaib Bhat will step down as the developer of LastWave on 16 September 2026. His contributions have been invaluable, and his work will always remain a cornerstone for our community.

#### Added
- **SongPlayStatsEntity + SongPlayStatsDao** — local per-track listening stats (total play time, skip count, play count, last played).
- **SongPlayStatsRepository** — records listened time and skips.
- **LocalTasteSuggestionEngine** — local, Last.fm-free recommendation engine (ported scoring model: play time, skip penalty, liked bonus, recency, time-of-day), seeded via existing `InnerTubeMusicApi.fetchRelatedSongs`.
- **Room migration 12 → 13** — creates `song_play_stats` table without wiping existing data.

#### Changed
- **MusicPlayer.kt** — hooks `onMediaItemTransition` to record listened duration and detect skips (non-AUTO transition + <85% played).
- **AppDatabase.kt / DatabaseModule.kt** — registers the new entity/DAO, bumps DB version, adds migration.
- **FeedScreen.kt** — redesigned `QuickTilesGrid`/`QuickTileCard` (Liked Songs / Mix / New Releases): equal-width scrollable cards with per-type gradient icons, replacing the old grid-chunked layout that left an orphaned half-row.

#### Not yet wired
- `LocalTasteSuggestionEngine.run()` isn't called from any screen yet — integration notes are in the file itself (Feed's discover section or as a Last.fm fallback in `GenerateRepository`).

### Fixed
- **Player state and cached track lost after leaving the app in the background with nothing playing.** (Fix by [@musaibbhat120605](https://github.com/musaibbhat120605))

  `MusicPlaybackService.onTaskRemoved()` unconditionally called
  `musicPlayer.stopAndClear()` whenever the task left Recents — even when
  nothing was playing. `stopAndClear()` wipes both the in-memory player
  state and the persisted session in SharedPreferences
  (`clearPersistedPlaybackSession()`), so the next launch had nothing to
  restore: the player appeared closed and the last track wasn't cached,
  regardless of battery-optimization settings.

  Fixed by (1) skipping the stop entirely when something is actively
  playing, so playback isn't killed just because the task left Recents,
  and (2) giving `stopAndClear()` an optional `clearSession` parameter
  (default `true`, unchanged for every other call site) so the
  paused/idle case can stop the service without deleting the persisted
  session — leaving it restorable on the next launch.

  Files changed:
  `app/src/main/java/com/lastwave/app/playback/MusicPlaybackService.kt`,
  `app/src/main/java/com/lastwave/app/playback/MusicPlayer.kt`

- **Songs silently disappearing from the Home listing during background polling.** (Fix by [@musaibbhat120605](https://github.com/musaibbhat120605))

  `HomeViewModel`'s 12-second background refresh loop merged newly
  polled recent tracks with the existing in-memory history and then
  truncated the **entire combined list** to `HOME_TRACK_HISTORY_CAP`
  (500 entries). `loadNextPage()` (triggered by scrolling) appends
  paginated tracks without any cap of its own — so once a user
  scrolled far enough to load more than 500 tracks, the very next
  background poll would silently drop everything past position 500,
  including tracks the user had just scrolled into view seconds
  earlier. This made songs appear to randomly vanish from the Home
  listing with no user action to explain it.

  Fixed by making the background poll's cap dynamic: it now only
  bounds organic growth from polling (`max(HOME_TRACK_HISTORY_CAP,
  currentListSize)`), so it can never truncate below what pagination
  has already legitimately loaded into view.

  Files changed:
  `app/src/main/java/com/lastwave/app/ui/home/HomeViewModel.kt`

- **Duplicate/overlapping "now playing" notification on Android 10 (One UI 2.x).**
  `buildNotification()` used `Notification.DecoratedMediaCustomViewStyle`
  with a `MediaSession` attached, alongside a fully custom `RemoteViews`
  player (own artwork, title, artist, transport buttons). On Android 10 +
  Samsung One UI 2.x, SystemUI's older media-notification renderer drew
  its own full media chrome as a second layer instead of just framing the
  custom view, producing two overlapping players in the notification
  shade/quick controls.

  Now version-gated: Android 11+ keeps `DecoratedMediaCustomViewStyle`
  with the session attached as before; Android 10 and below uses
  `Notification.DecoratedCustomViewStyle` (no session tag on the
  notification itself). Lock screen controls, Bluetooth, Android Auto,
  and the in-app widget are unaffected, since they all read from
  `mediaSession` directly rather than this notification's `Style` object.

### Added
- **Offline playback priority across all screens (Fixes #31).**
  `MusicPlayer.resolveTrackAudioStream()` now checks the local Room database (`DownloadedTrackDao`) and verifies file presence on disk before attempting remote network resolution (Lossless / YouTube Music / InnerTube). When a track has already been downloaded, LastWave plays the local media file directly without making network calls, enabling seamless offline playback across Home, Search, Playlists, Album, and Artist screens and saving cellular data when online.

  Files changed:
  `app/src/main/java/com/lastwave/app/playback/MusicPlayer.kt`

- **Download state awareness and duplicate download prevention.**
  - Added `DownloadedTrackDao.findByTrackKey` and deduplication checks in `TrackDownloadManager.downloadTrack` to prevent duplicate download jobs, redundant network requests, and duplicate files (e.g. `(1).flac`) in MediaStore.
  - The 3-dot context menu sheet (`TrackContextMenuSheet`) now dynamically reflects the track's status (`Downloaded` with check icon, `Downloading…`, or `Download (Max Quality)`), giving instant visual feedback and preventing accidental re-downloads.

  Files changed:
  `app/src/main/java/com/lastwave/app/data/local/db/DownloadedTrackDao.kt`
  `app/src/main/java/com/lastwave/app/data/download/TrackDownloadManager.kt`
  `app/src/main/java/com/lastwave/app/ui/common/TrackContextMenuSheet.kt`

- **Direct navigation from download notifications to the Downloads screen.**
  `TrackDownloadManager` now fires pending intents with `ACTION_VIEW_DOWNLOADS` targeting `DownloadsScreen` (`AppRoute.Downloads`). Integrated an `AppRouteNavigator` singleton and `AppRouteNavBridge` into `NavGraph` and `MainActivity` so tapping download notifications opens the Downloads screen directly instead of only bringing the app to the foreground.

  Files changed:
  `app/src/main/java/com/lastwave/app/data/download/TrackDownloadManager.kt`
  `app/src/main/java/com/lastwave/app/MainActivity.kt`
  `app/src/main/java/com/lastwave/app/ui/navigation/AppRouteNavigator.kt`
  `app/src/main/java/com/lastwave/app/ui/navigation/NavGraph.kt`

## 2026-09-02 — musaibbhat120605

### Fixed
- **WebM/Opus downloads not showing metadata in external players/file managers.**
  `AudioTagWriter.embedIntoWebm()` previously appended the `Tags`/`Attachments`
  elements at the very end of the file, after all audio `Cluster` data. This
  produced structurally valid EBML, but many players and file managers only
  scan the header region of a WebM/Matroska file (stopping once they reach
  audio data) instead of reading the whole file, so the tags were effectively
  invisible outside the app.

  Metadata is now spliced in right before the first `Cluster`, matching where
  real muxers place it:
  - Any existing `SeekHead` is dropped instead of left with stale offsets —
    compliant readers fall back to a normal sequential scan when it's absent.
  - Any existing `Tags`/`Attachments` elements are removed so duplicates
    aren't left behind.
  - The `Segment` size field is patched to match the new layout.
  - If a `Cues` index is present (rare for YouTube's DASH audio, but possible
    for other muxed sources), the old end-of-file append is used instead,
    since rewriting `Cues` byte offsets safely is out of scope for this fix.

  Files changed: `app/src/main/java/com/lastwave/app/data/download/AudioTagWriter.kt`

### Fixed
- **Home screen (Last.fm) lag / stutter.**
  `HomeUiState.visibleRows()` (filters, day-groups, sorts, and dedupes the
  full track history) was being recomputed inline inside a Compose
  `remember` block, which runs on the UI thread. Last.fm's now-playing and
  recent-tracks polling ticks every 12–30 seconds, so every time a track
  scrobbled in, this fairly expensive rebuild ran right on the frame meant
  to update the screen, causing a visible stutter tied directly to
  scrobbling. On top of that, the "Recent" track list was never capped, so
  it kept growing (and getting more expensive to rebuild) the longer Home
  stayed open in a session.

  - `HomeViewModel` now recomputes the row list on a background dispatcher
    (`Dispatchers.Default`) and exposes it as its own `StateFlow`, so the UI
    thread just collects a finished list instead of building it.
  - The 30-second recent-tracks poll now caps the merged track history at
    500 entries instead of growing it indefinitely.

  Files changed:
  `app/src/main/java/com/lastwave/app/ui/home/HomeViewModel.kt`,
  `app/src/main/java/com/lastwave/app/ui/home/HomeScreen.kt`

### Fixed
- **Downloaded tracks appearing twice in the Downloads list.**
  `TrackDownloadManager.downloadTrack()` had no check against tracks that
  were already downloaded — it only guarded against the *same* download
  running twice concurrently (`activeKeys`), which is cleared as soon as a
  download finishes. Re-downloading a track you already had correctly
  overwrote the file on disk, but `downloadedTrackDao.insert()` always
  created a brand-new row: `DownloadedTrackEntity.id` is an
  autoincrement primary key with no other unique constraint, so
  `OnConflictStrategy.REPLACE` never had anything to actually collide
  with.

  - Added a normalized `trackKey` column (`"${artist}_${title}"`,
    lowercased/trimmed) with a **unique index**, so the database itself
    can no longer hold two rows for the same track.
  - Migration `10 → 11` backfills `trackKey` for existing rows, deletes
    any duplicate rows already present (keeping the most recently
    downloaded copy of each), then creates the unique index.
  - `downloadTrack()` now checks the database first; if the track is
    already downloaded and its file still exists, it skips re-downloading
    entirely instead of re-fetching and duplicating. If the file was
    removed outside the app, it falls through and re-downloads, and the
    unique index makes that insert safely `REPLACE` the stale row instead
    of duplicating it.

  Files changed:
  `app/src/main/java/com/lastwave/app/data/local/db/DownloadedTrackEntity.kt`,
  `app/src/main/java/com/lastwave/app/di/DatabaseModule.kt`,
  `app/src/main/java/com/lastwave/app/data/download/TrackDownloadManager.kt`

### Fixed
- **Skipping to the next track did nothing when playing from Downloads.**
  `DownloadsViewModel.playTrack()` started playback via `MusicPlayer.play()`,
  which always builds a single-track queue (`listOf(track)`) regardless of
  how many tracks are downloaded. So the moment you played anything from the
  Downloads screen, the player's queue had exactly one item — there was
  never a "next" track to advance to, so `next()` correctly found no
  following item and silently did nothing.

  `playTrack()` now builds the full queue from every currently downloaded
  track (in the same order shown on screen), starting at the tapped
  track's position, via `MusicPlayer.playQueue()` instead of `play()`.
  Next/previous now move through the rest of your downloads normally.

  Files changed:
  `app/src/main/java/com/lastwave/app/ui/settings/DownloadsViewModel.kt`