plugins {
    id("english.kotlin.jvm")
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.truth)
}
