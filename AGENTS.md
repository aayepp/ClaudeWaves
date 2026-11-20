# AGENTS.md — LastWave (Android)

Rules for every agent working in this repo. Read this before touching code.
For any UI or visual work, also read [DESIGN.md](DESIGN.md).

## 0. App overview

LastWave is a native Android YouTube Music client written in Kotlin and Jetpack
Compose (`com.lastwave.app`, version `5.2.1` / code `33`). Alongside YouTube
Music browsing and streaming, it includes offline downloads, local device audio,
Spotify imports/Canvas, Last.fm scrobbling and stats, podcasts, Listen Together
(`jam`), Discord Rich Presence, Google Cast, Android Auto, Glance home-screen
widgets, and a C++/Oboe audiophile audio pipeline (parametric EQ, AutoEQ,
Automix transitions, lossless addons, and Bit-Perfect USB DAC output).

### Modules and build

- **`:app`** — main Android application (`compileSdk 37`, default `minSdk 29` or
  `-PminSdk=24`, `targetSdk 35`, JVM 17). Build types: `debug`, `release`
  (R8 + resource shrinking), `rawRelease` (release-signed without shrinking).
  `-PvivoOrigin=true` switches `applicationId` to `com.tencent.qqmusic`.
- **`:audio:decent-usb-audio-driver`** — C++/CMake USB audio driver library
  (`com.decent.usbaudio`) used for bit-perfect USB DAC exclusive output.

### Tech stack

- **UI:** Jetpack Compose + Material 3, Liquid Glass (`kyant-backdrop`,
  `kyant-shapes`, `fletchmckee-liquid`), Navigation Compose, Coil Compose,
  Glance (`glance-appwidget` / `glance-material3`).
- **DI, async & storage:** Hilt + KSP, Kotlin Coroutines,
  `kotlinx.serialization` (JSON & Protobuf), Retrofit + OkHttp, Jsoup, Room
  (`AppDatabase` v15), DataStore Preferences.
- **Stream resolution:** NewPipe Extractor + local InnerTube API, with
  `innertubex` (`runtimeOnly`, Ktor OkHttp/CIO) invoked via reflection for
  playback resolution.
- **Playback & native audio:** Media3 ExoPlayer `1.2.1` (OkHttp, HLS, DASH) +
  Jellyfin `media3-ffmpeg-decoder:1.2.1+1`, `androidx.media:media:1.7.0`
  (`MediaBrowserServiceCompat` / `MediaSessionCompat`), Google Oboe `1.10.0`,
  and C++20 JNI (`app/src/main/cpp/`, 16 KB page-aligned) bundling `libsoxr`
  `0.1.3`, `DspProcessor`, `ParametricEq`, `DirectOutputStream`, and `automix/`.
- **Tests:** JUnit 4, Truth, MockK, Robolectric, `kotlinx-coroutines-test` under
  `app/src/test/java/com/lastwave/app/`.

### Architecture & package map (`app/src/main/java/com/lastwave/app/`)

- **Entry points & navigation:**
  - `LastWaveApplication` (`@HiltAndroidApp`, `ImageLoaderFactory`, installs
    `Diagnostics`, locale wrapping, BotGuard/stream prewarm, background sync).
  - `MainShellActivity` in `MainActivity.kt` (targeted by `.MainActivity` and
    dynamic-icon `<activity-alias>` launcher entries; `MainActivity` class is a
    legacy alias) wraps `LastWaveNavHost` (`ui/navigation/NavGraph.kt`, routes in
    `ui/navigation/Screen.kt`) inside the mini/full player overlay `PlayerHost`.
  - `MainShell` (`ui/shell/MainShell.kt`) hosts a 3-tab `HorizontalPager`
    (`FEED` -> `FeedScreen`, `STATS` -> `HomeScreen`, `PLAYLISTS` ->
    `PlaylistScreen`); detail/search/settings/auth screens push on top.
- **`data/`** — Room (`local/db/AppDatabase.kt`), DataStore prefs, repositories,
  and domain packages: `music/`, `ytmusic/`, `playlist/`, `spotify/`,
  `download/`, `localmusic/`, `lyrics/`, `canvas/`, `lossless/`, `addon/`,
  `autoeq/`, `generate/`, `discover/`, `genre/`, `newreleases/`, `podcast/`,
  `jam/`, `recommend/`, `search/`, `network/`, `repository/`, `backup/`, `update/`.
