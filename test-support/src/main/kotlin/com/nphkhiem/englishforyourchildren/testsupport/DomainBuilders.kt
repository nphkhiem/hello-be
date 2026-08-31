package com.nphkhiem.englishforyourchildren.testsupport

import com.nphkhiem.englishforyourchildren.domain.model.Activity
import com.nphkhiem.englishforyourchildren.domain.model.ActivityAttempt
import com.nphkhiem.englishforyourchildren.domain.model.ActivityFamily
import com.nphkhiem.englishforyourchildren.domain.model.ActivityId
import com.nphkhiem.englishforyourchildren.domain.model.ActivityInstanceId
import com.nphkhiem.englishforyourchildren.domain.model.AgeBand
import com.nphkhiem.englishforyourchildren.domain.model.AppSettings
import com.nphkhiem.englishforyourchildren.domain.model.AttemptOutcome
import com.nphkhiem.englishforyourchildren.domain.model.AvatarId
import com.nphkhiem.englishforyourchildren.domain.model.ChildProfile
import com.nphkhiem.englishforyourchildren.domain.model.Course
import com.nphkhiem.englishforyourchildren.domain.model.CourseId
import com.nphkhiem.englishforyourchildren.domain.model.CourseUnit
import com.nphkhiem.englishforyourchildren.domain.model.CourseVersion
import com.nphkhiem.englishforyourchildren.domain.model.EpochMillis
import com.nphkhiem.englishforyourchildren.domain.model.Lesson
import com.nphkhiem.englishforyourchildren.domain.model.LessonCheckpoint
import com.nphkhiem.englishforyourchildren.domain.model.LessonId
import com.nphkhiem.englishforyourchildren.domain.model.LessonSession
import com.nphkhiem.englishforyourchildren.domain.model.LessonStatus
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import com.nphkhiem.englishforyourchildren.domain.model.ProfileProgress
import com.nphkhiem.englishforyourchildren.domain.model.SessionId
import com.nphkhiem.englishforyourchildren.domain.model.SkillId
import com.nphkhiem.englishforyourchildren.domain.model.SkillProgress
import com.nphkhiem.englishforyourchildren.domain.model.UnitId
import com.nphkhiem.englishforyourchildren.domain.repository.CreateProfile
import com.nphkhiem.englishforyourchildren.domain.repository.PersistCheckpoint

/**
 * Valid domain values with one thing changed.
 *
 * Every default here satisfies the model's own invariants, so a test that cares about one field
 * says only that field and the rest is guaranteed to be legal. The defaults deliberately match the
 * approved starter curriculum: `u01-my-body-l1` is a real lesson id from `CONTENT_ID_REGISTRY.md`,
 * not a placeholder, so a fixture reads like the thing it stands for.
 *
 * These are builders and not fakes: they make values, they hold no state, and they decide nothing.
 */
object DomainBuilders {
    fun childProfile(
        id: ProfileId = ProfileId("p1"),
        nickname: String = "Minh",
        ageBand: AgeBand = AgeBand.THREE,
        avatarId: AvatarId = AvatarId("rabbit")
    ) = ChildProfile(id = id, nickname = nickname, ageBand = ageBand, avatarId = avatarId)

    fun createProfile(
        nickname: String = "Minh",
        ageBand: AgeBand = AgeBand.THREE,
        avatarId: AvatarId = AvatarId("rabbit")
    ) = CreateProfile(nickname = nickname, ageBand = ageBand, avatarId = avatarId)

    fun activity(
        id: ActivityId = ActivityId("u01-my-body-l1-a1"),
        ordinal: Int = 0,
        family: ActivityFamily = ActivityFamily.LISTEN_AND_CHOOSE
    ) = Activity(id = id, ordinal = ordinal, family = family)

    /** Six activities in the approved spine order, which is what a real teaching lesson holds. */
    fun spine(lessonId: String = "u01-my-body-l1"): List<Activity> = listOf(
        ActivityFamily.LISTEN_AND_CHOOSE,
        ActivityFamily.LISTEN_AND_CHOOSE,
        ActivityFamily.PICTURE_MATCHING,
        ActivityFamily.LETTER_AND_SOUND,
        ActivityFamily.SAY_WITH_PIP,
        ActivityFamily.REVIEW
    ).mapIndexed { index, family ->
        activity(id = ActivityId("$lessonId-a${index + 1}"), ordinal = index, family = family)
    }

    fun lesson(
        id: LessonId = LessonId("u01-my-body-l1"),
        unitId: UnitId = UnitId("u01-my-body"),
        ordinal: Int = 0,
        activities: List<Activity> = spine()
    ) = Lesson(id = id, unitId = unitId, ordinal = ordinal, activities = activities)

    fun courseUnit(
        id: UnitId = UnitId("u01-my-body"),
        courseId: CourseId = CourseId("starter"),
        ordinal: Int = 0,
        theme: String = "My Body",
        word: String = "body",
        lessons: List<Lesson> = listOf(lesson())
    ) = CourseUnit(
        id = id,
        courseId = courseId,
        ordinal = ordinal,
        theme = theme,
        word = word,
        lessons = lessons
    )

