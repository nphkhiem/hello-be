plugins {
    `kotlin-dsl`
}

group = "com.nphkhiem.englishforyourchildren.buildlogic"

dependencies {
    implementation(libs.android.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.compose.compiler.gradle.plugin)
    implementation(libs.ksp.gradle.plugin)
    implementation(libs.hilt.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "english.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "english.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "english.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("androidHilt") {
            id = "english.android.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        register("kotlinJvm") {
            id = "english.kotlin.jvm"
            implementationClass = "KotlinJvmConventionPlugin"
        }
    }
}
