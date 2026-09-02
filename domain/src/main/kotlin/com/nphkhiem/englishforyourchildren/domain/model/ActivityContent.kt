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

/**
 * The recordings that ask this activity's question, in the order they are spoken.
 *
 * A prompt recording is a stem rather than a sentence. "Where is the" is one file, reused by every
 * question that opens that way, and what names the target follows it. That is the only arrangement
 * in which the four prompt recordings the registry allows can cover the ten distinct sentences unit
 * one asks, and it is what gives the word recordings a job: the same `aud-en-nose` a choice is
 * named by is the one that completes the question about it.
 *
 * What names the target is the last thing heard, so it is the clip whose ending means the question
 * has been asked. See ADR 0004, which is what that matters for.
 *
 * Empty when any part of it is unrecorded, because half a question is worse than a silent one. A
 * child who hears "Where is the" and then nothing has been asked to guess, and the audio-unavailable
 * path exists precisely so that they are not.
 */
val ActivityContent.spokenPrompt: List<AssetId>
    get() {
        val stem = promptAsset ?: return emptyList()
        val target = spokenTarget ?: return emptyList()
        return listOf(stem, target)
    }

/**
 * The recording that names what this activity is asking about.
 *
 * A letter activity names a letter, not a word: "which one starts with" is completed by the letter
 * itself, and the words are what the child chooses between rather than what is being asked for.
 *
 * Guided repetition models the first of its words, which is the one its prompt names. The rest are
 * there for the child to go on with once Pip has shown them how.
 */
private val ActivityContent.spokenTarget: AssetId?
    get() = when (this) {
        is ActivityContent.LetterAndSound -> letterAsset
        is ActivityContent.GuidedRepetition -> words.first().audio
        is ActivityContent.ListeningSelection -> correctChoice().audio
        is ActivityContent.PictureMatching -> correctChoice().audio
        is ActivityContent.ReviewQuestion -> correctChoice().audio
    }

/**
 * The choice that is right, which the model already guarantees is there.
 *
 * `requireAnswerable` refuses to build content whose correct answer is not among its choices, so
 * this cannot fail. Returning a nullable here would invent a case the constructor has already made
 * impossible, and every caller would have to carry it.
 */
private fun Answerable.correctChoice(): AnswerChoice = choices.first { it.skillId == correct }

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
