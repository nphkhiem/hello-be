package com.nphkhiem.englishforyourchildren.feature.caregiver

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.nphkhiem.englishforyourchildren.domain.model.CaregiverLanguage
import java.util.Locale

/**
 * The language the caregiver area is being read in.
 *
 * Provided around the caregiver destinations and nowhere else. That scoping is the design, not an
 * optimisation: child mode is English-led whatever a caregiver sets here, so the override must not
 * be able to reach it. A default of [CaregiverLanguage.BOTH] means a caller who forgets to provide
 * one still shows a caregiver something they can read.
 */
val LocalCaregiverLanguage = compositionLocalOf { CaregiverLanguage.BOTH }

/**
 * One string, in whichever language or languages the caregiver reads.
 *
 * Resolved through `createConfigurationContext` rather than by switching the app's locale, because
 * the app's locale is the child's and this is one region of one screen. No new dependency, and the
 * same on API 28 as on 36.
 *
 * In [CaregiverLanguage.BOTH] the two are joined by the middle dot the approved strings already
 * used when they carried both languages themselves. When a string has no Vietnamese yet, Android
 * hands back the English one, and joining a sentence to itself would read as a stutter rather than
 * as bilingual, so the English simply stands alone. That is what makes an unfinished `values-vi`
 * safe to ship.
 */
@Composable
@ReadOnlyComposable
fun caregiverText(@StringRes id: Int, vararg formatArgs: Any): String =
    LocalContext.current.caregiverText(LocalCaregiverLanguage.current, id, formatArgs)

/** The same resolution, reachable from somewhere that is not a composition. */
internal fun Context.caregiverText(
    language: CaregiverLanguage,
    @StringRes id: Int,
    formatArgs: Array<out Any> = emptyArray()
): String {
    val english = inLocale(ENGLISH).read(id, formatArgs)
    if (language == CaregiverLanguage.ENGLISH) return english

    val vietnamese = inLocale(VIETNAMESE).read(id, formatArgs)
    if (language == CaregiverLanguage.VIETNAMESE) return vietnamese

    return if (vietnamese == english) english else "$english$JOIN$vietnamese"
}

private fun Context.read(@StringRes id: Int, formatArgs: Array<out Any>): String =
    if (formatArgs.isEmpty()) getString(id) else getString(id, *formatArgs)

private fun Context.inLocale(language: String): Context {
    val configuration = android.content.res.Configuration(resources.configuration)
    configuration.setLocale(Locale.forLanguageTag(language))
    return createConfigurationContext(configuration)
}

/** English, then Vietnamese, joined the way the approved strings already joined them. */
private const val JOIN = " · "
private const val ENGLISH = "en"
private const val VIETNAMESE = "vi"
