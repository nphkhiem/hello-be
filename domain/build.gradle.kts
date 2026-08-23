plugins {
    id("english.kotlin.jvm")
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.truth)
}
