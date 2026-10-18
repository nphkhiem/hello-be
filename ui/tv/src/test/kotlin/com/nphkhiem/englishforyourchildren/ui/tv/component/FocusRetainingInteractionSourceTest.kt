package com.nphkhiem.englishforyourchildren.ui.tv.component

import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Test

class FocusRetainingInteractionSourceTest {

    private val source = FocusRetainingInteractionSource()

    @Test
    fun givenAControlAlreadyFocused_whenAListenerArrives_thenItIsToldFirst() {
        // The defect: a control focused before its listener started was drawn at rest.
        val focus = FocusInteraction.Focus()
        source.tryEmit(focus)

        assertThat(heardBy { }).containsExactly(focus)
    }

    @Test
    fun givenFocusHasAlreadyLeft_whenAListenerArrives_thenItIsToldNothing() {
        val focus = FocusInteraction.Focus()
        source.tryEmit(focus)
        source.tryEmit(FocusInteraction.Unfocus(focus))

        assertThat(heardBy { }).isEmpty()
    }

    @Test
    fun givenAListenerAlreadyListening_whenFocusComesAndGoes_thenItHearsEachOnceInOrder() {
        val focus = FocusInteraction.Focus()
        val unfocus = FocusInteraction.Unfocus(focus)

        val heard = heardBy {
            source.tryEmit(focus)
            source.tryEmit(unfocus)
        }

        assertThat(heard).containsExactly(focus, unfocus).inOrder()
    }

    /** Everything a listener that subscribes now hears, including what [meanwhile] emits. */
    private fun heardBy(meanwhile: () -> Unit): List<Interaction> = runBlocking {
        val heard = mutableListOf<Interaction>()
        val listener = launch(start = CoroutineStart.UNDISPATCHED) {
            source.interactions.collect { heard += it }
        }
        meanwhile()
        // Lets the listener take what was emitted before it is stopped.
        yield()
        listener.cancel()
        heard
    }
}
