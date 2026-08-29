package com.nphkhiem.englishforyourchildren.feature.caregiver

/** Three summaries at most, which is what the draft draws and what one glance holds. */
private const val MAX_SUMMARIES = 3

/** Six recent words at most. Bounded on purpose: this is a reminder, not a history. */
private const val MAX_RECENT_WORDS = 6

/**
 * The summaries actually drawn.
 *
 * Bounded here rather than trusted from above, so the panel cannot grow a fourth column and
 * quietly become a dashboard of metrics.
 */
internal fun summariesShown(summaries: List<OverviewSummary>): List<OverviewSummary> =
    summaries.take(MAX_SUMMARIES)

/**
 * The recent words actually drawn, most recent first.
 *
 * The stop condition forbids an infinite history, and the honest way to hold that is to make the
 * screen incapable of drawing one rather than to rely on whoever fills the state. A caregiver who
 * wants every word a child has met has free play for that.
 */
internal fun recentWordsShown(words: List<String>): List<String> = words.take(MAX_RECENT_WORDS)

/** Whether there is any practice to describe, which decides between a panel and an explanation. */
internal fun hasPractice(progress: OverviewProgress): Boolean =
    progress is OverviewProgress.Practiced
