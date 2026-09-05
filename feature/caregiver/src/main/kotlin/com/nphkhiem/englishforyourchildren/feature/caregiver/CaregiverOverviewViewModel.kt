package com.nphkhiem.englishforyourchildren.feature.caregiver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import com.nphkhiem.englishforyourchildren.domain.progression.tallySkills
import com.nphkhiem.englishforyourchildren.domain.repository.CurriculumRepository
import com.nphkhiem.englishforyourchildren.domain.repository.ProfileRepository
import com.nphkhiem.englishforyourchildren.domain.repository.ProgressRepository
import com.nphkhiem.englishforyourchildren.domain.result.DomainResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * How much practice there has been, counted.
 *
 * Three numbers and nothing else. There is deliberately no count of what a child got wrong, because
 * a caregiver reading this is being told how their child is getting on rather than being handed a
 * result, and a second number beside these could only ever mean a failure. The design brief bans
 * ranking language and the information architecture calls this summary privacy minimised; between
 * them that leaves counting what was met.
 */
data class OverviewCounts(val wordsMet: Int, val lessonsFinished: Int, val wordsNeedingReview: Int)

/**
 * What there is to say about one child's practice.
 *
 * [counts] is null when the child has never practised, which is a different thing from having
 * practised nothing lately and different again from a row of zeroes. A zero reads as a poor result;
 * the absence reads as what it is.
 *
 * No words here. Every label, every note and the period itself are written where there are string
 * resources and a caregiver language, the same way the gate's question is.
 */
data class CaregiverOverviewState(
    val profileName: String,
    val counts: OverviewCounts?,
    val recentWords: List<String>,
    val pendingSave: Boolean,
    val unreadable: Boolean
)

/**
 * The caregiver's view of their child's practice.
 *
 * It reads attempts and never passes one on. What leaves here is counts and the words themselves,
 * which is the whole of what a caregiver was promised: the information architecture calls this
 * summary privacy minimised, and the way to keep that true is for the aggregation to happen below
 * the state rather than above it.
 */
@HiltViewModel
class CaregiverOverviewViewModel @Inject constructor(
    private val profiles: ProfileRepository,
    private val progress: ProgressRepository,
    private val curriculum: CurriculumRepository
) : ViewModel() {
    private val _state = MutableStateFlow(
        CaregiverOverviewState(
            profileName = "",
            counts = null,
            recentWords = emptyList(),
            pendingSave = false,
            unreadable = false
        )
    )

    val state: StateFlow<CaregiverOverviewState> = _state.asStateFlow()

    fun start(profileId: ProfileId) {
        viewModelScope.launch {
            combine(
                profiles.observeProfiles(),
                progress.observeProfileProgress(profileId),
                curriculum.observeCourse()
            ) { people, practice, course -> Triple(people, practice, course) }
                .collect { (people, practice, course) ->
                    if (people !is DomainResult.Success ||
                        practice !is DomainResult.Success ||
                        course !is DomainResult.Success
                    ) {
                        _state.value = _state.value.copy(unreadable = true)
                        return@collect
                    }

                    val met = tallySkills(course.value, practice.value.attempts)
                    val labels = labelsIn(course.value)

                    _state.value = CaregiverOverviewState(
                        profileName = people.value
                            .firstOrNull { it.id == profileId }?.nickname.orEmpty(),
                        // Never practised is the absence of counts, not zeroes. See the state.
                        counts = if (met.isEmpty() && practice.value.lessonsCompleted.isEmpty()) {
                            null
                        } else {
                            OverviewCounts(
                                wordsMet = met.size,
                                lessonsFinished = practice.value.lessonsCompleted.size,
                                wordsNeedingReview = met.values.count { it.reviewNeeded }
                            )
                        },
                        // The word as the child saw it on the card. An id is a key, not a word.
                        recentWords = met.keys.mapNotNull { labels[it] },
                        pendingSave = practice.value.openCheckpoint != null,
                        unreadable = false
                    )
                }
        }
    }

    private fun labelsIn(
        course: com.nphkhiem.englishforyourchildren.domain.model.Course
    ): Map<com.nphkhiem.englishforyourchildren.domain.model.SkillId, String> = course.units
        .flatMap { it.lessons }
        .flatMap { it.activities }
        .mapNotNull { it.content }
        .flatMap { content ->
            (content as? com.nphkhiem.englishforyourchildren.domain.model.Answerable)
                ?.choices.orEmpty()
        }
        .associate { it.skillId to it.label }
}
