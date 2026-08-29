# Security Policy

## Reporting a Vulnerability

Multi-Space Launcher values security and privacy. If you discover a vulnerability or security issue:

1. **Do not create a public issue**.
2. Send a detailed report including steps to reproduce to the repository maintainers.
3. We will investigate promptly and provide fixes in a coordinated manner.

## Threat Model & Design

- **On-Device Storage**: Space configurations and PIN salts/hashes are stored locally in Room SQLite database.
- **PIN Cryptography**: Passwords/PINs are salted and hashed using `SHA-256` via `java.security.MessageDigest`.
