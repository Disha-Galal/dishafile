# Disha FileKit contribution guide

Work only in this repository. User documents must never be executed, injected as HTML, or overwritten implicitly. Keep all handwritten source comments in Arabic. Project credit: This project By Gogo & Disha ®.

Toolchain: JDK 17, Gradle 8.11.1 (official generated wrapper), AGP 8.9.2, Kotlin 2.2.10, compile/target SDK 35, minimum SDK 26, Node 22. Android packaging runs on GitHub Ubuntu x86_64. No PC or Android Studio needed by the user.

- Editor: `cd editor && npm ci && npm run build && npm test && npx playwright install chromium && npm run test:browser`.
- JVM codec tests without SDK: `./gradlew -PcoreOnly :core:test` using JDK 17.
- Android: `./gradlew :core:test :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest`.
- Rebuild and commit local assets whenever editor sources or package lock change. Never use a CDN at runtime.
- Bridge actions must remain allowlisted, restricted to the local HTTPS origin and main frame. Preserve CSP and blocked remote requests.
- New file handling must test Arabic, BOM, CRLF, read-only providers, malformed data, gzip limits and lossless encoding. Never silently use replacement characters.
- Keep draft-before-write, backup-before-replace, conflict detection, truncating writes, and readback verification. SAF providers cannot promise atomic replacement.
- Run emulator smoke tests on API 33; use `docs/ACCEPTANCE.md` for physical phone checks. Do not claim emulator or physical-device results unless actually run.
