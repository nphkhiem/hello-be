plugins {
    alias(libs.plugins.spotless)
}

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**", "**/.gradle/**")
        ktlint(libs.versions.ktlint.get())
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**", "**/.gradle/**")
        ktlint(libs.versions.ktlint.get())
    }
    format("projectFiles") {
        target("**/*.md", "**/*.xml", "**/*.properties", "**/*.toml", "**/.gitignore")
        targetExclude("**/build/**", "**/.gradle/**")
        trimTrailingWhitespace()
        endWithNewline()
    }
}
