package com.nphkhiem.englishforyourchildren.content

import com.nphkhiem.englishforyourchildren.data.pack.ContentUpdate
import com.nphkhiem.englishforyourchildren.data.pack.ContentUpdater
import com.nphkhiem.englishforyourchildren.data.pack.Refusal
import com.nphkhiem.englishforyourchildren.feature.caregiver.LessonUpdates
import com.nphkhiem.englishforyourchildren.feature.caregiver.TaskFailure
import com.nphkhiem.englishforyourchildren.feature.caregiver.TaskState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

/**
 * The caregiver's view of the content updater.
 *
 * The one translation the two sides need. The updater keeps transient failures silent, because in
 * the background a dropped download is simply tried again. A caregiver who asked is owed the truth
 * instead: a download that ends where it started, or a check that never reached the server, says
 * so with a retry, rather than leaving "Download new lessons" or "Up to date" on a television that
 * could not find out.
 */
class PackedLessonUpdates(private val updater: ContentUpdater) : LessonUpdates {
    private val lastAttemptFailed = MutableStateFlow(false)

    override val offered = true

    override val state: Flow<TaskState> =
        combine(updater.state, lastAttemptFailed) { update, failed -> taskState(update, failed) }

    override suspend fun check() {
        lastAttemptFailed.value = false
        lastAttemptFailed.value = !updater.check()
    }

    override suspend fun download() {
        lastAttemptFailed.value = false
        updater.download()
        lastAttemptFailed.value = updater.state.value is ContentUpdate.Available
    }
}

/** A build that has nowhere to get lessons from. The Lessons group is not drawn at all. */
object UnofferedLessonUpdates : LessonUpdates {
    override val offered = false
    override val state: Flow<TaskState> = flowOf(TaskState.UpToDate)

    override suspend fun check() = Unit

    override suspend fun download() = Unit
}

/**
 * Where the updater stands, in the caregiver's four words.
 *
 * Staged reads as up to date: from where the caregiver sits the lessons are here, and the moment
 * they go live is a rule about children's sessions rather than something to ask an adult to manage.
 * Any resting state reads as a failure with a retry when the caregiver's last attempt went nowhere,
 * because the state itself only says what was true before they asked.
 */
internal fun taskState(update: ContentUpdate, lastAttemptFailed: Boolean): TaskState =
    when (update) {
        ContentUpdate.Idle,
        ContentUpdate.UpToDate,
        is ContentUpdate.Staged,
        is ContentUpdate.Available -> when {
            lastAttemptFailed -> TaskState.Failed(TaskFailure.COULD_NOT_FINISH)
            update is ContentUpdate.Available -> TaskState.Available
            else -> TaskState.UpToDate
        }

        ContentUpdate.Checking, is ContentUpdate.Verifying -> TaskState.Working(done = 0, total = 0)

        is ContentUpdate.Downloading -> TaskState.Working(update.packsDone, update.packsTotal)

        is ContentUpdate.Refused -> TaskState.Failed(
            when (update.reason) {
                Refusal.APP_UPDATE_NEEDED -> TaskFailure.APP_UPDATE_NEEDED
                Refusal.SIGNATURE_INVALID, Refusal.CONTENT_INVALID -> TaskFailure.NOT_USABLE
            }
        )
    }
