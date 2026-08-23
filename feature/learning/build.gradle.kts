plugins {
    id("english.android.library")
    id("english.android.compose")
}

android {
    namespace = "com.nphkhiem.englishforyourchildren.feature.learning"
}

dependencies {
    implementation(projects.domain)
    implementation(projects.ui.tv)
    implementation(projects.playback)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.tv.material)
}
