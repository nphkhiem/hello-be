package com.nphkhiem.englishforyourchildren.data.progress

import com.nphkhiem.englishforyourchildren.domain.model.ActivityAttempt
import com.nphkhiem.englishforyourchildren.domain.model.ActivityId
import com.nphkhiem.englishforyourchildren.domain.model.ActivityInstanceId
import com.nphkhiem.englishforyourchildren.domain.model.AttemptOutcome
import com.nphkhiem.englishforyourchildren.domain.model.EpochMillis
import com.nphkhiem.englishforyourchildren.domain.model.SessionId

/**
 * Stored rows as the domain sees them.
 *
 * An outcome that is not one of the four throws rather than being guessed at, and the repository's
 * existing `catch` turns that into a typed failure. A row nobody can read is a database that has
 * been written to by something this app does not understand, and quietly dropping it would leave a
 * child's history short by however many rows without anyone being told.
 */
internal fun ActivityAttemptEntity.toDomain() = ActivityAttempt(
    sessionId = SessionId(sessionId),
    activityId = ActivityId(activityId),
    activityInstance = ActivityInstanceId(activityInstanceId),
    ordinal = ordinal,
    outcome = AttemptOutcome.valueOf(outcome),
    at = EpochMillis(at)
)
