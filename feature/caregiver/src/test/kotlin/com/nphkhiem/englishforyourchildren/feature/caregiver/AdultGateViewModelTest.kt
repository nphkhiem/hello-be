package com.nphkhiem.englishforyourchildren.feature.caregiver

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

/**
 * The door itself.
 *
 * Every challenge here comes from a source that was told what to say, so nothing in this file
 * depends on which numbers a random generator happened to pick. `GateChallengesTest` is where the
 * real source is held to its promises.
 */
class AdultGateViewModelTest {
    private val challenges = ScriptedChallenges()

    @AfterEach
    fun releaseMain() {
        Dispatchers.resetMain()
    }

    @Test
    fun givenTheGateOpens_whenItIsFirstShown_thenItIsAlreadyAskingSomething() = gate { model ->
        assertThat(model.state.value.arithmetic).isEqualTo(challenges.script[0])
        assertThat(model.state.value.previousAnswerWasWrong).isFalse()
        assertThat(model.state.value.opened).isFalse()
    }

    @Test
    fun givenTheRightAnswer_whenItIsPressed_thenTheDoorOpens() = gate { model ->
        val correct = model.state.value.arithmetic.correctIndex

        model.onAction(AdultGateAction.AnswerChosen(correct))

        assertThat(model.state.value.opened).isTrue()
    }

    @Test
    fun givenAWrongAnswer_whenItIsPressed_thenADifferentQuestionIsAsked() = gate { model ->
        // Rotating is what makes walking the row worthless: the next challenge puts the correct
        // answer somewhere else, and entry focus lands off it again.
        val wrong = wrongIndex(model)

        model.onAction(AdultGateAction.AnswerChosen(wrong))

        assertThat(model.state.value.arithmetic).isEqualTo(challenges.script[1])
        assertThat(model.state.value.previousAnswerWasWrong).isTrue()
        assertThat(model.state.value.opened).isFalse()
    }

    @Test
    fun givenAWrongAnswer_whenAnotherIsPressed_thenTheQuestionRotatesAgain() = gate { model ->
        model.onAction(AdultGateAction.AnswerChosen(wrongIndex(model)))

        model.onAction(AdultGateAction.AnswerChosen(wrongIndex(model)))

        assertThat(model.state.value.arithmetic).isEqualTo(challenges.script[2])
    }

    @Test
    fun givenNobodyTouchesIt_whenHalfAMinutePasses_thenItAsksSomethingElse() = gate { model ->
        // A caregiver who paused to think must not be thrown back to the child surface. The
        // question goes stale, not the person.
        advanceTimeBy(UNTOUCHED_MILLIS + 1)

        assertThat(model.state.value.arithmetic).isEqualTo(challenges.script[1])
        assertThat(model.state.value.opened).isFalse()
    }

    @Test
    fun givenAWrongAnswerThenAPause_whenTheQuestionGoesStale_thenTheNoticeGoesToo() =
        gate { model ->
            // The notice belongs to the question that was got wrong. Carrying it onto a question
            // nobody has answered yet would tell a caregiver they had failed at something they had
            // not been asked.
            model.onAction(AdultGateAction.AnswerChosen(wrongIndex(model)))
            assertThat(model.state.value.previousAnswerWasWrong).isTrue()

            advanceTimeBy(UNTOUCHED_MILLIS + 1)

            assertThat(model.state.value.previousAnswerWasWrong).isFalse()
        }

    @Test
    fun givenSomebodyIsAnswering_whenTheyKeepPressing_thenTheQuestionStays() = gate { model ->
        // Answering is touching it. A caregiver working through the row should never have the
        // question change mid-press because a timer that started on arrival ran out.
        advanceTimeBy(UNTOUCHED_MILLIS - 1)
        model.onAction(AdultGateAction.AnswerChosen(wrongIndex(model)))
        val afterAnswering = model.state.value.arithmetic

        advanceTimeBy(UNTOUCHED_MILLIS - 1)

        assertThat(model.state.value.arithmetic).isEqualTo(afterAnswering)
    }

    @Test
    fun givenTheDoorIsOpen_whenTimePasses_thenTheQuestionBehindItIsLeftAlone() = gate { model ->
        // Nothing should still be rotating a challenge nobody is looking at.
        model.onAction(AdultGateAction.AnswerChosen(model.state.value.arithmetic.correctIndex))
        val whenOpened = model.state.value.arithmetic

        advanceTimeBy(UNTOUCHED_MILLIS * 3)

        assertThat(model.state.value.arithmetic).isEqualTo(whenOpened)
        assertThat(model.state.value.opened).isTrue()
    }

    private fun wrongIndex(model: AdultGateViewModel): Int =
        model.state.value.arithmetic.answers.indices
            .first { it != model.state.value.arithmetic.correctIndex }

    /**
     * A gate on a clock a test controls, torn down whatever the body does.
     *
     * Cleared through a [ViewModelStore] rather than simply dropped, because the gate keeps a timer
     * running against that clock for as long as it lives. Leaving it alive would have `runTest`
     * advancing virtual time against a delay that reschedules itself forever, which hangs the run
     * rather than failing it.
     */
    private fun gate(body: suspend TestScope.(AdultGateViewModel) -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        val model = ViewModelProvider(
            store,
            viewModelFactory { initializer { AdultGateViewModel(challenges) } }
        )[AdultGateViewModel::class.java]

        try {
            body(model)
        } finally {
            store.clear()
        }
    }

    /** Says exactly what it was told to, in order, so "a different question" is a fact. */
    private class ScriptedChallenges : GateChallenges {
        val script = List(SCRIPTED) { index ->
            GateArithmetic(
                left = 10 + index,
                right = 20 + index,
                answers = listOf(30 + index * 2, 31 + index * 2, 32 + index * 2),
                correctIndex = index % 3
            )
        }

        private var asked = 0

        override fun next(): GateArithmetic = script[asked++ % script.size]
    }

    private companion object {
        const val UNTOUCHED_MILLIS = 30_000L
        const val SCRIPTED = 12
    }
}
