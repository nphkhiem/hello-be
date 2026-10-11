plugins {
    id("english.kotlin.jvm")
    alias(libs.plugins.kotlin.serialization)
}

// Plain JVM on purpose: the content pipeline that builds lesson packs runs on a build machine, not
// a television, and it has to parse and validate exactly as the app does. An Android library could
// not be its dependency.
dependencies {
    api(projects.domain)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.truth)
}
