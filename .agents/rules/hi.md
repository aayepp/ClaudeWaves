---
trigger: always_on
---

# Hi → git pull + caveman ultra

When the user's whole message is a greeting ("hi", "hai", "hello", "helo"):

1. Run `git pull --ff-only` (never a plain pull: `main` has been force-rewritten
   before). Local edits block it or it can't fast-forward → stop and ask before
   stashing, discarding or resetting.
2. Re-read `AGENTS.md`, `DESIGN.md` and `.agents/rules/` from the pulled tree.
3. Reply in caveman ultra for the rest of the session: no articles, filler or
   pleasantries; fragments; abbreviate prose words (never code symbols); arrows for
   cause → effect; keep the user's language. Code, commits, security warnings and
   irreversible-action confirmations are written normally. Ends on "stop caveman"
   or "normal mode".
4. Answer with one short ready line saying what the pull did.
