plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("androidx.navigation.safeargs")
}

android {
    namespace = "com.example.groupprojectfirsttry"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.groupprojectfirsttry"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    //flavors
    flavorDimensions += "brand"

    productFlavors {

        create("standard") {
            dimension = "brand"
            applicationId = "com.example.groupprojectfirsttry"
            resValue("string", "app_name", "MPOS")
            buildConfigField("Boolean", "CAN_CHANGE_THEME", "true")
            buildConfigField("Boolean", "USE_DOCX_THEORY", "true")
            buildConfigField("Boolean", "SUPPORT_ADAPTIVE_TRAINER", "true")
        }

        create("impuls") {
            dimension = "brand"
            applicationId = "com.example.groupprojectfirsttry.impuls"
            resValue("string", "app_name", "Impuls")
            buildConfigField("Boolean", "CAN_CHANGE_THEME", "true")
            buildConfigField("Boolean", "USE_DOCX_THEORY", "false")
            buildConfigField("Boolean", "SUPPORT_ADAPTIVE_TRAINER", "false")
        }
    }

    buildFeatures {
        buildConfig = true // Нужно для buildConfigField
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
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.3")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    // MPAndroidChart v3.1.0 and up requires Java 8
    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation("org.apache.poi:poi-ooxml:5.2.3")
    implementation("org.apache.xmlbeans:xmlbeans:5.1.1")
    implementation("org.apache.commons:commons-compress:1.23.0")
    implementation("org.apache.commons:commons-collections4:4.4")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")
    implementation("androidx.cardview:cardview:1.0.0")

    // Skeleton/Shimmer effect
    implementation("com.facebook.shimmer:shimmer:0.5.0")

    // ExoPlayer (Media3)
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
    implementation("androidx.media3:media3-common:1.4.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
