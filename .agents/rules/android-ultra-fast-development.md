---
trigger: always_on
---

# Android Ultra Fast Development Mode

Default workflow: **EDIT -> REVIEW -> DONE** (coding-first, build & test on demand).

## Strict No-Auto-Build & No-Auto-Device Policy

Do NOT automatically run Gradle builds (`assembleDebug`, `build`, `clean`, `check`, `lint`), unit tests (`test`, `testDebugUnitTest`), instrumentation tests (`connectedAndroidTest`), APK generation, Android emulators, or ADB / device operations (`install`, `launch`, `logcat`, `clear data`) after code edits, bug fixes, UI changes, refactors, or dependency changes.

Only run build, test, or device commands when the user explicitly requests them (e.g., "Build the app", "Run unit tests", "Test this feature", "Install to device"). Exception: "commit and push" ships a release (`AGENTS.md` section 7) and starts with the compile gate `./gradlew :app:compileDebugKotlin`; push nothing if it fails.

## Fast Editing & Lightweight Verification

- Inspect only necessary files; reuse discovered structure and avoid redundant reads/searches or unrelated refactoring.
- Make minimal, targeted changes preserving existing architecture and file line endings.
- After editing, verify via **lightweight static inspection only**: review modified code and diffs, check imports, types, function signatures, call sites, and logical consistency.
- Never claim the app was built or tested unless those commands actually ran; state clearly when build, unit tests, and device testing were not run.

## Completion Report

Keep post-edit reports concise with:
- **Completed:** summary of changes and modified files.
- **Verification:** static inspection status, plus `Build: not run`, `Unit tests: not run`, `Device testing: not run` (unless explicitly executed).
- **Notes:** significant risks or unresolved items only.
