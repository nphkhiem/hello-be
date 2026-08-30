package com.nphkhiem.englishforyourchildren.domain.time

import com.nphkhiem.englishforyourchildren.domain.model.EpochMillis

/**
 * Where the current time comes from.
 *
 * It exists so that nothing which records a checkpoint, an attempt or a last-practised time reads
 * the wall clock directly. A test that reads the real clock is a test whose result depends on when
 * it ran, and progress records are exactly the things whose timestamps have to be assertable.
 */
fun interface TimeProvider {
    fun now(): EpochMillis
}
