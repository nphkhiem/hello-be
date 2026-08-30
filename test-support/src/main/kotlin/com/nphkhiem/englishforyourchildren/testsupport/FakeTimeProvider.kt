package com.nphkhiem.englishforyourchildren.testsupport

import com.nphkhiem.englishforyourchildren.domain.model.EpochMillis
import com.nphkhiem.englishforyourchildren.domain.time.TimeProvider

/**
 * A clock that only moves when a test moves it.
 *
 * It refuses to be wound backwards. Time going forwards is an assumption the progress records rely
 * on, since a checkpoint written before the one it replaces would be a resume that loses work, and
 * a fake that allowed it would let a test prove behaviour the real clock can never produce.
 */
class FakeTimeProvider(initial: EpochMillis) : TimeProvider {
    private var current: EpochMillis = initial

    override fun now(): EpochMillis = current

    fun advanceBy(milliseconds: Long) {
        require(milliseconds >= 0) { "Time does not run backwards" }
        current = EpochMillis(current.value + milliseconds)
    }
}
