plugins {
    id("english.android.library")
    id("english.android.compose")
    id("english.android.hilt")
}

android {
    namespace = "com.nphkhiem.englishforyourchildren.feature.caregiver"

    lint {
        // `CaregiverTranslationTest` is this module's translation gate, and it is the stricter of
        // the two. It names every key that has no Vietnamese yet, fails when a new English string
        // joins that list without anyone deciding to, and fails again when a translation is written
        // without crossing the key off. Lint can only say that something somewhere is untranslated,
        // which here describes fifty-six deliberate gaps waiting on a native speaker, so it would
        // fail the build for a state this project has chosen and written down.
        disable += "MissingTranslation"
    }
}

dependencies {
    implementation(projects.domain)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(projects.ui.tv)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.tv.material)
    // BackHandler only, so a destructive confirmation can treat Back as its safe choice. This
    // module reaches no navigation and no playback API. It does reach the domain repositories now,
    // because the settings screen writes what a caregiver changes, which is the point of it.
    implementation(libs.androidx.activity.compose)

    testImplementation(projects.testSupport)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.truth)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.tv.material)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.junit4)
    androidTestImplementation(libs.truth)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