    fun course(
        id: CourseId = CourseId("starter"),
        version: CourseVersion = CourseVersion("2026.09"),
        schemaVersion: Int = 1,
        supportedLocales: Set<String> = setOf("en", "vi"),
        units: List<CourseUnit> = listOf(courseUnit())
    ) = Course(
        id = id,
        version = version,
        schemaVersion = schemaVersion,
        supportedLocales = supportedLocales,
        units = units
    )

    fun lessonSession(
        id: SessionId = SessionId("s1"),
        profileId: ProfileId = ProfileId("p1"),
        courseVersion: CourseVersion = CourseVersion("2026.09"),
        lessonId: LessonId = LessonId("u01-my-body-l1"),
        currentActivity: ActivityInstanceId? = ActivityInstanceId("u01-my-body-l1-a1-1"),
        status: LessonStatus = LessonStatus.IN_PROGRESS
    ) = LessonSession(
        id = id,
        profileId = profileId,
        courseVersion = courseVersion,
        lessonId = lessonId,
        currentActivity = currentActivity,
        status = status
    )

    fun lessonCheckpoint(
        profileId: ProfileId = ProfileId("p1"),
        courseVersion: CourseVersion = CourseVersion("2026.09"),
        lessonId: LessonId = LessonId("u01-my-body-l1"),
        lastCompletedActivity: ActivityId? = ActivityId("u01-my-body-l1-a1"),
        sessionId: SessionId = SessionId("s1"),
        updatedAt: EpochMillis = EpochMillis(NOW)
    ) = LessonCheckpoint(
        profileId = profileId,
        courseVersion = courseVersion,
        lessonId = lessonId,
        lastCompletedActivity = lastCompletedActivity,
        sessionId = sessionId,
        updatedAt = updatedAt
    )

    fun persistCheckpoint(
        sessionId: SessionId = SessionId("s1"),
        profileId: ProfileId = ProfileId("p1"),
        courseVersion: CourseVersion = CourseVersion("2026.09"),
        lessonId: LessonId = LessonId("u01-my-body-l1"),
        activityId: ActivityId = ActivityId("u01-my-body-l1-a1"),
        activityInstanceId: ActivityInstanceId = ActivityInstanceId("u01-my-body-l1-a1-1"),
        activityOrdinal: Int = 0,
        outcome: AttemptOutcome = AttemptOutcome.CORRECT,
        completedAt: EpochMillis = EpochMillis(NOW)
    ) = PersistCheckpoint(
        sessionId = sessionId,
        profileId = profileId,
        courseVersion = courseVersion,
        lessonId = lessonId,
        activityId = activityId,
        activityInstanceId = activityInstanceId,
        activityOrdinal = activityOrdinal,
        outcome = outcome,
        completedAt = completedAt
    )

    fun activityAttempt(
        sessionId: SessionId = SessionId("s1"),
        activityInstance: ActivityInstanceId = ActivityInstanceId("u01-my-body-l1-a1-1"),
        ordinal: Int = 0,
        outcome: AttemptOutcome = AttemptOutcome.CORRECT,
        at: EpochMillis = EpochMillis(NOW)
    ) = ActivityAttempt(
        sessionId = sessionId,
        activityInstance = activityInstance,
        ordinal = ordinal,
        outcome = outcome,
        at = at
    )

    fun skillProgress(
        skillId: SkillId = SkillId("word-eyes"),
        exposures: Int = 3,
        supportedSuccesses: Int = 2,
        reviewNeeded: Boolean = false,
        lastPractisedAt: EpochMillis? = EpochMillis(NOW)
    ) = SkillProgress(
        skillId = skillId,
        exposures = exposures,
        supportedSuccesses = supportedSuccesses,
        reviewNeeded = reviewNeeded,
        lastPractisedAt = lastPractisedAt
    )

    fun profileProgress(
        profileId: ProfileId = ProfileId("p1"),
        lessonsCompleted: Set<LessonId> = emptySet(),
        skills: List<SkillProgress> = emptyList(),
        openCheckpoint: LessonCheckpoint? = null
    ) = ProfileProgress(
        profileId = profileId,
        lessonsCompleted = lessonsCompleted,
        skills = skills,
        openCheckpoint = openCheckpoint
    )

    fun appSettings(
        selectedProfileId: ProfileId? = ProfileId("p1"),
        caregiverLocaleTag: String = "vi",
        vietnameseHelpEnabled: Boolean = true,
        captionsEnabled: Boolean = true,
        reducedMotionEnabled: Boolean = false,
        highContrastEnabled: Boolean = false,
        backgroundMusicEnabled: Boolean = true
    ) = AppSettings(
        selectedProfileId = selectedProfileId,
        caregiverLocaleTag = caregiverLocaleTag,
        vietnameseHelpEnabled = vietnameseHelpEnabled,
        captionsEnabled = captionsEnabled,
        reducedMotionEnabled = reducedMotionEnabled,
        highContrastEnabled = highContrastEnabled,
        backgroundMusicEnabled = backgroundMusicEnabled
    )

    private const val NOW = 1_756_000_000_000
}
