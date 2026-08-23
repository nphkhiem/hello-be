plugins {
    id("english.android.library")
}

android {
    namespace = "com.nphkhiem.englishforyourchildren.playback"
}

dependencies {
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.kotlinx.coroutines.android)
}
