plugins {
    id("english.android.application")
    id("english.android.compose")
    id("english.android.hilt")
}

android {
    namespace = "com.nphkhiem.englishforyourchildren"

    defaultConfig {
        applicationId = "com.nphkhiem.englishforyourchildren"
        versionCode = 1
        versionName = "0.1.0"

        // Swaps the application for HiltTestApplication so an instrumented test can replace
        // bindings. HelloBeApplication carries nothing but @HiltAndroidApp, so nothing is lost.
        testInstrumentationRunner = "com.nphkhiem.englishforyourchildren.HelloBeTestRunner"
    }
}

dependencies {
    implementation(projects.feature.profiles)
    implementation(projects.feature.learning)
    implementation(projects.feature.caregiver)
    implementation(projects.ui.tv)
    implementation(projects.playback)
    implementation(projects.domain)
    implementation(projects.data)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(projects.content.starter)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.tv.material)
    implementation(libs.kotlinx.coroutines.android)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.truth)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.tv.material)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.junit4)
    androidTestImplementation(libs.truth)
    // The custom runner subclasses AndroidJUnitRunner, which the Compose test artifact brings in
    // at runtime but not on the compile classpath.
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.hilt.android.testing)
    // Test scope only, and deliberately. Building a journey's graph means naming the storage types
    // it replaces, while production :app still cannot reach a database directly.
    androidTestImplementation(libs.androidx.room.runtime)
    androidTestImplementation(libs.androidx.datastore.preferences)
    kspAndroidTest(libs.hilt.compiler)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
