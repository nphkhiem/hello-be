plugins {
    id("english.android.library")
    id("english.android.compose")
}

android {
    namespace = "com.nphkhiem.englishforyourchildren.ui.tv"
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.tv.material)
}
