package com.nphkhiem.englishforyourchildren.playback

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.domain.model.AssetId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Media3PlaybackControllerTest {
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val controller =
        Media3PlaybackController(context, PackagedAssetLocator.forAudio(context))

    @After
    fun releasePlayer() {
        controller.stop()
    }

    @Test
    fun givenARecordingNobodyHasMade_whenItIsPlayed_thenItFailsAsMissing() {
        val event = awaitEvent { controller.play(listOf(UNRECORDED)) }

        assertThat(event).isEqualTo(PlaybackEvent.Failed(UNRECORDED, PlaybackFailureCode.MISSING))
    }

    @Test
    fun givenAPackagedRecording_whenItIsPlayed_thenItReachesItsEnd() {
        val event = awaitEvent { controller.play(listOf(TONE)) }

        assertThat(event).isEqualTo(PlaybackEvent.Completed(TONE))
    }

    @Test
    fun givenASequenceMissingItsSecondHalf_whenItIsPlayed_thenNothingIsHeardAtAll() {
        // A question is a stem and then the word it is about. Starting the stem and finding out
        // half a second later that the word was never recorded would leave a child listening to a
        // sentence that stops in the middle, so every part is found before any of it plays.
        val event = awaitEvent { controller.play(listOf(TONE, UNRECORDED)) }

        assertThat(event).isEqualTo(PlaybackEvent.Failed(UNRECORDED, PlaybackFailureCode.MISSING))
    }

    @Test
    fun givenASequenceThatIsAllThere_whenItIsPlayed_thenTheEndIsReportedAgainstItsLastPart() {
        // One question asked, not two recordings played. The first event to arrive names the word
        // rather than the stem, so a listener cannot be told the question is over while the word
        // it is about is still being said. See ADR 0004.
        val event = awaitEvent { controller.play(listOf(TONE, WORD)) }

        assertThat(event).isEqualTo(PlaybackEvent.Completed(WORD))
    }

    /**
     * The whole lifecycle rule in one story, because the three halves only mean something
     * together: leaving pauses, coming back does not undo that, and only the child does.
     */
    @Test
    fun givenARecordingIsSounding_whenTheAppLeavesAndComesBack_thenOnlyTheChildRestartsIt() =
        withSubscription { event ->
            val owner = foregroundOwner()

            controller.play(listOf(TONE))
            onMain { owner.registry.currentState = Lifecycle.State.CREATED }
            onMain { owner.registry.currentState = Lifecycle.State.STARTED }

            assertThat(withTimeoutOrNull(SILENCE_MILLIS) { event.await() }).isNull()

            controller.resume()

            assertThat(withTimeout(TIMEOUT_MILLIS) { event.await() })
                .isEqualTo(PlaybackEvent.Completed(TONE))
        }

    @Test
    fun givenARecordingIsSounding_whenAnotherReplacesIt_thenTheOutgoingOneSaysNothing() =
        withSubscription { event ->
            controller.play(listOf(TONE))
            controller.play(listOf(UNRECORDED))

            // Never Completed(TONE): a recording a child never heard the end of did not end.
            assertThat(withTimeout(TIMEOUT_MILLIS) { event.await() })
                .isEqualTo(PlaybackEvent.Failed(UNRECORDED, PlaybackFailureCode.MISSING))
        }

    @Test
    fun givenARecordingIsSounding_whenPlaybackIsStopped_thenNothingMoreIsHeardFromIt() =
        withSubscription { event ->
            controller.play(listOf(TONE))
            controller.stop()

            assertThat(withTimeoutOrNull(SILENCE_MILLIS) { event.await() }).isNull()
        }

    /**
     * Subscribes before anything is played, undispatched so the collector is attached before the
     * caller acts, and always cancelled afterwards.
     *
     * The cancelling is not tidiness: [runBlocking] waits for its children, so a subscription left
     * pending in a test that expects silence would hang the run rather than fail it.
     */
    private fun withSubscription(block: suspend CoroutineScope.(Deferred<PlaybackEvent>) -> Unit) =
        runBlocking {
            val event = async(start = CoroutineStart.UNDISPATCHED) { controller.events.first() }
            try {
                block(event)
            } finally {
                event.cancel()
            }
        }

    private fun foregroundOwner(): TestOwner {
        val owner = TestOwner()
        onMain {
            owner.registry.currentState = Lifecycle.State.RESUMED
            owner.registry.addObserver(controller)
        }
        return owner
    }

    private fun awaitEvent(act: suspend () -> Unit): PlaybackEvent {
        lateinit var seen: PlaybackEvent
        withSubscription { event ->
            act()
            seen = withTimeout(TIMEOUT_MILLIS) { event.await() }
        }
        return seen
    }

    private fun onMain(block: () -> Unit) =
        InstrumentationRegistry.getInstrumentation().runOnMainSync(block)

    private class TestOwner : LifecycleOwner {
        val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle get() = registry
    }

    private companion object {
        val UNRECORDED = AssetId("aud-en-prompt-where-is")
        val TONE = AssetId("aud-test-tone")

        /** A second playable file, so "the last one" is a claim a test can actually fail. */
        val WORD = AssetId("aud-test-word")
        const val TIMEOUT_MILLIS = 5_000L

        /** Comfortably longer than the fixture, so "still silent" means it really is paused. */
        const val SILENCE_MILLIS = 2_000L
    }
}
