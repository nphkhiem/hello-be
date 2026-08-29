package com.nphkhiem.englishforyourchildren.feature.caregiver

/**
 * Caregiver states, shared by the debug catalog and the instrumented tests.
 *
 * Debug sources only, because the execution contract forbids production placeholder data.
 */
object CaregiverFixtures {

    /** The approved draft's own challenge: the correct answer sits in the middle. */
    fun gate(): AdultGateUiState = AdultGateUiState(
        challenge = GateChallenge(
            question = QUESTION,
            answers = listOf(WRONG_LOW, CORRECT, WRONG_HIGH),
            correctIndex = 1
        ),
        previousAnswerWasWrong = false
    )

    /** The correct answer first, which is the case focus must step past. */
    fun gateCorrectFirst(): AdultGateUiState = gate().copy(
        challenge = GateChallenge(
            question = QUESTION,
            answers = listOf(CORRECT, WRONG_LOW, WRONG_HIGH),
            correctIndex = 0
        )
    )

    /** After a wrong answer the host rotates the challenge and says so neutrally. */
    fun gateAfterWrongAnswer(): AdultGateUiState = AdultGateUiState(
        challenge = GateChallenge(
            question = SECOND_QUESTION,
            answers = listOf(SECOND_WRONG, SECOND_CORRECT, SECOND_WRONG_HIGH),
            correctIndex = 1
        ),
        previousAnswerWasWrong = true
    )

    /** A challenge that cannot offer a wrong answer to stand on. It must fail closed. */
    fun gateUnusable(): AdultGateUiState = gate().copy(
        challenge = GateChallenge(
            question = QUESTION,
            answers = listOf(CORRECT),
            correctIndex = 0
        )
    )

    fun shell(section: CaregiverSection = CaregiverSection.OVERVIEW): CaregiverShellState =
        CaregiverShellState(profileName = PROFILE, section = section)

    /** A week with practice in it: the draft's own summaries and words. */
    fun overview(): CaregiverOverviewUiState = CaregiverOverviewUiState(
        profileName = PROFILE,
        period = PERIOD,
        progress = OverviewProgress.Practiced(
            summaries = listOf(
                OverviewSummary(
                    label = "Adventures finished",
                    value = "3",
                    note = "Sessions completed"
                ),
                OverviewSummary(
                    label = "Words encountered",
                    value = "18",
                    note = NOT_A_SCORE
                ),
                OverviewSummary(
                    label = "Practice suggestion",
                    value = "My Home",
                    note = "Repeat familiar words"
                )
            ),
            recentWords = RECENT_WORDS
        ),
        suggestion = CoPlaySuggestion(title = SUGGESTION, instruction = SUGGESTION_HINT),
        pendingSave = false
    )

    /** More history than the panel may draw, which is what the bound exists for. */
    fun overviewLongHistory(): CaregiverOverviewUiState = overview().copy(
        progress = OverviewProgress.Practiced(
            summaries = (overview().progress as OverviewProgress.Practiced).summaries +
                OverviewSummary(label = "A fourth", value = "9", note = "Should not appear"),
            recentWords = RECENT_WORDS + OVERFLOW_WORDS
        )
    )

    fun overviewNewProfile(): CaregiverOverviewUiState =
        overview().copy(progress = OverviewProgress.NewProfile)

    fun overviewNothingRecent(): CaregiverOverviewUiState =
        overview().copy(progress = OverviewProgress.NothingRecent)

    fun overviewPendingSave(): CaregiverOverviewUiState = overview().copy(pendingSave = true)

    /** No suggestion can be offered, which is the brief's unavailable-content state. */
    fun overviewNoSuggestion(): CaregiverOverviewUiState = overview().copy(suggestion = null)

    /** Vietnamese copy at its longest, to check the panels hold bilingual text. */
    fun overviewLongCopy(): CaregiverOverviewUiState = overview().copy(
        profileName = LONG_PROFILE,
        suggestion = CoPlaySuggestion(title = LONG_SUGGESTION, instruction = LONG_HINT)
    )

    fun overviewStates(): List<Pair<String, CaregiverOverviewUiState>> = listOf(
        "active learner" to overview(),
        "more history than fits" to overviewLongHistory(),
        "new profile" to overviewNewProfile(),
        "nothing this week" to overviewNothingRecent(),
        "progress pending" to overviewPendingSave(),
        "no suggestion available" to overviewNoSuggestion(),
        "long bilingual copy" to overviewLongCopy()
    )

    fun gateStates(): List<Pair<String, AdultGateUiState>> = listOf(
        "initial" to gate(),
        "correct answer first" to gateCorrectFirst(),
        "after a wrong answer" to gateAfterWrongAnswer(),
        "challenge not usable" to gateUnusable()
    )

    fun shellStates(): List<Pair<String, CaregiverShellState>> = listOf(
        "overview" to shell(CaregiverSection.OVERVIEW),
        "settings" to shell(CaregiverSection.SETTINGS),
        "profiles" to shell(CaregiverSection.PROFILES)
    )

    const val PROFILE = "Minh"
    const val PERIOD = "This week"
    const val NOT_A_SCORE = "Not a test score"
    const val SUGGESTION = "Find a chair"
    const val SUGGESTION_HINT = "Point together and say chair."
    const val LONG_PROFILE = "Nguyễn Hoàng Phương"
    const val LONG_SUGGESTION = "Cùng nhau tìm một chiếc ghế trong nhà"
    const val LONG_HINT =
        "Chỉ vào chiếc ghế và cùng nói \u201cchair\u201d. Point together and say chair."
    const val OVERFLOWED_WORD = "spoon"
    val RECENT_WORDS = listOf("eyes", "hands", "mama", "chair", "bed", "lamp")
    val OVERFLOW_WORDS = listOf(OVERFLOWED_WORD, "window", "rug")
    const val QUESTION = "What is 7 + 4?"
    const val WRONG_LOW = "10"
    const val CORRECT = "11"
    const val WRONG_HIGH = "12"
    const val SECOND_QUESTION = "What is 5 + 6?"
    const val SECOND_WRONG = "9"
    const val SECOND_CORRECT = "11"
    const val SECOND_WRONG_HIGH = "13"
}