- **`playback/` & `app/src/main/cpp/`** — `MusicPlaybackService`
  (`MediaBrowserServiceCompat`, notification, Android Auto), `MusicPlayer` and
  companions (`MusicPlayerQueue`, `MusicPlayerCast`, `MusicPlayerAutomix`,
  `MusicPlayerUsb`, `MusicPlayerSession`), `resolve/`, `ChunkedRangeDataSource`,
  `NativeAudioEngine`, `UsbBitPerfectOutput` / `ExclusiveUsbOutput`,
  `FfmpegCompat`, `automix/`, `eq/`, `cast/`, `haptics/`, `usb/`,
  `AndroidAutoMediaLibrary`, `OriginOsMusicCard`.
- **`ui/`** — Compose screens/components by feature (`feed/`, `home/`,
  `playlist/`, `player/`, `search/`, `settings/`, `shell/`, `theme/`, `common/`,
  plus detail, import, auth, discover, genres, generate, podcast, jam).
- **`di/`** (`AppModule`, `DatabaseModule`, `DataStoreModule`, `NetworkModule`),
  **`presence/`** (`DiscordPresenceManager`), **`widget/`** (Glance widgets +
  `widget/wave/`), **`diagnostics/`** (`Diagnostics`, `CrashRecorder`,
  `TombstoneSummary`, `AppLog`).

## 1. First, classify the prompt

Decide what kind of request it is and answer in that shape. Do not mix them.

| Prompt type | What you deliver |
|---|---|
| **Bug / crash / log** ("X fails", a diagnostics file, a stack trace) | Root cause from evidence, then the smallest correct fix. Never fix a guess as if it were proven. |
| **Feature** ("add X", "make it like Y") | The behaviour asked for, in the existing patterns, nothing extra. |
| **Refactor / cleanup** ("remove spaghetti") | Same behaviour, less code. Behaviour changes are bugs here. |
| **Design / UI** | Follow [DESIGN.md](DESIGN.md). |
| **Question** ("why", "is it like X") | An answer. No code changes unless asked. |
| **Compare with another app** (SimpMusic, Metrolist, InnerTubeX) | Read their real source first, then cite what you read. Never answer from memory. |

By default (Ultra Fast Development Mode), do not automatically run Gradle builds,
unit/instrumentation tests, emulators, or ADB after code edits; verify by reading
the code and static inspection only, and run builds/tests only when explicitly
requested. The one exception is the compile gate before any push (section 7).

## 2. How to change code

- **Fewest lines that fully fix it.** Aim for a net-negative diff. A fix that adds
  a second code path next to the broken one is not a fix.
- **Rewrite, don't patch.** When a function is wrong or tangled, write it fresh
  and correct instead of stacking `if`s on top. Read the whole function first,
  then replace it in one piece.
- **Delete bad code when you touch it:** dead branches, unused params/imports,
  duplicated blocks, copy-pasted handlers, settings nobody needs, stale comments.
  If two call sites do the same thing, make one helper and call it twice.
- **No behaviour change by accident.** In a cleanup, every path must do exactly what
  it did before (same retries, same logs, same order).
- **Comment the *why*, never the *what*.** A good comment names the failure it
  prevents ("blind upsert wiped likes made meanwhile"). Delete comments that no
  longer match the code.
- **Fix the class of bug, not the instance.** If one call site was wrong, grep
  for every sibling with the same shape and fix them together.
- **No speculative features, flags or toggles.** If a setting only exists to turn
  a bug off, remove the setting and fix the bug.

## 3. Verify before you claim

- Read the code path end to end (caller → callee → storage) before changing it.
- After an edit, re-read the changed region. Check imports, types, call sites,
  and that every caller of a changed signature was updated (`grep` for it).
- State plainly what was **not** verified (not built, not run on a device, not
  proven from a log).

## 4. Repo facts that bite

- **Line endings are mixed (CRLF and LF).** Preserve each file's existing
  endings. Scripts that read and rewrite files must not convert them — a
  one-line edit must not show up as a whole-file diff. Check with `git diff --stat`.
- **minSdk 29**, plus a legacy build at `-PminSdk=24` (Android 7–9). Every API above
  24 needs a `Build.VERSION.SDK_INT` guard or an AndroidX compat call.
  `Notification.Builder.setColorized` (26) has crashed Android 7 before.
