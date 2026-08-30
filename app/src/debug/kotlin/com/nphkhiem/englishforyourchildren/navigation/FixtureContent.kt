package com.nphkhiem.englishforyourchildren.navigation

import com.nphkhiem.englishforyourchildren.domain.model.LessonId
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import com.nphkhiem.englishforyourchildren.domain.model.ShelfId
import com.nphkhiem.englishforyourchildren.domain.model.UnitId
import com.nphkhiem.englishforyourchildren.feature.caregiver.AdultGateUiState
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverConfirmationKind
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverConfirmationState
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverFixtures
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverOverviewUiState
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverSettingsUiState
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverShellState
import com.nphkhiem.englishforyourchildren.feature.learning.CelebrationFixtures
import com.nphkhiem.englishforyourchildren.feature.learning.CelebrationUiState
import com.nphkhiem.englishforyourchildren.feature.learning.ChildHomeFixtures
import com.nphkhiem.englishforyourchildren.feature.learning.ChildHomeUiState
import com.nphkhiem.englishforyourchildren.feature.learning.FreePlayFixtures
import com.nphkhiem.englishforyourchildren.feature.learning.FreePlayUiState
import com.nphkhiem.englishforyourchildren.feature.learning.LearningPathFixtures
import com.nphkhiem.englishforyourchildren.feature.learning.LearningPathUiState
import com.nphkhiem.englishforyourchildren.feature.learning.LessonFixtures
import com.nphkhiem.englishforyourchildren.feature.learning.LessonUiState
import com.nphkhiem.englishforyourchildren.feature.profiles.CreateProfileFixtures
import com.nphkhiem.englishforyourchildren.feature.profiles.CreateProfileUiState
import com.nphkhiem.englishforyourchildren.feature.profiles.ProfilePickerFixtures
import com.nphkhiem.englishforyourchildren.feature.profiles.ProfilePickerUiState

/**
 * Every screen's state, from the fixtures each feature already ships for review.
 *
 * Debug sources only. This is what makes every route walkable on a television and drivable in a
 * test without a data layer, and it is the reason no invented child appears in an installed build:
 * production reads a gateway that honestly reports it cannot read anything yet.
 *
 * It reuses the fixtures the features already own rather than inventing a parallel set, so what a
 * reviewer walks through the navigation is the same state the screens' own tests assert against.
 */
class FixtureContent(
    private val lessonStates: List<LessonUiState> = LessonFixtures.reviewStates().map { it.second }
) : HelloBeContent {

    override fun childHome(profileId: ProfileId): ChildHomeUiState = ChildHomeFixtures.returning()

    override fun learningPath(profileId: ProfileId, preferredUnitId: UnitId?): LearningPathUiState =
        LearningPathFixtures.midUnit()

    override fun lesson(profileId: ProfileId, lessonId: LessonId): LessonUiState =
        lessonStates.first()

    override fun celebration(profileId: ProfileId, lessonId: LessonId): CelebrationUiState =
        CelebrationFixtures.settled()

    override fun freePlay(profileId: ProfileId, preferredShelfId: ShelfId?): FreePlayUiState =
        FreePlayFixtures.shelves()

    override fun profilePicker(mode: ProfilePickerMode): ProfilePickerUiState =
        ProfilePickerFixtures.twoProfiles()

    override fun profileCreate(): CreateProfileUiState = CreateProfileFixtures.ready()

    override fun adultGate(): AdultGateUiState = CaregiverFixtures.gate()

    override fun caregiverShell(profileId: ProfileId?): CaregiverShellState =
        CaregiverFixtures.shell()

    override fun caregiverOverview(profileId: ProfileId?): CaregiverOverviewUiState =
        CaregiverFixtures.overview()

    override fun caregiverSettings(): CaregiverSettingsUiState = CaregiverFixtures.settings()

    override fun profileManagement(selectedProfileId: ProfileId?) = CaregiverFixtures.profiles()

    override fun confirmation(
        kind: CaregiverConfirmationKind,
        profileId: ProfileId
    ): CaregiverConfirmationState = when (kind) {
        CaregiverConfirmationKind.DELETE_PROFILE -> CaregiverFixtures.deleteConfirmation()
        CaregiverConfirmationKind.RESET_PROGRESS -> CaregiverFixtures.resetConfirmation()
    }
}

/** A gateway that answers with whatever a test or the catalog wants launch to see. */
class FixtureProfileGateway(private val snapshot: ProfileSnapshot) : ProfileGateway {
    override fun snapshot(): ProfileSnapshot = snapshot
}
