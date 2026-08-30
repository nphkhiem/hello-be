plugins {
    id("english.android.library")
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.nphkhiem.englishforyourchildren.data"
}

ksp {
    arg("room.generateKotlin", "true")
    arg("room.schemaLocation", file("schemas").path)
}

dependencies {
    implementation(projects.domain)
    implementation(libs.hilt.android)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    ksp(libs.androidx.room.compiler)
    ksp(libs.hilt.compiler)

    testImplementation(projects.testSupport)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.truth)

    androidTestImplementation(projects.testSupport)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.test.ext.junit)
    // :ui:tv gets the runner transitively through the Compose test artifact. This module has no
    // Compose, so it asks for it directly, or the test APK has no runner to start.
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.junit4)
    androidTestImplementation(libs.truth)
}
