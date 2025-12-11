plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-parcelize")
    alias(libs.plugins.google.services)
    id("kotlin-kapt")
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.navigation.safe.args)
}

android {
    namespace = "com.example.desainmoviereview2"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.desainmoviereview2"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
        buildConfig = true
        compose = true
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.viewpager2)

    // Import the Firebase Bill of Materials (BOM)
    implementation(platform(libs.firebase.bom))

    // Add Firebase dependencies
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.appcheck)
    implementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug)

    // Glide for image loading
    implementation(libs.glide)
    implementation(libs.glide.transformations)
    implementation("com.github.bumptech.glide:compose:1.0.0-beta01")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.02.02"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("com.google.android.material:compose-theme-adapter-3:1.1.1")
    implementation("androidx.compose.material:material-icons-extended-android:1.6.7")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.glance:glance-appwidget:1.1.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Accompanist
    implementation(libs.accompanist.pager)
    implementation(libs.accompanist.pager.indicators)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)


}