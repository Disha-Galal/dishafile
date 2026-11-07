# Acceptance and remaining device checks

Automated test results and actual workflow URLs are recorded in `docs/BUILD_STATUS.md`. This checklist is for an Android 13 physical device, especially OPPO CPH2203. An emulator result does not prove a vendor provider's behavior.

1. In airplane mode, Open file → Downloads → sample.json. Select/copy a line, edit a key, Save As example-edited.json. Reopen and Validate.
2. From Downloads choose Open with Disha FileKit when offered. From Telegram share a compatible content URI. `application/octet-stream` is deliberately not claimed: use Open file if not offered.
3. Open malformed.json. Format must refuse, preserve content and show line/column.
4. Repeat editing, selection, Find/Replace, undo/redo and Save As with .md, .sql, .html, .txt. HTML must remain inert text.
5. Open config.json.gz and backup.sql.gz, edit, Save As .gz. Verify independently with `gzip -t` and `gzip -dc`. Save to original must explicitly confirm recompression.
6. Reject fake/corrupt/concatenated gzip and >32 MiB decompressed payloads. Reject tar.gz/tgz and binary data. Original stays unchanged on open failure.
7. Preserve Arabic, UTF-8 BOM and CRLF in arabic-crlf.txt; test UTF-16 BOM and Windows-1256. Unrepresentable added characters must refuse save and offer explicit UTF-8 Save As.
8. A provider with no write flag or grant must offer Save As. Revoke a recent URI grant and verify Open file guidance. Simulate disk-full/provider write failure; recover both draft and original backup.
9. Change a file externally after opening. Save must refuse overwriting and request Save As.
10. >8 MiB text: read-only 256 KiB preview, disabled export/save. Mixed line endings are read-only to avoid silent normalization.
11. Background/rotate/relaunch the app, Recover draft; verify content and Save As. Recovery intentionally has no original URI and cannot silently overwrite it.
12. Install the APK from Actions on Android 8+ and Android 13. Confirm no network/storage permissions and usable keyboard, insets, long-press selection and touch targets.

Known v1 limits: one document at a time; no archives/binary view, execution, preview rendering or folders. Drafts debounce 700 ms; immediate process death may lose the latest keystrokes. Recovery opens from Home after process death/rotation. Backups/drafts are app-private and unencrypted at application level, not backed up to cloud; uninstall deletes them. Recovery entries are retained (no automatic pruning). Share exports live in app cache and Android can clear them. Providers do not guarantee atomic writes. Mixed newline files are read-only. Encoding selection is explicit for Windows-1256; no probabilistic autodetection. Large gzip is fully verified (bounded 32 MiB) before preview, so decompression is not instantaneous. Android 8 requires an updated compatible WebView. New debug signing keys on different GitHub runners may require uninstalling an earlier debug build; export drafts before uninstalling.
