plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.stage.play"
    compileSdk = 33

    defaultConfig {
        minSdk = 23

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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

    implementation("androidx.core:core-ktx:1.10.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    implementation ("org.checkerframework:checker-qual:3.38.0")

//    implementation("com.github.rubensousa:previewseekbar:3.1.1")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    /*Media 3 player*/
    val  media3Version = "1.1.1"

    // For media playback using ExoPlayer
    api("androidx.media3:media3-exoplayer:$media3Version")

    // For DASH playback support with ExoPlayer
    api ("androidx.media3:media3-exoplayer-dash:$media3Version")
    // For HLS playback support with ExoPlayer
    api ("androidx.media3:media3-exoplayer-hls:$media3Version")
    // For RTSP playback support with ExoPlayer
    api ("androidx.media3:media3-exoplayer-rtsp:$media3Version")
    // For ad insertion using the Interactive Media Ads SDK with ExoPlayer
    api ("androidx.media3:media3-exoplayer-ima:$media3Version")

    // For loading data using the Cronet network stack
    api ("androidx.media3:media3-datasource-cronet:$media3Version")
    // For loading data using the OkHttp network stack
    api ("androidx.media3:media3-datasource-okhttp:$media3Version")
    // For loading data using librtmp
    api ("androidx.media3:media3-datasource-rtmp:$media3Version")

    // For building media playback UIs
    api ("androidx.media3:media3-ui:$media3Version")
    // For building media playback UIs for Android TV using the Jetpack Leanback library
    api ("androidx.media3:media3-ui-leanback:$media3Version")

    // For exposing and controlling media sessions
    api ("androidx.media3:media3-session:$media3Version")

    // For extracting data from media containers
    api ("androidx.media3:media3-extractor:$media3Version")

    // For integrating with Cast
    api ("androidx.media3:media3-cast:$media3Version")

    // For scheduling background operations using Jetpack Work's WorkManager with ExoPlayer
    api ("androidx.media3:media3-exoplayer-workmanager:$media3Version")

}