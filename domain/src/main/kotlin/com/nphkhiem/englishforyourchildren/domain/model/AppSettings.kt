package com.nphkhiem.englishforyourchildren.domain.model

/**
 * Everything a caregiver can change, and the profile the television is currently on.
 *
 * The six switches are exactly the six the built caregiver settings screen offers. A setting that
 * a caregiver can change with nowhere to persist it is a setting that silently forgets itself, so
 * this model and that screen have to be read together.
 *
 * [caregiverLocaleTag] is the caregiver's own language, not the child's. Child mode is English-led
 * whatever this says; the tag decides which language the caregiver area speaks and which language
 * the third rung of the support ladder uses.
 */
data class AppSettings(
    val selectedProfileId: ProfileId?,
    val caregiverLocaleTag: String,
    val vietnameseHelpEnabled: Boolean,
    val captionsEnabled: Boolean,
    val reducedMotionEnabled: Boolean,
    val highContrastEnabled: Boolean,
    val backgroundMusicEnabled: Boolean
) {
    init {
        require(caregiverLocaleTag.isNotBlank()) { "A caregiver locale tag cannot be blank" }
    }

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
            caregiverLocaleTag = "vi",
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
