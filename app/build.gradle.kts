import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

// Load keystore properties
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.ciclismo.portugal"
    compileSdk = 35

    // Signing configuration for release builds
    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = file("../${keystoreProperties["storeFile"]}")
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    defaultConfig {
        applicationId = "com.ciclismo.portugal"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "1.2.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Production AdMob IDs
            buildConfigField("String", "ADMOB_BANNER_ID", "\"ca-app-pub-4498446920337333/8835929871\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"ca-app-pub-4498446920337333/7055944347\"")
            buildConfigField("String", "ADMOB_REWARDED_ID", "\"ca-app-pub-4498446920337333/5996151723\"")
            // YouTube Data API
            buildConfigField("String", "YOUTUBE_API_KEY", "\"***REMOVED_EXPOSED_KEY***\"")
            // Gemini AI API Key (from keystore.properties)
            buildConfigField("String", "GEMINI_API_KEY", "\"${keystoreProperties.getProperty("geminiApiKey", "")}\"")
            // Daily AI request limit (free tier = 1000)
            buildConfigField("int", "AI_DAILY_LIMIT", "800")
        }
        debug {
            isDebuggable = true
            // Test AdMob IDs (Google's official test IDs)
            buildConfigField("String", "ADMOB_BANNER_ID", "\"ca-app-pub-3940256099942544/6300978111\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "ADMOB_REWARDED_ID", "\"ca-app-pub-3940256099942544/5224354917\"")
            // YouTube Data API
            buildConfigField("String", "YOUTUBE_API_KEY", "\"***REMOVED_EXPOSED_KEY***\"")
            // Gemini AI API Key (from keystore.properties)
            buildConfigField("String", "GEMINI_API_KEY", "\"${keystoreProperties.getProperty("geminiApiKey", "")}\"")
            // Daily AI request limit (higher for debug testing)
            buildConfigField("int", "AI_DAILY_LIMIT", "500")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.10.0")

    // ExifInterface for reading image metadata
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // Jetpack Compose
    val composeBom = platform("androidx.compose:compose-bom:2025.01.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Room Database
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Retrofit & OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Jsoup for HTML parsing
    implementation("org.jsoup:jsoup:1.18.3")

    // Hilt Dependency Injection
    implementation("com.google.dagger:hilt-android:2.54")
    ksp("com.google.dagger:hilt-compiler:2.54")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")

    // DataStore (for preferences)
    implementation("androidx.datastore:datastore-preferences:1.1.2")

    // Gson for JSON parsing
    implementation("com.google.code.gson:gson:2.11.0")

    // Google Mobile Ads (AdMob)
    implementation("com.google.android.gms:play-services-ads:23.6.0")

    // UMP SDK for GDPR consent
    implementation("com.google.android.ump:user-messaging-platform:3.1.0")

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Splash Screen
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.0.0")

    // Charts for statistics (Fantasy)
    implementation("com.patrykandpatrick.vico:compose-m3:1.13.1")

    // YouTube Player for embedded videos
    implementation("com.pierfrancescosoffritti.androidyoutubeplayer:core:12.1.1")

    // Chrome Custom Tabs for OAuth flows
    implementation("androidx.browser:browser:1.8.0")

    // Google Generative AI (Gemini API)
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    // Google Play Billing (for Premium subscriptions)
    implementation("com.android.billingclient:billing-ktx:7.0.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
