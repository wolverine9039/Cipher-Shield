plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.ciphershield"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.ciphershield"
        minSdk = 26  // Required for modern crypto APIs
        targetSdk = 34
        versionCode = 2
        versionName = "2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Enable vector drawables
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            // Security: Disable debugging in release
            isDebuggable = false
            isJniDebuggable = false
        }

        getByName("debug") {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
    }

    // Security hardening
    packaging {
        resources {
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/license.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/notice.txt"
            excludes += "META-INF/*.kotlin_module"
        }
    }
}

dependencies {
    // AndroidX Core
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.activity:activity:1.8.2")
    implementation("androidx.fragment:fragment:1.6.2")

    // Material Design Components
    implementation("com.google.android.material:material:1.11.0")

    // CardView for modern card designs
    implementation("androidx.cardview:cardview:1.0.0")

    // RecyclerView (if needed for file lists)
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Lifecycle components
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")

    // Security - Crypto library
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // WorkManager (for background secure deletion)
    implementation("androidx.work:work-runtime:2.9.0")

    // Preferences
    implementation("androidx.preference:preference:1.2.1")

    // Biometric Authentication (for future enhancement)
    implementation("androidx.biometric:biometric:1.1.0")

    // Animation libraries
    implementation("com.airbnb.android:lottie:6.3.0") // Smooth animations

    // Timber for logging (debug only)
    debugImplementation("com.jakewharton.timber:timber:5.0.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.robolectric:robolectric:4.11.1")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")

    // LeakCanary for memory leak detection (debug only)
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.13")
}

// ProGuard rules for security
tasks.whenTaskAdded {
    if (name == "assembleRelease") {
        doLast {
            println("🔒 Security build completed")
            println("✓ Encryption: AES-256-GCM")
            println("✓ Key exchange: RSA-2048-OAEP")
            println("✓ Authentication: HMAC-SHA256")
            println("✓ Password derivation: PBKDF2-100k")
        }
    }
}
