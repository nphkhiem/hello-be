package com.nphkhiem.englishforyourchildren.domain.session

import com.nphkhiem.englishforyourchildren.domain.model.ActivityInstanceId
import com.nphkhiem.englishforyourchildren.domain.model.AssetId
import com.nphkhiem.englishforyourchildren.domain.model.CourseVersion
import com.nphkhiem.englishforyourchildren.domain.model.Lesson
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import com.nphkhiem.englishforyourchildren.domain.model.SessionId
import com.nphkhiem.englishforyourchildren.domain.repository.PersistCheckpoint

/** Where a lesson is: asking something, waiting for a write, or over. */
enum class LessonPhase {
    Asking,
    AwaitingCheckpoint,
    Finished
}

/**
 * Whether the work behind the child is written down.
 *
 * Pending save is the product's own word for the middle case, and it exists because nothing may
 * claim progress is saved until storage says so. [Unsaved] carries the checkpoint that did not
 * land, so a retry sends the same one rather than rebuilding it from a later moment.
 */
sealed interface SaveStatus {
    data object Saved : SaveStatus

    data class Unsaved(val pending: PersistCheckpoint) : SaveStatus
}

/**
 * Everything a lesson is, with none of its scenery.
 *
 * No strings, no pictures and no sounds: those belong to content and to the screen. What is here is
 * the part that has to be right, and it is a value so a test can hold two of them side by side.
 */
data class LessonSessionState(
    val sessionId: SessionId,
    val profileId: ProfileId,
    val courseVersion: CourseVersion,
    val lesson: Lesson,
    val activityIndex: Int,
    val currentInstance: ActivityInstanceId,
    val phase: LessonPhase,
    val saveStatus: SaveStatus,
    /** How much help Pip is giving, from none to the last rung. Reset by moving on. */
    val supportLevel: Int,
    val audioAvailable: Boolean,
    /**
     * The recording being spoken right now, or null for silence.
     *
     * Null is silence rather than a flag beside the asset, so a lesson cannot claim to be sounding
     * something it never started. What ends it is compared against this rather than against
     * [promptAsset], so a support phrase or a word finishing cannot answer for the question.
     */
    val soundingPrompt: AssetId?,
    val stopRequested: Boolean
) {
    init {
        require(activityIndex in lesson.activities.indices) {
            "A lesson cannot be on an activity it does not have"
        }
        require(supportLevel in 0..MAX_SUPPORT_LEVEL) { "Support has $MAX_SUPPORT_LEVEL rungs" }
    }

    val currentActivity get() = lesson.activities[activityIndex]

    /**
     * The recording for the question being asked right now.
     *
     * Derived rather than stored, so it cannot be left behind when a child moves on. It used to be
     * a field set once at the start, which meant every question after the first asked for the
     * first one's recording.
     */
    val promptAsset: AssetId? get() = currentActivity.content?.promptAsset

    val isLastActivity get() = activityIndex == lesson.activities.lastIndex

    companion object {
        /**
         * Calm repeat, slower repeat with a stronger demonstration, then one Vietnamese phrase.
         * There is no fourth rung: past this the help stops escalating rather than becoming louder.
         */
        const val MAX_SUPPORT_LEVEL = 3
    }
}