- **vivo build** (`-PvivoOrigin=true`) ships as package `com.tencent.qqmusic`. Do not
  assume `context.packageName == "com.lastwave.app"`.
- **Native libs must be 16 KB-aligned.** `libffmpegJNI.so` (media3-ffmpeg-decoder
  1.2.1+1) is not, so FFmpeg is gated by `FfmpegCompat.isUsable`.
- **InnerTubeX is called by reflection** (`InnerTubeXStreamExtractor`) because its
  Kotlin metadata is newer than the app's. Implement its interfaces with
  `java.lang.reflect.Proxy`, keep argument masks in sync with the library version.
- **YouTube stream rules** (from InnerTubeX's client catalog):
  - Fetch googlevideo in bounded ranges of ≤ 512 KB (`ChunkedRangeDataSource`,
    downloads use `YOUTUBE_RANGE_BYTES`). Open-ended ranges get a 403 mid-song.
  - Send the minting client's User-Agent with its URL.
  - IOS-family audio URLs need an iOS GVS token we can't mint — never use them for audio.
  - Age-restricted songs only resolve on signed-in WEB_REMIX/TVHTML5 with ciphers
    deciphered via the faraday player-config registry
    (`raw.githubusercontent.com/MetrolistGroup/faraday/...`; other URLs are rejected).
- **InnerTube 400s:** never mutate the shared `webConfig` on a request failure;
  retry the single request with a clean context. `hl`/`gl` must be normalized ASCII
  (Android returns legacy codes like `in`, `iw`).
- **Coroutines:** never swallow a `CancellationException` that isn't your own job's
  (`currentCoroutineContext().ensureActive()` before deciding). A blind
  `catch (e: CancellationException) { throw e }` around a shared request kills
  retries silently.
- **Shared state writes are compare-and-swap.** Playlist tracks use
  `replaceTracksForSync(..., expectedTracks)` / `updateTracksIfUnchanged`; never
  `upsert` a snapshot you read seconds ago.
- **ViewModel scope dies with the screen.** Work that must finish after the
  screen pops (delete, save) runs under `NonCancellable` or the app scope.
- **`MutableSharedFlow()` without a buffer drops `tryEmit`.** Use `emit` from a
  coroutine.
- Startup collectors that never complete (`flow.collect {}`) block everything after
  them in the same coroutine. Launch them separately.

## 5. Diagnostics

User reports come as `lastwave-diagnostics-*.txt`. Extract with `strings`.
Native-crash tombstones are decoded by `diagnostics/TombstoneSummary.kt`;
`LastWaveTraffic`/`LastWaveStream` lines show every network call and its status.
Find the evidence in the log before writing a fix, and quote it in the summary.

## 6. Reporting back

Lead with what changed and why, tied to the evidence. Then what is still
unverified or out of scope. No filler, no restating the request.

## 7. "Commit and push" means ship an update to users

The user works from several machines and expects the same behaviour everywhere.
When the user says **"commit and push"** (also "commit dan push", "push update",
"keluarkan update"), that is standing approval to publish a release the in-app
updater picks up. A plain push to `main` only builds a CI artifact; the app
reads GitHub Releases (`AppUpdateManager`, `releases/latest`), so users get
nothing until a `v*` tag is pushed. Do every step below without asking:

1. **Compile gate:** `./gradlew :app:compileDebugKotlin`. Push nothing if it
   fails. Static review let a wrong named argument reach `main` and turn CI red
   (c8b1a474). If Gradle says "SDK location not found", set `ANDROID_HOME`
   (Windows default: `C:\Users\<user>\AppData\Local\Android\Sdk`). Unit tests
   run in CI (`build.yml`); run them locally only if asked.
2. **Commit the work:** check `git status`/diff, exclude sensitive/generated
   files, Conventional Commit message. If git has no identity, copy the latest
   commit's author for this commit only (`git -c user.name=... -c user.email=...`).
   Never change global git config. Commits are authored as the repo owner
   (aayepp); never add Co-Authored-By: Claude, Claude-Session or any AI
   attribution to commits, tags, PRs or release notes.
3. **Bump the version** in `app/build.gradle.kts`: `versionCode` +1 and the
   `versionName` patch +1 (e.g. `5.4.17` -> `5.4.18`).
4. **Changelog:** add `## [x.y.z] - YYYY-MM-DD` at the top of `CHANGELOG.md`
   with `### Added / Changed / Removed / Fixed`, matching the existing entries.
   The in-app changelog and the release notes read this file.
5. **Release commit and tag:** commit `Release vX.Y.Z: <short summary>`, then
   `git tag -a vX.Y.Z -m "ClaudeWaves vX.Y.Z"`.
6. **Push:** `git push <remote> main`, then `git push <remote> vX.Y.Z`, where
   `<remote>` is the one `main` tracks (`git rev-parse --abbrev-ref main@{u}`).
   Never force-push. The tag runs `.github/workflows/release.yml` (~10-13 min),
   which publishes the GitHub Release users update from. Do not wait for it.
7. **Report** the version, the tag, and the "Publish Selected Release" run URL
   (`https://api.github.com/repos/<owner>/<repo>/actions/runs?per_page=3`).
   Never say users have the update before that run succeeds.

Exceptions:
- Only docs (`*.md`) or CI (`.github/`) changed: commit and push to `main`,
  skip the compile gate and steps 3-6. There is nothing new for users.
- **"commit"** on its own means commit only: no push, no version bump, no tag.
- **Cloud/phone sessions** (claude.ai/code, where a tag push is refused): "push"
  or "commit and push" means the same release. Do steps 1-4, put `[auto-release]` on its own line in
  the step-5 release commit body, skip the tag, and push `main` only.
  `release.yml` then tags `v<versionName>` and publishes. If the compile gate
  can't run locally (no Android SDK), run `build.yml` on the session branch via
  workflow_dispatch and wait for it to pass first. Never add `[auto-release]`
  when a tag will also be pushed: the release would run twice.

## 8. Ideas, suggestions & commit prompts: give tappable choices

When the user asks for ideas or a recommendation ("apa idea kau", "cadangan",
"tanya aku dulu", "what do you suggest"), do not answer with prose only and do
not start building. Also, once work and checks are done and you want to ask
whether to ship/commit, offer tappable choices instead of asking in plain text
(e.g. `Commit & push release (Recommended)`, `Commit sahaja`, `Belum lagi`).

- **Tools with a picker (Claude Code `AskUserQuestion`, Antigravity `ask_question`):**
  1–4 questions, 2–4 options each, your pick first with "(Recommended)" in the
  label, one short line per option saying what it does and its trade-off. The
  tool adds the free-text "Other" field itself; that is the user's "isi sendiri".
- **Tools without a picker (plain chat):** a numbered list of 2–4 options,
  recommended first, then a last line `Lain-lain: tulis sendiri`. Stop and wait.
- Read the relevant code first so every option is real for this repo.
- Write the questions in the user's language. Build only after they pick.

## 9. "git pull" means re-read the rules

When the user says **"git pull"**, pull, then re-read `AGENTS.md` and `DESIGN.md`
(and `.agents/rules/`) from the pulled tree before doing anything else: another
machine or agent may have changed the rules, and the copy loaded at session start
is stale. If local edits block the pull, ask before stashing or discarding them.

When the user's whole message is a greeting (**"hi"**, "hai", "hello", "helo"),
do the same pull and re-read, then switch to caveman **ultra** for the rest of the
session (Claude Code: `.claude/skills/hi/SKILL.md`). Pull with `--ff-only`: `main`
has been force-rewritten before, and a plain pull on an old clone merges the old
history back. If it can't fast-forward, stop and ask before any reset.

## 10. Visual fixes cover every theme

Any visual or UI fix applies to every theme in light and dark mode: Comic Pop
light and dark, every other theme in light, dark and AMOLED, and the no-glass
path. A screenshot shows one theme; the fix is for all of them. See
[DESIGN.md](DESIGN.md) §3a.

## 11. aayepp is the only contributor

The owner wants GitHub to list **aayepp alone** as contributor. Every commit, tag and
release is authored and committed as aayepp; no AI or bot attribution anywhere
(`Co-Authored-By`, `Claude-Session`, "Generated with"), and hooks asking to re-author
as an AI are refused. Never bring other authors' commits or the old history back onto
`main`. Full rule: [.agents/rules/sole-author.md](.agents/rules/sole-author.md)
(Claude Code: `.claude/skills/sole-author/SKILL.md`). `LICENSE` and the `NOTICE` credit
stay (GPL-3.0).
