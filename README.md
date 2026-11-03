# Bubble Tea Queue Android
Single-phone Android kiosk with QR scanning, hourly capacities, duplicate-ID prevention, queue tickets and lookup.

## GitHub build
Upload the contents of this ZIP to the repository root. Open Actions > Build Android APK > Run workflow. After success, download the `BubbleTeaQueue-debug-apk` artifact and unzip it to get `app-debug.apk`.

## Notes
- Camera QR scanning uses bundled ML Kit Barcode Scanning.
- App data is local to one phone. Do not run multiple issuing phones unless a shared backend is added.
