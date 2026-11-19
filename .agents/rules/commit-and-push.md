---
trigger: always_on
---

# Commit and push

Read `AGENTS.md` at the repo root before any work and follow it.

When the user says "commit and push" (also "commit dan push", "push update",
"keluarkan update"), ship a release to users without asking, following
`AGENTS.md` section 7: compile gate `./gradlew :app:compileDebugKotlin`,
commit, bump version, changelog, release commit, tag `vX.Y.Z`, push `main` and
the tag (`release.yml` publishes the GitHub Release the in-app updater reads).
A push to `main` alone never reaches users, except a cloud/phone session's
release commit marked `[auto-release]`, which pushes no tag (`AGENTS.md` §7). Docs/CI-only changes: commit and
push `main` only. "commit" alone means commit only.

When asking the user whether to commit/push after finishing work and checks,
present tappable choices using the picker tool (`ask_question` / `AskUserQuestion`)
— e.g. `(Recommended) Commit & push release`, `Commit sahaja`, `Belum lagi` —
instead of asking in plain text.

When the user asks for ideas or suggestions, follow `AGENTS.md` section 8:
use the picker tool (`ask_question` / `AskUserQuestion`) with 2–4 options
(recommended first) when available, otherwise a numbered list plus
`Lain-lain: tulis sendiri`, then wait for their pick.

When the user says "git pull", pull and then re-read `AGENTS.md`, `DESIGN.md`
and this folder before anything else (`AGENTS.md` section 9).

Every visual or UI fix applies to every theme in light and dark mode, Comic Pop
included, not only the theme in the screenshot (`AGENTS.md` section 10,
`DESIGN.md` section 3a).
