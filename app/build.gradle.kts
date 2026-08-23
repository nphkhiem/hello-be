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
}
