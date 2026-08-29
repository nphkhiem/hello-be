package com.nphkhiem.englishforyourchildren.navigation

import com.nphkhiem.englishforyourchildren.feature.caregiver.AdultGateUiState
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverConfirmationKind
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverConfirmationState
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverOverviewUiState
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverSettingsUiState
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverShellState
import com.nphkhiem.englishforyourchildren.feature.caregiver.ProfileManagementUiState
import com.nphkhiem.englishforyourchildren.feature.learning.CelebrationUiState
import com.nphkhiem.englishforyourchildren.feature.learning.ChildHomeUiState
import com.nphkhiem.englishforyourchildren.feature.learning.FreePlayUiState
import com.nphkhiem.englishforyourchildren.feature.learning.LearningPathUiState
import com.nphkhiem.englishforyourchildren.feature.learning.LessonUiState
import com.nphkhiem.englishforyourchildren.feature.profiles.CreateProfileUiState
import com.nphkhiem.englishforyourchildren.feature.profiles.ProfilePickerUiState

/**
 * Where every screen's state comes from.
 *
 * The adapter seam this task is asked to define, and the whole of what a data layer will have to
 * satisfy. It is deliberately a plain interface of pure reads: no Room type, no coroutine, no
 * lifecycle and nothing mutable crosses it, so the navigation host can be driven by a fake in a
 * test as easily as by a database in production.
 *
 * There is no production implementation yet, and the app is honest about that rather than shipping
 * invented children: with no source of profiles the entry resolver reports storage unreadable and
 * the app opens on the caregiver recovery, which is exactly what an installed build with no data
 * layer should say. The debug catalog supplies a fixture-backed implementation so every route can
 * still be walked on a television.
 */
interface HelloBeContent {
    fun childHome(profileId: ProfileId): ChildHomeUiState

    fun learningPath(profileId: ProfileId, preferredUnitId: UnitId?): LearningPathUiState

    fun lesson(profileId: ProfileId, lessonId: LessonId): LessonUiState

    fun celebration(profileId: ProfileId, lessonId: LessonId): CelebrationUiState

    fun freePlay(profileId: ProfileId, preferredShelfId: ShelfId?): FreePlayUiState

    fun profilePicker(mode: ProfilePickerMode): ProfilePickerUiState

    fun profileCreate(): CreateProfileUiState

    fun adultGate(): AdultGateUiState

    fun caregiverShell(profileId: ProfileId?): CaregiverShellState

    fun caregiverOverview(profileId: ProfileId?): CaregiverOverviewUiState

    fun caregiverSettings(): CaregiverSettingsUiState

    fun profileManagement(selectedProfileId: ProfileId?): ProfileManagementUiState

    fun confirmation(
        kind: CaregiverConfirmationKind,
        profileId: ProfileId
    ): CaregiverConfirmationState
}

/**
 * Reads what is stored about profiles.
 *
 * Separate from [HelloBeContent] because launch depends on it before any screen exists, and
 * because it is the one read the app cannot start without.
 */
interface ProfileGateway {
    fun snapshot(): ProfileSnapshot
}

/**
 * The gateway an installed build has until a data layer lands.
 *
 * It reports that storage cannot be read, which is true: nothing can read it yet. That sends the
 * app to the caregiver recovery, which explains the situation to an adult and offers a retry, and
 * is a great deal more honest than inventing a child to show a home screen to.
 */
class UnavailableProfileGateway : ProfileGateway {
    override fun snapshot(): ProfileSnapshot = ProfileSnapshot(
        storageReadable = false,
        validProfileIds = emptyList(),
        rememberedProfileId = null
    )
}
