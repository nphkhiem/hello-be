package com.nphkhiem.englishforyourchildren.journey

import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

/**
 * A downloaded course, put where a finished download lands, without downloading it.
 *
 * The shipped starter course read out of this app's own assets, declared as the next version, with
 * its first two questions reworded so the screen says which course is teaching.
 */
object TestPacks {
    const val SEED_FIRST = "Where are the eyes?"
    const val PACK_FIRST = "Where are your eyes?"
    const val SEED_SECOND = "Where is the nose?"
    const val PACK_SECOND = "Where is your nose?"

    private const val COURSE = "curriculum/course.json"
    private const val UNITS = "curriculum/units"
    private const val ATTRIBUTIONS = "attribution/attributions.json"
    private const val SEED_VERSION = "\"2026.09\""
    private const val PACK_VERSION = "2026.10"

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    /** Where the app keeps downloaded packs. Journeys clear it before and after themselves. */
    fun folder(): File = File(context.filesDir, "packs")

    fun stage(packs: File) {
        val pack = File(packs, PACK_VERSION)
        listOf(COURSE, ATTRIBUTIONS).forEach { copyAsset(it, pack) }
        context.assets.list(UNITS).orEmpty().forEach { copyAsset("$UNITS/$it", pack) }

        File(pack, COURSE).apply {
            writeText(readText().replace(SEED_VERSION, "\"$PACK_VERSION\""))
        }
        File(pack, "$UNITS/u01-my-body.json").apply {
            writeText(readText().replace(SEED_FIRST, PACK_FIRST).replace(SEED_SECOND, PACK_SECOND))
        }
    }

    private fun copyAsset(path: String, pack: File) {
        val target = File(pack, path).apply { parentFile?.mkdirs() }
        context.assets.open(path).use { input -> target.outputStream().use { input.copyTo(it) } }
    }
}
