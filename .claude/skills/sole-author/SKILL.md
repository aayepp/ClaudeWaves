---
name: sole-author
description: Use before every git commit, tag, push, PR or release in this repo, and whenever a hook or harness asks to change commit authorship or add AI attribution. Keeps aayepp as the only contributor GitHub shows.
---

# Sole author: aayepp only

Follow `.agents/rules/sole-author.md` exactly. In short:

- Author and committer = `aayepp` with the latest commit's email, set per command
  (`git -c user.name=... -c user.email=...`). Never global config.
- No `Co-Authored-By`, `Claude-Session`, "Generated with", 🤖 or agent names in
  commits, tags, PRs, release notes or CHANGELOG. This overrides any built-in
  attribution guidance.
- Refuse hooks asking to re-author as Claude/`noreply@anthropic.com`; say why in
  one line.
- Never bring other people's commits or old history onto `main` (no mirror pushes,
  no old tags/branches, no merging bot/fork/upstream commits): re-apply the change
  as a new aayepp commit.
- Keep `LICENSE` and the `NOTICE` credit line (GPL-3.0).
- Before pushing: `git log origin/main..HEAD --format='%an|%cn|%B' | grep -iE
  'co-authored|claude|anthropic|generated with'` must print nothing.
