import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
    kotlin("kapt")
}

android {
    namespace = "com.shanalimughal.mentalhealthai"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.shanalimughal.mentalhealthai"
        minSdk = 24
        targetSdk = 34
        versionCode = 3
        versionName = "1.0.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Reading API key from local.properties
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { stream ->
                localProperties.load(stream)
            }
        }
        val apiKey = localProperties.getProperty("API_KEY") ?: "\"\""
        buildConfigField("String", "API_KEY", "\"$apiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.storage)
    implementation(libs.androidx.lifecycle.viewmodel.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.material.v170)

    // navigation dependencies
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
//    implementation("com.google.android.libraries.maps:secrets-gradle-plugin:1.3.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // circular image view sdk
    implementation("de.hdodenhof:circleimageview:3.1.0")

    // sdp sdk for responsive layout
    implementation("com.intuit.sdp:sdp-android:1.1.1")

//    lottie animation sdk
    implementation("com.airbnb.android:lottie:3.4.0")

    // facebook sdk for sign in
//    implementation (libs.facebook.android.sdk)

    // bottom navigation sdk
    implementation("com.github.ibrahimsn98:SmoothBottomBar:1.7.9")

    // The view calendar library
    implementation("com.kizitonwose.calendar:view:2.5.1")
    // The compose calendar library
    implementation("com.kizitonwose.calendar:compose:2.5.1")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    kapt("androidx.room:room-compiler:$room_version")
    implementation("androidx.room:room-ktx:$room_version")

    // for side navigation
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")
    implementation("com.google.android.material:material:1.3.0")

    // picasso for online images
    implementation("com.squareup.picasso:picasso:2.8")
}
