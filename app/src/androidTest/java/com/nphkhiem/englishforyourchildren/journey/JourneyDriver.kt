package com.nphkhiem.englishforyourchildren.journey

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.printToLog
import androidx.compose.ui.test.requestFocus
import androidx.test.platform.app.InstrumentationRegistry

/** A remote control, for a test. Everything a child can do here, they do with the D-pad. */
class JourneyDriver(private val compose: ComposeTestRule) {

    /**
     * Focus, then Select.
     *
     * A semantics click is not enough on this television: it lands focus on a card and leaves it
     * unchosen, because every control is driven by the D-pad rather than by a tap.
     */
    fun press(text: String) {
        awaitReachable(text)
        compose.onNodeWithText(text).requestFocus()
        lookAtIt()
        compose.onNodeWithText(text).performKeyInput { pressKey(Key.DirectionCenter) }
        compose.waitForIdle()
    }

    /**
     * Select pressed twice in quick succession.
     *
     * Two complete key cycles with nothing between them, so the second arrives before the screen
     * has had any chance to react to the first. Calling [press] twice would not be the same
     * gesture: it waits for idle in between, and that gap is the whole point.
     *
     * Deliberately not called a hold. A held key produces system repeat, which is a different
     * gesture that may or may not fire a click per repeat, and nothing here has established which.
     */
    fun doublePress(text: String) {
        awaitReachable(text)
        compose.onNodeWithText(text).requestFocus()
        lookAtIt()
        compose.onNodeWithText(text).performKeyInput {
            pressKey(Key.DirectionCenter)
            pressKey(Key.DirectionCenter)
        }
        compose.waitForIdle()
    }

    /**
     * Waits until the control will actually take focus.
     *
     * Now that lessons speak, an answer is out of the focus order for as long as its question is
     * sounding: that is ADR 0004, and it is the point rather than a delay to work around. A driver
     * that focused regardless was asking for something the screen is deliberately refusing, and
     * failed with "Failed to perform RequestFocus action" the moment real recordings shipped.
     *
     * Waiting here is what a child does. It is not a sleep: it ends as soon as the control is
     * enabled, so a screen with nothing sounding is not slowed down at all.
     */
    private fun awaitReachable(text: String) {
        try {
            compose.waitUntil(TIMEOUT_MILLIS) {
                compose.onAllNodes(hasText(text) and isEnabled()).fetchSemanticsNodes().isNotEmpty()
            }
        } catch (_: ComposeTimeoutException) {
            compose.onRoot().printToLog("hello-be-journey")
            throw AssertionError("\"$text\" never became something a child could press")
        }
    }

    /**
     * The moment a child spends looking at something before pressing at it.
     *
     * A lesson refuses an answer that lands the instant a question appears, because a press that
     * quick belongs to the gesture that turned the page rather than to the question now on it. A
     * test drives a television faster than any child can, so it waits where a child would look.
     * Only the first press of a gesture waits: what makes a double press one is the gap that is not
     * there between its halves.
     */
    private fun lookAtIt() {
        Thread.sleep(LOOKED_AT_IT_MILLIS)
    }

    /**
     * Waits for the screen to actually say something, rather than for recomposition to settle.
     *
     * On failure it prints what the screen did say, because a journey that stops half way is
     * otherwise a timeout with no clue which of a dozen presses failed to land.
     */
    fun awaitText(text: String) {
        try {
            compose.waitUntil(TIMEOUT_MILLIS) {
                compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
            }
        } catch (timeout: ComposeTimeoutException) {
            compose.onRoot(useUnmergedTree = true).printToLog("JOURNEY")
            throw AssertionError("The screen never said \"$text\"", timeout)
        }
    }

    fun string(name: String): String {
        val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
        return resources.getString(resources.getIdentifier(name, "string", PACKAGE))
    }

    private companion object {
        const val PACKAGE = "com.nphkhiem.englishforyourchildren"
        const val TIMEOUT_MILLIS = 10_000L

        /** Longer than the window a lesson holds a new question closed for. */
        const val LOOKED_AT_IT_MILLIS = 400L
    }
}
