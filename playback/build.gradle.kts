plugins {
    id("english.android.library")
    id("english.android.hilt")
}

android {
    namespace = "com.nphkhiem.englishforyourchildren.playback"
}

dependencies {
    // api, not implementation: PlaybackController names AssetId in its own signature, so every
    // caller needs the type on its compile classpath.
    api(projects.domain)
    // api: the controller implements DefaultLifecycleObserver, so the type is part of its shape.
    api(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.kotlinx.coroutines.android)

    androidTestImplementation(libs.androidx.test.ext.junit)
    // This module has no Compose, so it asks for the runner directly or the test APK has none.
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.junit4)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
