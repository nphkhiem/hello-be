package com.nphkhiem.englishforyourchildren

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Starts instrumented tests on Hilt's test application rather than the real one.
 *
 * That is what lets a test replace a binding. Nothing is lost by the swap: [HelloBeApplication]
 * carries no behaviour of its own, only the annotation that builds the graph.
 */
class HelloBeTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        classLoader: ClassLoader?,
        className: String?,
        context: Context?
    ): Application =
        super.newApplication(classLoader, HiltTestApplication::class.java.name, context)
}
