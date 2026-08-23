plugins {
    id("english.kotlin.jvm")
}

dependencies {
    api(projects.domain)
    api(libs.junit.jupiter)
    api(libs.truth)
    api(libs.kotlinx.coroutines.core)
}
