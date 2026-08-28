package com.nphkhiem.englishforyourchildren.feature.learning

/**
 * Learning path states, shared by the debug catalog and the instrumented tests.
 *
 * Debug sources only, because the execution contract forbids production placeholder data.
 */
object LearningPathFixtures {

    fun midUnit(): LearningPathUiState = LearningPathUiState(
        profileName = PROFILE,
        profileAvatar = "M",
        unit = UnitPageState(
            unitId = "u2",
            unitNumber = 2,
            unitCount = UNIT_COUNT,
            theme = THEME,
            objective = OBJECTIVE,
            lessons = listOf(
                lesson(EYES, LessonProgress.COMPLETED),
                lesson(NOSE, LessonProgress.COMPLETED),
                lesson(HANDS, LessonProgress.RECOMMENDED),
                lesson(MOVE, LessonProgress.FUTURE),
                lesson(REVIEW, LessonProgress.FUTURE, LessonKind.REVIEW)
            )
        ),
        previousUnit = UnitSummary(unitId = "u1", unitNumber = 1, theme = "My Home"),
        nextUnit = UnitSummary(unitId = "u3", unitNumber = 3, theme = "My Toys"),
        pendingSave = false
    )

    /** The first unit, where there is no unit behind and the previous control must not appear. */
    fun firstUnit(): LearningPathUiState = midUnit().copy(
        unit = midUnit().unit?.copy(unitId = "u1", unitNumber = 1, theme = "My Home"),
        previousUnit = null,
        nextUnit = UnitSummary(unitId = "u2", unitNumber = 2, theme = THEME)
    )

    /** The last unit, where there is nothing ahead. */
    fun lastUnit(): LearningPathUiState = midUnit().copy(
        unit = midUnit().unit?.copy(unitId = "u12", unitNumber = UNIT_COUNT, theme = "My Day"),
        previousUnit = UnitSummary(unitId = "u11", unitNumber = 11, theme = "My Garden"),
        nextUnit = null
    )

    /** Every lesson finished, so nothing is recommended and the whole unit is history. */
    fun unitFinished(): LearningPathUiState = midUnit().copy(
        unit = midUnit().unit?.copy(
            lessons = listOf(
                lesson(EYES, LessonProgress.COMPLETED),
                lesson(NOSE, LessonProgress.COMPLETED),
                lesson(HANDS, LessonProgress.COMPLETED),
                lesson(MOVE, LessonProgress.COMPLETED),
                lesson(REVIEW, LessonProgress.COMPLETED, LessonKind.REVIEW)
            )
        )
    )

    /** A lesson that exists but will not load, stated rather than hidden. */
    fun lessonWillNotOpen(): LearningPathUiState = midUnit().copy(
        unit = midUnit().unit?.copy(
            lessons = listOf(
                lesson(EYES, LessonProgress.COMPLETED),
                lesson(HANDS, LessonProgress.RECOMMENDED, openable = false),
                lesson(MOVE, LessonProgress.FUTURE)
            )
        )
    )

    /** Nothing in the unit is reachable yet, so entry focus has to fall to the stepper. */
    fun nothingReachable(): LearningPathUiState = midUnit().copy(
        unit = midUnit().unit?.copy(
            lessons = listOf(
                lesson(MOVE, LessonProgress.FUTURE),
                lesson(REVIEW, LessonProgress.FUTURE, LessonKind.REVIEW)
            )
        )
    )

    fun pendingSave(): LearningPathUiState = midUnit().copy(pendingSave = true)

    /** The unit will not load at all. */
    fun recovering(): LearningPathUiState = midUnit().copy(unit = null)

    /** A unit that loaded but carries nothing, which is the same dead end by another route. */
    fun emptyUnit(): LearningPathUiState =
        midUnit().copy(unit = midUnit().unit?.copy(lessons = emptyList()))

    /** Every approved path state, in the order a reviewer would walk them. */
    fun reviewStates(): List<Pair<String, LearningPathUiState>> = listOf(
        "part way through a unit" to midUnit(),
        "first unit" to firstUnit(),
        "last unit" to lastUnit(),
        "unit finished" to unitFinished(),
        "a lesson will not open" to lessonWillNotOpen(),
        "nothing reachable yet" to nothingReachable(),
        "progress pending" to pendingSave(),
        "unit will not load" to recovering(),
        "unit is empty" to emptyUnit()
    )

    private fun lesson(
        title: String,
        progress: LessonProgress,
        kind: LessonKind = LessonKind.PRACTICE,
        openable: Boolean = true
    ): LessonNodeState = LessonNodeState(
        id = title.lowercase().replace(' ', '-'),
        title = title,
        progress = progress,
        kind = kind,
        openable = openable
    )

    const val PROFILE = "Minh"
    const val THEME = "My Body"
    const val OBJECTIVE = "Five little adventures"
    const val UNIT_COUNT = 12
    const val EYES = "Eyes and ears"
    const val NOSE = "Nose and mouth"
    const val HANDS = "Hands and feet"
    const val MOVE = "Move with me"
    const val REVIEW = "Review story"
}
