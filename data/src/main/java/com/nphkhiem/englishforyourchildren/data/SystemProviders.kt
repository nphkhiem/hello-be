package com.nphkhiem.englishforyourchildren.data

import com.nphkhiem.englishforyourchildren.domain.id.IdProvider
import com.nphkhiem.englishforyourchildren.domain.model.EpochMillis
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import com.nphkhiem.englishforyourchildren.domain.model.SessionId
import com.nphkhiem.englishforyourchildren.domain.time.TimeProvider
import java.util.UUID
import javax.inject.Inject

/** The real clock. Everything that records a time takes it from here rather than reading it. */
class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun now(): EpochMillis = EpochMillis(System.currentTimeMillis())
}

/** Real sitting identities. One per lesson a child opens, and never reused. */
class UuidSessionIdProvider @Inject constructor() : IdProvider<SessionId> {
    override fun next(): SessionId = SessionId(UUID.randomUUID().toString())
}

/**
 * Real profile identities.
 *
 * Random rather than sequential, because an id that can be guessed from another one is an id that
 * says how many children came before it.
 */
class UuidProfileIdProvider @Inject constructor() : IdProvider<ProfileId> {
    override fun next(): ProfileId = ProfileId(UUID.randomUUID().toString())
}
