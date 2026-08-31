package com.nphkhiem.englishforyourchildren.feature.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nphkhiem.englishforyourchildren.domain.model.Answerable
import com.nphkhiem.englishforyourchildren.domain.model.CourseVersion
import com.nphkhiem.englishforyourchildren.domain.model.Lesson
import com.nphkhiem.englishforyourchildren.domain.model.LessonId
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import com.nphkhiem.englishforyourchildren.domain.repository.CurriculumRepository
import com.nphkhiem.englishforyourchildren.domain.repository.ProgressRepository
import com.nphkhiem.englishforyourchildren.domain.result.DomainResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The storybook page at the end of a lesson.
 *
 * It reports what a child met rather than what they got right. The design brief describes progress
 * as "words encountered", and there is no score anywhere in this product, so a lesson walked
 * entirely on the unscored skip still ends on its words.
 *
 * There is no clock here. `revealed` is the host's to decide, per ADR 0003, because the reveal
 * budget is a motion token and belongs with the screen that owns motion.
 */
@HiltViewModel
class LessonCelebrationViewModel @Inject constructor(
    private val curriculum: CurriculumRepository,
    private val progress: ProgressRepository
) : ViewModel() {

    private val _state = MutableStateFlow(
        CelebrationUiState(
            unitWord = "",
            words = emptyList(),
            revealed = false,
            saveConfirmed = false
        )
    )
    val state: StateFlow<CelebrationUiState> = _state.asStateFlow()

    suspend fun start(profileId: ProfileId, lessonId: LessonId, courseVersion: CourseVersion) {
        val lesson = curriculum.getLesson(lessonId, courseVersion)
        if (lesson !is DomainResult.Success) return

        val course = curriculum.observeCourse().first()
        val unit = (course as? DomainResult.Success)?.value?.units
            ?.firstOrNull { it.id == lesson.value.unitId }

        _state.update {
            it.copy(unitWord = unit?.word.orEmpty(), words = learnedWords(lesson.value))
        }

        watchTheWriteLand(profileId, lessonId)
    }

    /**
     * Nothing says the words are in the storybook until storage says the lesson is finished.
     *
     * Read back rather than assumed. The lesson screen asked for the write on its way out, and a
     * page that claimed it landed without checking is exactly the claim the brief forbids.
     */
    private fun watchTheWriteLand(profileId: ProfileId, lessonId: LessonId) {
        viewModelScope.launch {
            progress.observeProfileProgress(profileId).collect { stored ->
                val finished = (stored as? DomainResult.Success)
                    ?.value?.lessonsCompleted?.contains(lessonId) == true
                _state.update { it.copy(saveConfirmed = finished) }
            }
        }
    }

    /**
     * The words the lesson is for, labelled the way the child saw them on the cards.
     *
     * The label comes from the answers rather than from the id, because "word-eyes" is a key and
     * a child reads "eyes". A taught word that never appeared as a choice has no label to show and
     * is left out rather than named by its id.
     */
    private fun learnedWords(lesson: Lesson): List<LearnedWord> {
        val labels = lesson.activities
            .flatMap { (it.content as? Answerable)?.choices.orEmpty() }
            .associate { it.skillId to it.label }

        return lesson.teaches.mapNotNull { skill ->
            labels[skill]?.let { LearnedWord(id = skill.value, label = it) }
        }
    }
}
