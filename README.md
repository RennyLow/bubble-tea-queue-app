# Bubble Tea Queue Android app

A single-phone event kiosk app for issuing hourly bubble-tea queue tickets.

## Features
- Select hourly slot and show remaining capacity / NOT AVAILABLE
- Scan Student QR with rear camera
- Bundled Google ML Kit QR model, so recognition is available immediately after installation
- Manual Student ID fallback
- Prevent duplicate ticket for the same Student ID
- Queue numbering by hour, e.g. 09-001, 09-002, 10-001
- Retrieve an existing ticket
- Edit capacities on-device
- Reset event data
- Data stored locally on the Android phone with SharedPreferences

## Build APK
Open the project in Android Studio, allow Gradle sync, then use **Build > Build APK(s)**. The output is normally `app/build/outputs/apk/debug/app-debug.apk`.

The project uses minSdk 23 and the bundled ML Kit barcode dependency `com.google.mlkit:barcode-scanning:17.3.0`.

## Important
This version is for ONE phone. If more than one Android phone will issue tickets, use a central database/API so two devices cannot allocate the final place independently.
