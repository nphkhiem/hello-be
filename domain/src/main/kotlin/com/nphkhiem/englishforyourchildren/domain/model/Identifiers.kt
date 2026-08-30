package com.nphkhiem.englishforyourchildren.domain.model

/**
 * The names this product uses for the things it can point at.
 *
 * Each one is a value class over an opaque value, so a function that wants a lesson cannot be handed
 * a profile, and neither can be handed a bare string that happens to look right. They validate in an
 * `init` block rather than behind a factory returning a result type: every value constructed today
 * is a literal or a copy of a known-good one, so a blank id here is a programmer error and is
 * reported as one. Untrusted input, when curriculum parsing and Room arrive, gets a parse helper at
 * that boundary with this check still underneath it. See ADR 0011.
 */
private fun requireIdentifier(value: String, name: String) {
    require(value.isNotBlank()) { "$name cannot be blank" }
}

/**
 * A profile's identity, and nothing about the child.
 *
 * A value class over an opaque id so a nickname, an age or an avatar can never travel in a
 * navigation key. Child data in keys is the thing to keep out, and a type that can only hold an id
 * is how that is held rather than remembered.
 */
@JvmInline
value class ProfileId(val value: String) {
    init {
        requireIdentifier(value, "ProfileId")
    }
}

/** Which picture a child chose for themselves. An id, never a file path. */
@JvmInline
value class AvatarId(val value: String) {
    init {
        requireIdentifier(value, "AvatarId")
    }
}

/** The course a child is working through. */
@JvmInline
value class CourseId(val value: String) {
    init {
        requireIdentifier(value, "CourseId")
    }
}

/**
 * Which published version of a course some progress belongs to.
 *
 * Progress records carry it so that a lesson finished under one version is never silently counted
 * against a different one.
 */
@JvmInline
value class CourseVersion(val value: String) {
    init {
        requireIdentifier(value, "CourseVersion")
    }
}

/** A themed group of lessons, such as My Home. */
@JvmInline
value class UnitId(val value: String) {
    init {
        requireIdentifier(value, "UnitId")
    }
}

/** One session of learning, made of activities in order. */
@JvmInline
value class LessonId(val value: String) {
    init {
        requireIdentifier(value, "LessonId")
    }
}

/** One activity as the curriculum defines it, the same for every child who reaches it. */
@JvmInline
value class ActivityId(val value: String) {
    init {
        requireIdentifier(value, "ActivityId")
    }
}

/**
 * One child's single encounter with an activity.
 *
 * Distinct from [ActivityId] because the same activity can be met more than once, in a review or on
 * a second attempt at a lesson, and an attempt has to say which encounter it belongs to.
 */
@JvmInline
value class ActivityInstanceId(val value: String) {
    init {
        requireIdentifier(value, "ActivityInstanceId")
    }
}

/** One sitting at a lesson, from starting it to finishing or leaving it. */
@JvmInline
value class SessionId(val value: String) {
    init {
        requireIdentifier(value, "SessionId")
    }
}

/** Something a child is learning, such as a letter sound or a word, tracked across lessons. */
@JvmInline
value class SkillId(val value: String) {
    init {
        requireIdentifier(value, "SkillId")
    }
}

/** A shelf in free play, holding the words a child has already met. */
@JvmInline
value class ShelfId(val value: String) {
    init {
        requireIdentifier(value, "ShelfId")
    }
}

/**
 * One packaged file: a picture, a recording, or a font.
 *
 * It arrives with the attribution ledger rather than with the curriculum models, because the first
 * thing that needed to name an asset was the record of who made it and under what licence.
 */
@JvmInline
value class AssetId(val value: String) {
    init {
        requireIdentifier(value, "AssetId")
    }
}

/**
 * An instant, as milliseconds since the epoch.
 *
 * Zero is a real instant and is accepted. A model that means "no time yet" says so with null, so
 * that no sentinel value has to be remembered by everyone who reads one.
 */
@JvmInline
value class EpochMillis(val value: Long) {
    init {
        require(value >= 0) { "EpochMillis cannot be negative" }
    }
}
