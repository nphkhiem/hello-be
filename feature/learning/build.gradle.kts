plugins {
    id("english.android.library")
    id("english.android.compose")
    id("english.android.hilt")
}

android {
    namespace = "com.nphkhiem.englishforyourchildren.feature.learning"
}

dependencies {
    implementation(projects.domain)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(projects.ui.tv)
    implementation(projects.playback)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.tv.material)
    // BackHandler only. This module still reaches no navigation, repository or playback API.
    implementation(libs.androidx.activity.compose)

    testImplementation(projects.testSupport)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.truth)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.tv.material)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.junit4)
    androidTestImplementation(libs.truth)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
