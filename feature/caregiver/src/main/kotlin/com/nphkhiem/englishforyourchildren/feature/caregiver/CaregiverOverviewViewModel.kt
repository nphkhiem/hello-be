package com.nphkhiem.englishforyourchildren.feature.caregiver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nphkhiem.englishforyourchildren.domain.model.ActivityAttempt
import com.nphkhiem.englishforyourchildren.domain.model.Answerable
import com.nphkhiem.englishforyourchildren.domain.model.CoPlayIdea
import com.nphkhiem.englishforyourchildren.domain.model.Course
import com.nphkhiem.englishforyourchildren.domain.model.CourseUnit
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import com.nphkhiem.englishforyourchildren.domain.model.SkillId
import com.nphkhiem.englishforyourchildren.domain.model.SkillProgress
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
    /**
     * The one thing to try away from the television, as the content author wrote it.
     *
     * Both languages, not one: which of them a caregiver reads is settled where the screen is
     * drawn, because that is where the caregiver language is provided.
     */
    val suggestion: CoPlayIdea?,
    /**
     * The theme of the unit worth going back to, or null when nothing is.
     *
     * A name rather than a count, which is why the summary that draws it takes a string. Null when
     * no word is waiting for another go: a practice suggestion offered to a child with nothing to
     * practise would be a deficit invented to fill a card.
     */
    val unitToPractise: String?,
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
            suggestion = null,
            unitToPractise = null,
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
                        suggestion = suggestionAfter(course.value, practice.value.attempts),
                        unitToPractise = unitWorthAnotherGo(course.value, met),
                        pendingSave = practice.value.openCheckpoint != null,
                        unreadable = false
                    )
                }
        }
    }

    /**
     * The idea belonging to whatever the child was doing most recently.
     *
     * Attempts rather than finished lessons, because the finished ones are a set with no order to
     * them and a child who stopped halfway through a lesson still deserves the idea that goes with
     * it. Null where they have never practised, where the lesson has no idea written for it, or
     * where the activity is not in the course any more, all of which are the same thing to a
     * caregiver: there is nothing to suggest today.
     */
    private fun suggestionAfter(course: Course, attempts: List<ActivityAttempt>): CoPlayIdea? {
        val latest = attempts.maxByOrNull { it.at.value }?.activityId ?: return null
        return course.units
            .flatMap { it.lessons }
            .firstOrNull { lesson -> lesson.activities.any { it.id == latest } }
            ?.coPlay
    }

    /**
     * The unit holding the most words that wanted another go.
     *
     * The one distinction this summary is allowed to draw, followed through: not that a child was
     * wrong, but that these words are worth coming back to, and here is where they live. Ties go to
     * the unit that comes first in the course, so the same history always names the same unit.
     */
    private fun unitWorthAnotherGo(course: Course, met: Map<SkillId, SkillProgress>): String? {
        val waiting = met.filterValues { it.reviewNeeded }.keys
        if (waiting.isEmpty()) return null

        return course.units
            .maxByOrNull { unit -> unit.skills().count { it in waiting } }
            ?.takeIf { unit -> unit.skills().any { it in waiting } }
            ?.theme
    }

    /** Every word a unit is about, which is what its lessons say they teach. */
    private fun CourseUnit.skills(): List<SkillId> = lessons.flatMap { it.teaches }

    private fun labelsIn(course: Course): Map<SkillId, String> = course.units
        .flatMap { it.lessons }
        .flatMap { it.activities }
        .mapNotNull { it.content }
        .flatMap { content ->
            (content as? Answerable)?.choices.orEmpty()
        }
        .associate { it.skillId to it.label }
}
