# Publishing a new version

1. Bump `versionCode` (+1) and `versionName` in `app/build.gradle.kts`.
2. Build in Android Studio: Build > Generate App Bundles or APKs > Generate APKs.
   The APK is copied to `apk/StorageInventory.apk`.
3. Copy it to `release/StorageInventory.apk` and update `release/update.json`
   (versionCode, versionName, apkUrl with the new tag, notes).
4. Commit and push. The "Publish release" workflow creates the GitHub release;
   phones pick it up from Settings > Check for updates.

Always build on the same PC: Android only accepts an update signed with the same key.
