package com.nphkhiem.englishforyourchildren.ui.tv.component

import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onSubscription

/**
 * The interaction source every focusable control here draws its focus from.
 *
 * A control learns it is focused only from a listener it starts after its first frame, and the
 * default source tells a listener nothing that happened before it subscribed. Focus can arrive
 * sooner than that: when the focused control leaves the screen, Android hands focus to the first
 * control of whatever replaced it, in the same frame. That control held focus while painted at
 * rest, so a child opening their first lesson saw nothing focused on it at all.
 */
@Composable
fun rememberHelloBeInteractionSource(): MutableInteractionSource =
    remember { FocusRetainingInteractionSource() }

/**
 * The default source, except that a listener arriving while the control is focused is told so
 * first. The focus is replayed from inside the subscription, so an unfocus emitted afterwards
 * reaches the listener after it and cannot be missed.
 */
@Stable
internal class FocusRetainingInteractionSource : MutableInteractionSource {
    // The default source's buffer, so nothing else about delivery changes.
    private val events = MutableSharedFlow<Interaction>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private var focus: FocusInteraction.Focus? = null

    override val interactions: Flow<Interaction> =
        events.onSubscription { focus?.let { emit(it) } }

    override suspend fun emit(interaction: Interaction) {
        track(interaction)
        events.emit(interaction)
    }

    override fun tryEmit(interaction: Interaction): Boolean {
        track(interaction)
        return events.tryEmit(interaction)
    }

    private fun track(interaction: Interaction) {
        when (interaction) {
            is FocusInteraction.Focus -> focus = interaction
            is FocusInteraction.Unfocus -> if (interaction.focus == focus) focus = null
        }
    }
}
