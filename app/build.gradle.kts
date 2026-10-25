
plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.angonogiants.application"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.angonogiants.application"

        minSdk = 25
        targetSdk = 34

        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    // ----------------------------------
    // Android UI
    // ----------------------------------

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // ----------------------------------
    // Glide - Image and GIF Loading
    // ----------------------------------

    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)

    // ----------------------------------
    // Firebase Bill of Materials
    // ----------------------------------

    implementation(
        platform(
            "com.google.firebase:firebase-bom:34.19.0"
        )
    )

    // ----------------------------------
    // Firebase Authentication
    // ----------------------------------

    implementation(
        "com.google.firebase:firebase-auth"
    )

    // ----------------------------------
    // Firebase Realtime Database
    // ----------------------------------

    implementation(
        "com.google.firebase:firebase-database"
    )

    // ----------------------------------
    // Unit Testing
    // ----------------------------------

    testImplementation(libs.junit)

    // ----------------------------------
    // Android Instrumented Testing
    // ----------------------------------

    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
