package com.nphkhiem.englishforyourchildren.ui.tv.component

/**
 * How an action surface may be reached, per the Component state matrix in DESIGN_TOKENS.md.
 *
 * [DISABLED] and [UNAVAILABLE] look similar but behave differently on a D-pad: a disabled
 * control is skipped entirely by focus traversal, while an unavailable one still takes focus
 * so its "later" explanation can be read and announced. Neither emits a click.
 */
enum class HelloBeAvailability {
    /** Focusable and actionable. */
    ENABLED,

    /** Muted surface, no focus ring, cannot receive focus. */
    DISABLED,

    /** Muted surface with a quiet border; focusable so the reason can be announced, never clickable. */
    UNAVAILABLE;

    internal val isFocusable: Boolean get() = this != DISABLED

    internal val isClickable: Boolean get() = this == ENABLED
}

/**
 * Emphasis of an action, which selects its container/content pair from the token palette.
 * Tone never carries meaning on its own — a destructive action also states its verb.
 */
enum class HelloBeActionTone {
    PRIMARY,
    SECONDARY,
    DESTRUCTIVE
}

/**
 * Learning feedback carried by a [ChoiceCard]. This is deliberately separate from selection:
 * a card can be selected while feedback is still [NEUTRAL], and supportive retry never reads
 * as an error, per the "Supportive retry" row of the state matrix.
 */
enum class HelloBeChoiceFeedback {
    NEUTRAL,
    CORRECT,
    SUPPORTIVE_RETRY
}
