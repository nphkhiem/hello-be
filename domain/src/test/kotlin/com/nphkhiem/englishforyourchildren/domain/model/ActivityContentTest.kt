package com.nphkhiem.englishforyourchildren.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ActivityContentTest {
    @Test
    fun givenAnActivityThatAsksNothing_whenItIsCreated_thenItIsRejected() {
        assertThrows<IllegalArgumentException> { listening(prompt = "") }
    }

    @Test
    fun givenNoChoices_whenAQuestionIsCreated_thenItIsRejected() {
        // A question with nothing to press is a stage a child is stuck on.
        assertThrows<IllegalArgumentException> { listening(choices = emptyList()) }
    }

    @Test
    fun givenTheSameWordOffatTwice_whenAQuestionIsCreated_thenItIsRejected() {
        assertThrows<IllegalArgumentException> {
            listening(choices = listOf(choice("eyes"), choice("eyes")))
        }
    }

    @Test
    fun givenAnAnswerThatIsNotOnOffer_whenAQuestionIsCreated_thenItIsRejected() {
        // A question no child can get right. The content validator refuses this in a bundle; the
        // domain refuses it for content built in code too, so the rule holds either way.
        assertThrows<IllegalArgumentException> {
            listening(choices = listOf(choice("eyes"), choice("ears")), correct = "nose")
        }
    }

    @Test
    fun givenAWellFormedQuestion_whenItIsCreated_thenItKeepsWhatItWasGiven() {
        val activity = listening()

        assertThat(activity.prompt).isEqualTo("Where are the eyes?")
        assertThat(
            activity.choices.map {
                it.skillId.value
            }
        ).containsExactly("word-eyes", "word-ears")
        assertThat(activity.correct).isEqualTo(SkillId("word-eyes"))
    }

    @Test
    fun givenSpeakingPractice_whenItIsRead_thenThereIsNowhereToPutAScore() {
        // Say with Pip is modelled and unscored. Not a nullable answer that happens to be null: the
        // variant does not implement the interface that has one, so a later change cannot quietly
        // start scoring a three-year-old on their pronunciation without someone deciding to.
        val speaking: ActivityContent = ActivityContent.GuidedRepetition(
            prompt = "Say it with me: eyes.",
            promptAsset = null,
            words = listOf(choice("eyes"))
        )

        assertThat(speaking).isNotInstanceOf(Answerable::class.java)
        assertThat(listening()).isInstanceOf(Answerable::class.java)
    }

    @Test
    fun givenALetterActivity_whenItIsRead_thenItNamesTheLetterItIsAbout() {
        val letters = ActivityContent.LetterAndSound(
            prompt = "Which one starts with E?",
            promptAsset = null,
            choices = listOf(choice("eyes"), choice("mouth")),
            correct = SkillId("word-eyes"),
            letter = SkillId("letter-e"),
            letterAsset = null
        )

        assertThat(letters.letter).isEqualTo(SkillId("letter-e"))
    }

    @Test
    fun givenNoRecordingHasBeenMade_whenAnActivityIsCreated_thenItIsStillAValidActivity() {
        // Every prompt recording is unmade. The app treats absent audio as a first-class state, and
        // this is the same truth one layer down: content without sound is content, not an error.
        val silent = listening(promptAsset = null)

        assertThat(silent.promptAsset).isNull()
    }

    @Test
    fun givenAnActivityWhoseFamilyDisagreesWithItsContent_whenItIsCreated_thenItIsRejected() {
        // Two ways of saying the same thing can disagree, so the one place they meet checks them.
        assertThrows<IllegalArgumentException> {
            Activity(
                id = ActivityId("u01-my-body-l1-a1"),
                ordinal = 0,
                family = ActivityFamily.PICTURE_MATCHING,
                content = listening()
            )
        }
    }

    @Test
    fun givenAnActivityWhoseFamilyMatches_whenItIsCreated_thenItIsAccepted() {
        val activity = Activity(
            id = ActivityId("u01-my-body-l1-a1"),
            ordinal = 0,
            family = ActivityFamily.LISTEN_AND_CHOOSE,
            content = listening()
        )

        assertThat(activity.content).isNotNull()
    }

    @Test
    fun givenAnActivityWithNoContentYet_whenItIsCreated_thenItIsStillAllowed() {
        // Structure without payload is how every activity existed before the blueprint, and how a
        // test that only cares about ordering still builds one.
        val structural = Activity(
            id = ActivityId("u01-my-body-l1-a1"),
            ordinal = 0,
            family = ActivityFamily.REVIEW
        )

        assertThat(structural.content).isNull()
    }

    @Test
    fun givenEveryFamily_whenContentIsAsked_thenThereIsOneKindForEachScreen() {
        assertThat(
            listOf(
                ActivityFamily.LISTEN_AND_CHOOSE,
                ActivityFamily.PICTURE_MATCHING,
                ActivityFamily.LETTER_AND_SOUND,
                ActivityFamily.SAY_WITH_PIP,
                ActivityFamily.REVIEW
            ).map { it.name }
        ).containsExactlyElementsIn(ActivityFamily.entries.map { it.name })
    }

    private fun listening(
        prompt: String = "Where are the eyes?",
        promptAsset: AssetId? = AssetId("aud-en-prompt-where-is"),
        choices: List<AnswerChoice> = listOf(choice("eyes"), choice("ears")),
        correct: String = "word-eyes"
    ) = ActivityContent.ListeningSelection(
        prompt = prompt,
        promptAsset = promptAsset,
        choices = choices,
        correct = SkillId(correct)
    )

    private fun choice(word: String) = AnswerChoice(
        skillId = SkillId("word-$word"),
        label = word,
        image = AssetId("img-$word"),
        audio = AssetId("aud-en-$word")
    )
}
