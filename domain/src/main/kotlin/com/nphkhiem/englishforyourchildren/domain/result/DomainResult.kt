package com.nphkhiem.englishforyourchildren.domain.result

/**
 * What a repository gives back: the thing asked for, or a reason it could not be had.
 *
 * This is the boundary ADR 0011 deferred the result type to. Inside the domain, a value class that
 * cannot be blank throws, because a blank id there is a programmer error. Here, at the edge where
 * storage and packaged content live, failure is ordinary: a database may not open and a lesson may
 * not exist, and neither is anybody's mistake. So it is a value the caller has to handle rather
 * than an exception they may forget to catch.
 */
sealed interface DomainResult<out T> {
    data class Success<T>(val value: T) : DomainResult<T>

    data class Failure(val error: DomainError) : DomainResult<Nothing>
}

/**
 * The reasons a repository can fail.
 *
 * Deliberately few and deliberately closed. Each one maps to something a person is shown: the
 * recovery family for the unreadable and missing cases, the capacity state on profile management
 * for the full one. A reason with no screen behind it would be a reason nobody could act on.
 */
sealed interface DomainError {
    /** Four is the limit, and this television already knows four children. */
    data object ProfileLimitReached : DomainError

    /** The profile asked for is not there, which usually means it was deleted elsewhere. */
    data object ProfileNotFound : DomainError

    /** The lesson asked for is not in the packaged course at this version. */
    data object LessonNotFound : DomainError

    /** The packaged content is present but does not parse or does not validate. */
    data object InvalidContent : DomainError

    /** Storage will not open or will not answer. The caregiver recovery says so plainly. */
    data object PersistenceUnavailable : DomainError

    /** The audio for this moment cannot be played, which never blocks the lesson. */
    data object MediaUnavailable : DomainError
}
