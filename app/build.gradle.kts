plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Build outside OneDrive: OneDrive locks files while syncing, which breaks Gradle.
layout.buildDirectory.set(file("${System.getProperty("user.home")}/.storageinventory-build/app"))

android {
    namespace = "com.malek.storageinventory"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.malek.storageinventory"
        minSdk = 26
        targetSdk = 35
        versionCode = 13
        versionName = "2.3.1.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Signed with the debug key so the release APK is installable without extra setup.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // Camera + on-device QR decoding (bundled model, works offline)
    val camerax = "1.4.1"
    implementation("androidx.camera:camera-core:$camerax")
    implementation("androidx.camera:camera-camera2:$camerax")
    implementation("androidx.camera:camera-lifecycle:$camerax")
    implementation("androidx.camera:camera-view:$camerax")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    // On-device image labelling, used by "Search by photo" (bundled model, works offline)
    implementation("com.google.mlkit:image-labeling:17.0.9")

    // Account sign-in + cloud sync (Firebase), Google sign-in via Credential Manager
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    // Firebase pulls in an empty "listenablefuture" stub; CameraX needs the real ListenableFuture.
    implementation("com.google.guava:guava:33.3.1-android")
}


// After each debug build, copy the APK back into the project's "apk" folder.
val copyApk by tasks.registering(Copy::class) {
    from(layout.buildDirectory.dir("outputs/apk/debug"))
    include("*.apk")
    into(rootProject.file("apk"))
    rename { "StorageInventory.apk" }
}
afterEvaluate {
    tasks.named("assembleDebug") { finalizedBy(copyApk) }
}
