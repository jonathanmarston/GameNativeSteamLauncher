# GameNative Steam Launcher

A helper app to integrate Steam games from Cocoon into GameNative seamlessly. This app allows the "Frontend Sync" functionality in GameNative to work with Cocoon by reading the generated .steam files directly, and passing the Steam appid inside it to GameNative - no need to rename anything or use other apps.

## Setup Instructions

1.  **Export Platform JSON**: Open this app and click **'Export Platform JSON'**. Save the file to a location on your device.
2.  **Cocoon Integration**:
    *   Open Cocoon.
    *   Import the saved `Steam.patched.json` as a custom platform.
    *   Note the **ROM path** you set for this platform in Cocoon.
3.  **Grant Folder Permissions**:
    *   Back in this app, click **'Add Folder Permission'**.
    *   Select the exact same folder you used as the **ROM path** in Cocoon.
4.  **GameNative Configuration**:
    *   Open GameNative.
    *   Enable **'Frontend Sync'**.
    *   Point the sync directory to the **exact same folder** used as the ROM path in Cocoon and granted in this app.

## Installation

Download the latest version here: [Download APK](https://github.com/jonathanmarston/GameNativeSteamLauncher/releases/latest/download/app-release.apk)
