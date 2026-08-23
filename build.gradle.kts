plugins {
    alias(libs.plugins.spotless)
}

spotless {
    kotlin {
        target(
            fileTree(".") {
                include("**/*.kt")
                exclude("**/build/**", "**/.gradle/**")
            }
        )
        ktlint(libs.versions.ktlint.get())
    }
    kotlinGradle {
        target(
            fileTree(".") {
                include("**/*.gradle.kts")
                exclude("**/build/**", "**/.gradle/**")
            }
        )
        ktlint(libs.versions.ktlint.get())
    }
    format("projectFiles") {
        target(
            fileTree(".") {
                include("**/*.md", "**/*.xml", "**/*.properties", "**/*.toml", "**/.gitignore")
                exclude("**/build/**", "**/.gradle/**")
            }
        )
        trimTrailingWhitespace()
        endWithNewline()
    }
}
