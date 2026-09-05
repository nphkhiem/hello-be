package com.nphkhiem.englishforyourchildren.domain.model

/**
 * Which language the caregiver area speaks.
 *
 * Three modes rather than a locale tag, because the approved settings screen offers three and the
 * third is both languages at once, which no locale names. [stored] is what goes in the preferences
 * file; [from] reads back the two single-language tags earlier builds wrote, so a television that
 * was already set to Vietnamese stays set to it.
 *
 * This is the caregiver's own language and never the child's. Child mode is English-led whatever
 * this says.
 */
enum class CaregiverLanguage(val stored: String) {
    ENGLISH("en"),
    VIETNAMESE("vi"),
    BOTH("en+vi");

    companion object {
        /**
         * Anything unrecognised, including nothing at all, reads as [BOTH].
         *
         * A preference is not a child's work, so one that will not read falls back to a usable
         * default rather than failing, which is the choice the settings file's corruption handler
         * already made. [BOTH] is the fallback because it is the only mode that cannot strand a
         * caregiver who reads just one of the two languages.
         */
        fun from(stored: String?): CaregiverLanguage =
            entries.firstOrNull { it.stored == stored } ?: BOTH
    }
}

/**
 * The same sentence, in whichever language or languages the caregiver reads.
 *
 * [CaregiverLanguage.BOTH] joins the two with the middle dot the approved caregiver strings already
 * used when they carried both languages themselves. A Vietnamese that is missing, or that is only
 * the English over again, stands as the English alone: joining a sentence to itself reads as a
 * stutter rather than as bilingual, and a caregiver who asked for Vietnamese and has none written
 * yet is better served by words they may not read than by an empty line.
 *
 * This is the rule for text that arrives already written in both languages, which is content. A
 * string resource resolves through the caregiver module instead, and then joins with this.
 */
fun CaregiverLanguage.read(english: String, vietnamese: String): String {
    if (this == CaregiverLanguage.ENGLISH) return english
    if (vietnamese.isBlank() || vietnamese == english) return english
    if (this == CaregiverLanguage.VIETNAMESE) return vietnamese
    return "$english$CAREGIVER_JOIN$vietnamese"
}

/** English, then Vietnamese, joined the way the approved caregiver strings already joined them. */
const val CAREGIVER_JOIN = " · "

/**
 * Everything a caregiver can change, and the profile the television is currently on.
 *
 * The six switches are exactly the six the built caregiver settings screen offers. A setting that
 * a caregiver can change with nowhere to persist it is a setting that silently forgets itself, so
 * this model and that screen have to be read together.
 *
 * [caregiverLanguage] is the caregiver's own language, not the child's. Child mode is English-led
 * whatever this says.
 */
data class AppSettings(
    val selectedProfileId: ProfileId?,
    val caregiverLanguage: CaregiverLanguage,
    val vietnameseHelpEnabled: Boolean,
    val captionsEnabled: Boolean,
    val reducedMotionEnabled: Boolean,
    val highContrastEnabled: Boolean,
    val backgroundMusicEnabled: Boolean
) {
    companion object {
        /**
         * What a television that has never been configured looks like.
         *
         * Captions and Vietnamese help are on, because the cost of them being on for a child who
         * does not need them is small and the cost of being off for one who does is a lesson they
         * cannot follow. Reduced motion and high contrast are off, because they change the
         * experience for everyone and should be chosen deliberately.
         */
        val DEFAULT = AppSettings(
            selectedProfileId = null,
            caregiverLanguage = CaregiverLanguage.BOTH,
            vietnameseHelpEnabled = true,
            captionsEnabled = true,
            reducedMotionEnabled = false,
            highContrastEnabled = false,
            backgroundMusicEnabled = true
        )
    }
}

/**
 * Who made a packaged asset and under what licence.
 *
 * The runtime slice of `ATTRIBUTION_LEDGER.md`: only what would be shown to a person, not the
 * provenance and evidence columns, which exist so a human can audit the ledger rather than so the
 * app can display them.
 */
data class Attribution(
    val assetId: AssetId,
    val source: String,
    val licence: String,
    val attributionText: String?
) {
    init {
        require(source.isNotBlank()) { "An attribution needs a source" }
        require(licence.isNotBlank()) { "An attribution needs a licence" }
    }
}
