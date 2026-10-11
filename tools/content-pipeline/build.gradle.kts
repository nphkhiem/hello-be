plugins {
    id("english.kotlin.jvm")
    application
}

// The tool that turns an authored course into signed lesson packs. Run on the owner's machine,
// never shipped: nothing in the app depends on it.
application {
    mainClass.set("com.nphkhiem.englishforyourchildren.tools.pipeline.MainKt")
}

// Paths given to `run` are read from the repository root, where an author is standing, rather
// than from this module's folder.
tasks.named<JavaExec>("run") {
    workingDir = rootDir
}

// The tests build packs from the starter course that ships, read off disk. Without this Gradle
// cannot see that input and would skip them as up to date after a content change, the mistake
// `data/build.gradle.kts` records for the same reason.
tasks.withType<Test>().configureEach {
    inputs
        .dir(layout.projectDirectory.dir("../../content/starter/src"))
        .withPropertyName("starterContent")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}

dependencies {
    implementation(projects.content.validation)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.truth)
    // The television verifies with Tink, so the tests verify what this signs with Tink too.
    testImplementation(libs.tink.android)
}
