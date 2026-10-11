package com.nphkhiem.englishforyourchildren.content

import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.data.pack.ContentUpdate
import com.nphkhiem.englishforyourchildren.data.pack.Refusal
import com.nphkhiem.englishforyourchildren.domain.model.CourseVersion
import com.nphkhiem.englishforyourchildren.feature.caregiver.TaskFailure
import com.nphkhiem.englishforyourchildren.feature.caregiver.TaskState
import org.junit.jupiter.api.Test

/** The updater's states, as the four words a caregiver reads. */
class PackedLessonUpdatesTest {

    @Test
    fun givenLessonsOnDiskButNotYetLive_whenRead_thenTheCaregiverIsToldTheyAreUpToDate() {
        // Going live waits for a moment no child is in a lesson. That is not the adult's to manage.
        assertThat(taskState(ContentUpdate.Staged(NEXT), lastDownloadFailed = false))
            .isEqualTo(TaskState.UpToDate)
        assertThat(taskState(ContentUpdate.Active(NEXT), lastDownloadFailed = false))
            .isEqualTo(TaskState.UpToDate)
    }

    @Test
    fun givenADownloadThatEndedWhereItStarted_whenRead_thenItSaysItFailedAndOffersARetry() {
        val back = ContentUpdate.Available(NEXT, sizeBytes = 10)

        assertThat(taskState(back, lastDownloadFailed = false)).isEqualTo(TaskState.Available)
        assertThat(taskState(back, lastDownloadFailed = true))
            .isEqualTo(TaskState.Failed(TaskFailure.COULD_NOT_FINISH))
    }

    @Test
    fun givenADownloadUnderway_whenRead_thenItCountsThePacks() {
        assertThat(taskState(ContentUpdate.Downloading(NEXT, 1, 3), lastDownloadFailed = false))
            .isEqualTo(TaskState.Working(done = 1, total = 3))
        assertThat(taskState(ContentUpdate.Checking, lastDownloadFailed = false))
            .isEqualTo(TaskState.Working(done = 0, total = 0))
    }

    @Test
    fun givenARefusal_whenRead_thenOnlyANeededAppUpdateIsToldApart() {
        assertThat(taskState(ContentUpdate.Refused(Refusal.APP_UPDATE_NEEDED), false))
            .isEqualTo(TaskState.Failed(TaskFailure.APP_UPDATE_NEEDED))
        assertThat(taskState(ContentUpdate.Refused(Refusal.SIGNATURE_INVALID), false))
            .isEqualTo(TaskState.Failed(TaskFailure.NOT_USABLE))
        assertThat(taskState(ContentUpdate.Refused(Refusal.CONTENT_INVALID), false))
            .isEqualTo(TaskState.Failed(TaskFailure.NOT_USABLE))
    }

    private companion object {
        val NEXT = CourseVersion("2026.10")
    }
}
