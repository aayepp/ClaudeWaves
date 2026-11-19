---
name: hi
description: Use when the user's whole message is a greeting like "hi", "hai", "hello" or "helo" (any case, with or without punctuation). Pulls the latest main, re-reads the repo rules, and switches replies to caveman ultra mode for the rest of the session.
---

# Hi → git pull + caveman ultra

The user greets with "hi" to start a session synced and in caveman ultra mode.

1. **Pull.** Run `git pull --ff-only`. `main` has been force-rewritten before;
   a plain pull on an old clone merges the old history back.
   - Local edits block it → ask before stashing or discarding them.
   - Can't fast-forward (diverged) → stop, show `git status -sb`, and ask
     before any `git reset --hard origin/main`.
2. **Re-read the rules** from the pulled tree: `AGENTS.md`, `DESIGN.md` and
   `.agents/rules/` (AGENTS.md §9). The copy loaded at session start may be stale.
3. **Caveman ultra.** If the `caveman:caveman` skill is available, invoke it with
   args `ultra`. Otherwise apply these rules yourself for every reply until the
   user says "stop caveman" or "normal mode":
   - Drop articles, filler, pleasantries and hedging. Fragments are fine.
   - Abbreviate prose words (DB, auth, config, req, res, fn, impl); never
     abbreviate code symbols, function names, API names or error strings.
   - Use arrows for cause and effect (X → Y); one word when one word is enough.
   - Keep the user's language (Malay stays Malay).
   - Code, commits and PRs are written normally. Security warnings and
     irreversible-action confirmations are written in full sentences.
4. Reply with one short ready line in the user's language that says what the
   pull did (e.g. "Pull ok, 3 commit baru. Nak buat apa?" or "Dah terkini. Nak buat apa?"),
   without naming the mode.
