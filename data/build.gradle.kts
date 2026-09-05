import org.gradle.api.tasks.PathSensitivity

plugins {
    id("english.android.library")
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.nphkhiem.englishforyourchildren.data"

    sourceSets {
        // The migration test reads the exported schemas out of the test APK, so the directory KSP
        // writes them to has to travel with it.
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }
}

// `StarterContentTest` opens the packaged bundle off disk rather than through a fixture, which is
// the point of it: it checks the same files the app will, in both source sets, down to which
// recordings are actually present. Gradle cannot see that from this module's source tree, so
// without this a content mistake ships green, the tests having been skipped as up to date because
// no Kotlin changed. A wrong answer in the shipped bundle survived exactly that way.
tasks.withType<Test>().configureEach {
    inputs
        .dir(layout.projectDirectory.dir("../content/starter/src"))
        .withPropertyName("shippedContent")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}

ksp {
    arg("room.generateKotlin", "true")
    arg("room.schemaLocation", file("schemas").path)
}

dependencies {
    implementation(projects.domain)
    implementation(libs.kotlinx.serialization.json)
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
