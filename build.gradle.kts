plugins {
    alias(libs.plugins.spotless)
}

// `.idea/` is excluded everywhere below. Those files are written by the IDE, not by this project:
// they are not in version control, nobody reviews them, and the IDE rewrites `workspace.xml`
// without a trailing newline whenever it feels like it, which failed the format check on a file no
// change of ours had touched.
spotless {
    kotlin {
        target(
            fileTree(".") {
                include("**/*.kt")
                exclude("**/build/**", "**/.gradle/**", "**/.idea/**")
            }
        )
        ktlint(libs.versions.ktlint.get())
    }
    kotlinGradle {
        target(
            fileTree(".") {
                include("**/*.gradle.kts")
                exclude("**/build/**", "**/.gradle/**", "**/.idea/**")
            }
        )
        ktlint(libs.versions.ktlint.get())
    }
    format("projectFiles") {
        target(
            fileTree(".") {
                include("**/*.md", "**/*.xml", "**/*.properties", "**/*.toml", "**/.gitignore")
                exclude("**/build/**", "**/.gradle/**", "**/.idea/**")
            }
        )
        trimTrailingWhitespace()
        endWithNewline()
    }
}
