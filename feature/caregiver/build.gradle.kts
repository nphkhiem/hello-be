plugins {
    id("english.android.library")
    id("english.android.compose")
}

android {
    namespace = "com.nphkhiem.englishforyourchildren.feature.caregiver"
}

dependencies {
    implementation(projects.domain)
    implementation(projects.ui.tv)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.tv.material)
}
