package com.nphkhiem.englishforyourchildren.debugcatalog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

/**
 * Debug-only entry point for the Storybook Stage token catalog. Declared solely in the
 * `debug` source set and manifest, so release builds cannot compile, package, or navigate
 * to it, per HB-D01's "release sources cannot navigate to the debug catalog" requirement.
 */
class ThemeCatalogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ThemeCatalogScreen()
        }
    }
}
