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
}
