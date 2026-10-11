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
 * the background a dropped download is simply tried again; a caregiver who pressed the row and saw
 * it quietly return to "Download new lessons" would not know it had failed, so here a download that
 * ends where it started says so, with a retry.
 */
class PackedLessonUpdates(private val updater: ContentUpdater) : LessonUpdates {
    private val lastDownloadFailed = MutableStateFlow(false)

    override val offered = true

    override val state: Flow<TaskState> =
        combine(updater.state, lastDownloadFailed) { update, failed -> taskState(update, failed) }

    override suspend fun check() {
        lastDownloadFailed.value = false
        updater.check()
    }

    override suspend fun download() {
        lastDownloadFailed.value = false
        updater.download()
        lastDownloadFailed.value = updater.state.value is ContentUpdate.Available
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
 */
internal fun taskState(update: ContentUpdate, lastDownloadFailed: Boolean): TaskState =
    when (update) {
        ContentUpdate.Idle,
        ContentUpdate.UpToDate,
        is ContentUpdate.Staged,
        is ContentUpdate.Active -> TaskState.UpToDate

        is ContentUpdate.Available -> if (lastDownloadFailed) {
            TaskState.Failed(TaskFailure.COULD_NOT_FINISH)
        } else {
            TaskState.Available
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
