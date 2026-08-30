plugins {
    id("english.kotlin.jvm")
}

dependencies {
    api(projects.domain)
    api(libs.kotlinx.coroutines.core)

    // Not api: the builders and fakes use neither. Exporting a test framework would force JUnit 5
    // onto every consumer, including Android instrumented tests, which run on JUnit 4 and whose
    // packaging step fails outright when both are present.
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.truth)
}
