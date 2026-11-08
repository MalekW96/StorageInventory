# Storage Inventory (Android)

Scan QR codes to register storage units, then scan more codes to add items or
smaller containers inside them. Containers can be nested as deep as you like.
All data stays on the phone (no account, no internet needed).

## How it works
1. Home shows **No Storage Data Available** until you add something. Tap **+** (top right).
2. Scan the **Storage QR code** -> name it -> **Done** (or **Cancel**).
3. You're asked to start adding items. Tap **Scan QR code**, scan the next code,
   pick **Item** or **Smaller container** from the list, give it a name, tap **Done**.
4. Keep scanning, or tap **Done**. Open a container to add things inside it.
5. The magnifier icon on Home finds where any scanned code is stored.
   Each QR code can only be registered once; rename/delete is in each row's menu.

## Build the APK

### Option A: Android Studio (easiest on Windows)
1. Install Android Studio, then **File > Open** this folder.
2. Wait for Gradle sync to finish (first time downloads the SDK pieces).
3. **Build > Build App Bundle(s) / APK(s) > Build APK(s)**.
4. APK: `app/build/outputs/apk/debug/app-debug.apk`

### Option B: GitHub (no install)
Push this folder to a GitHub repository. The included workflow
(`.github/workflows/build-apk.yml`) builds the APK automatically; download it from
the run's **Artifacts** section in the **Actions** tab.

### Option C: command line
With JDK 17+ and the Android SDK (`ANDROID_HOME` set): `gradlew assembleDebug`

## Installing on your phone
Copy the APK to the phone and open it. Android will ask you to allow installs from
that source (Files app / browser) the first time.

## Tech
Kotlin, Jetpack Compose (Material 3), CameraX, ML Kit barcode scanning (bundled,
offline). Min Android 8.0 (API 26), target API 35.

## iPhone / web version

The `docs/` folder is a web version of the app that runs in Safari on iPhone (and any browser):
https://malekw96.github.io/StorageInventory/

Open it in Safari, tap Share, then "Add to Home Screen". It works offline, keeps data on the phone,
and updates itself when a new version is pushed (Settings > Check for updates).
