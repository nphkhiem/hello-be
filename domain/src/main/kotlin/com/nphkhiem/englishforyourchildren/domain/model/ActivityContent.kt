package com.nphkhiem.englishforyourchildren.domain.model

/** One thing a child can press, and everything needed to show it. */
data class AnswerChoice(
    val skillId: SkillId,
    val label: String,
    val image: AssetId,
    val audio: AssetId
) {
    init {
        require(label.isNotBlank()) { "A choice a child cannot read or hear named is not a choice" }
    }
}

/**
 * An activity that can be got right.
 *
 * Four of the five families are answerable. Speaking practice is not, and it says so by not being
 * this: there is no field to leave null and no branch to forget, so scoring a three-year-old on
 * their pronunciation would take somebody deciding to add an interface, not omitting a check.
 */
sealed interface Answerable {
    val choices: List<AnswerChoice>
    val correct: SkillId
}

/**
 * What an activity actually asks.
 *
 * The payload the domain went without until the curriculum blueprint existed. It carries no strings
 * a screen has to interpret and no layout: a prompt to say, things to choose between, and which one
 * is right where that means anything.
 *
 * [promptAsset] is nullable because no recording has been made yet. Absent audio is a state this app
 * was designed for rather than an error, and this is that same truth one layer below the screen.
 */
sealed interface ActivityContent {
    val prompt: String
    val promptAsset: AssetId?

    /** Hear a word, choose the picture that matches it. */
    data class ListeningSelection(
        override val prompt: String,
        override val promptAsset: AssetId?,
        override val choices: List<AnswerChoice>,
        override val correct: SkillId
    ) : ActivityContent,
        Answerable {
        init {
            requireAnswerable(prompt, choices, correct)
        }
    }

    /** Find the named picture among several. */
    data class PictureMatching(
        override val prompt: String,
        override val promptAsset: AssetId?,
        override val choices: List<AnswerChoice>,
        override val correct: SkillId
    ) : ActivityContent,
        Answerable {
        init {
            requireAnswerable(prompt, choices, correct)
        }
    }

    /**
     * Which of these starts with this letter.
     *
     * The letter is always one inside a word the child is meeting that day. A letter met on its own
     * is the thing the progression rule exists to prevent.
     */
    data class LetterAndSound(
        override val prompt: String,
        override val promptAsset: AssetId?,
        override val choices: List<AnswerChoice>,
        override val correct: SkillId,
        val letter: SkillId,
        val letterAsset: AssetId?
    ) : ActivityContent,
        Answerable {
        init {
            requireAnswerable(prompt, choices, correct)
        }
    }

    /** Say it with Pip. Modelled, unscored, and with nothing to be wrong about. */
    data class GuidedRepetition(
        override val prompt: String,
        override val promptAsset: AssetId?,
        val words: List<AnswerChoice>
    ) : ActivityContent {
        init {
            require(prompt.isNotBlank()) { "An activity that asks nothing is not an activity" }
            require(words.isNotEmpty()) { "Nothing to say with Pip" }
        }
    }

    /** Recall of something already met. No new material. */
    data class ReviewQuestion(
        override val prompt: String,
        override val promptAsset: AssetId?,
        override val choices: List<AnswerChoice>,
        override val correct: SkillId
    ) : ActivityContent,
        Answerable {
        init {
            requireAnswerable(prompt, choices, correct)
        }
    }
}

/** Which family a piece of content belongs to, so an activity and its content cannot disagree. */
val ActivityContent.family: ActivityFamily
    get() = when (this) {
        is ActivityContent.ListeningSelection -> ActivityFamily.LISTEN_AND_CHOOSE
        is ActivityContent.PictureMatching -> ActivityFamily.PICTURE_MATCHING
        is ActivityContent.LetterAndSound -> ActivityFamily.LETTER_AND_SOUND
        is ActivityContent.GuidedRepetition -> ActivityFamily.SAY_WITH_PIP
        is ActivityContent.ReviewQuestion -> ActivityFamily.REVIEW
    }

private fun requireAnswerable(prompt: String, choices: List<AnswerChoice>, correct: SkillId) {
    require(prompt.isNotBlank()) { "An activity that asks nothing is not an activity" }
    require(choices.isNotEmpty()) {
        "A question with nothing to press is a stage a child is stuck on"
    }
    require(choices.map { it.skillId }.toSet().size == choices.size) {
        "The same word is offered twice"
    }
    require(choices.any { it.skillId == correct }) {
        "The correct answer ${correct.value} is not one of the choices"
    }
}
