package com.nphkhiem.englishforyourchildren.ui.tv.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.nphkhiem.englishforyourchildren.ui.tv.R

/**
 * Nunito Sans, the `type.family.primary` the tokens have always named.
 *
 * Bundled as a **variable** font rather than static weights because the ramp below asks for 450
 * and 750, which no static file carries. Static weights would round them to 500 and 700, and
 * `bodyMedium`, `bodySmall` and `headlineMedium` would quietly lose the distinction the tokens
 * drew. Every weight is declared against the one file with an explicit variation setting, so the
 * axis is resolved rather than approximated.
 *
 * Nunito Sans is also the family the tokens chose partly for its Vietnamese diacritics, which the
 * bilingual caregiver surfaces depend on and which the platform sans had never been checked for.
 *
 * Licence: SIL Open Font License 1.1, retained at `ui/tv/licenses/NunitoSans-OFL.txt`. The licence
 * requires the text accompany the font wherever it is distributed; there is currently no surface in
 * the app that displays it, which is the same unresolved attribution question the content licences
 * raise.
 */
object HelloBeFontFamily {
    val primary: FontFamily = FontFamily(
        nunitoSans(WEIGHT_BODY_LIGHT),
        nunitoSans(WEIGHT_BODY),
        nunitoSans(WEIGHT_CAPTION),
        nunitoSans(WEIGHT_TITLE),
        nunitoSans(WEIGHT_HEADLINE),
        nunitoSans(WEIGHT_DISPLAY)
    )
    val fallback: FontFamily = FontFamily.SansSerif
    val mono: FontFamily = FontFamily.Monospace
}

private const val WEIGHT_BODY_LIGHT = 450
private const val WEIGHT_BODY = 500
private const val WEIGHT_CAPTION = 600
private const val WEIGHT_TITLE = 700
private const val WEIGHT_HEADLINE = 750
private const val WEIGHT_DISPLAY = 800

/**
 * One weight off the shared variable file.
 *
 * [FontWeight] is what Compose matches a `TextStyle` against; the variation setting is what tells
 * the renderer where to sit on the axis. Both are needed, and giving only the first is how a
 * variable font ends up drawing every role at the same thickness.
 */
private fun nunitoSans(weight: Int) = Font(
    resId = R.font.nunito_sans_variable,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight))
)

@Immutable
data class HelloBeTypography(
    val displayHero: TextStyle,
    val displayLessonPrompt: TextStyle,
    val headlineLarge: TextStyle,
    val headlineMedium: TextStyle,
    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val titleSmall: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    val labelLarge: TextStyle,
    val labelMedium: TextStyle,
    val labelSmall: TextStyle,
    val caption: TextStyle,
    val learningGlyph: TextStyle
)

private val tightTracking = (-0.02).em
private val normalTracking = 0.em

fun helloBeTypography(fontFamily: FontFamily = HelloBeFontFamily.primary): HelloBeTypography {
    fun role(size: Int, lineHeight: Int, weight: Int, letterSpacing: TextUnit = normalTracking) =
        TextStyle(
            fontFamily = fontFamily,
            fontSize = size.sp,
            lineHeight = lineHeight.sp,
            fontWeight = FontWeight(weight),
            letterSpacing = letterSpacing
        )

    return HelloBeTypography(
        displayHero = role(48, 52, 800, tightTracking),
        displayLessonPrompt = role(40, 44, 800, tightTracking),
        headlineLarge = role(36, 40, 800),
        headlineMedium = role(32, 36, 750),
        titleLarge = role(28, 34, 700),
        titleMedium = role(22, 28, 700),
        titleSmall = role(20, 26, 700),
        bodyLarge = role(20, 29, 500),
        bodyMedium = role(18, 26, 450),
        bodySmall = role(16, 23, 450),
        labelLarge = role(18, 22, 700),
        labelMedium = role(16, 20, 700),
        labelSmall = role(14, 18, 700),
        caption = role(20, 28, 600),
        learningGlyph = role(96, 104, 800)
    )
}
