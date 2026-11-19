# Security Policy

## Supported Versions

Only the latest published release on [`aayepp/ClaudeWaves`](https://github.com/aayepp/ClaudeWaves/releases) receives security fixes and updates:

| Version | Supported |
| :--- | :--- |
| `5.2.x` (Latest Release) | Yes |
| Older / Debug builds | No |

## Reporting a Vulnerability

Please **do not** disclose security vulnerabilities in public GitHub Issues or pull requests.

Use GitHub's private reporting channel instead:
- Open a private report at **https://github.com/aayepp/ClaudeWaves/security/advisories/new** (Private Vulnerability Reporting is enabled on this repository).

Please include steps to reproduce, affected version, and potential impact so the issue can be reproduced and patched safely.

## Release Integrity & Secrets Hygiene

- Official release APKs are signed with the project release key and verified in CI (`SHA-256` certificate digest prefix: `542ffe57`).
- Keystores (`*.keystore`, `*.jks`), `local.properties`, and `.env` are excluded from version control via `.gitignore`.
- Secret scanning and push protection are enabled on the repository to block accidental credential commits.
