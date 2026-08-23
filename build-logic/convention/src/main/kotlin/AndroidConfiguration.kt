import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

internal const val COMPILE_SDK = 37
internal const val MIN_SDK = 28
internal const val TARGET_SDK = 37

internal fun Project.configureAndroid(extension: CommonExtension) {
    extension.apply {
        compileSdk = COMPILE_SDK
        defaultConfig.apply {
            minSdk = MIN_SDK
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
        compileOptions.apply {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
        buildFeatures.apply {
            buildConfig = false
        }
        testOptions.apply {
            unitTests.all {
                it.useJUnitPlatform()
            }
        }
    }

    extensions.configure<KotlinAndroidProjectExtension> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
    dependencies.add("testRuntimeOnly", libs.findLibrary("junit-platform-launcher").get())
}
