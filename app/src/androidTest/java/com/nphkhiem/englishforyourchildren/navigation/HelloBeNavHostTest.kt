package com.nphkhiem.englishforyourchildren.navigation

import android.view.KeyEvent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.HelloBeRoot
import com.nphkhiem.englishforyourchildren.domain.model.CaregiverLanguage
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import com.nphkhiem.englishforyourchildren.feature.caregiver.R as CaregiverR
import com.nphkhiem.englishforyourchildren.feature.caregiver.caregiverText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HelloBeNavHostTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val resources
        get() = InstrumentationRegistry.getInstrumentation().targetContext.resources

    @Test
    fun givenNoProfiles_whenAppStarts_thenCreateProfileIsRoot() {
        setApp(profiles = emptyList())

        composeTestRule.onNodeWithText(createProfileHeading()).assertIsDisplayed()
    }

    @Test
    fun givenOneProfile_whenAppStarts_thenTheLearningPathIsRoot() {
        setApp(profiles = listOf(ProfileId("minh")))

        composeTestRule.onNodeWithText(continueLabel()).assertIsDisplayed()
    }

    @Test
    fun givenMultipleProfiles_whenAppStarts_thenProfilePickerIsRoot() {
        setApp(profiles = listOf(ProfileId("minh"), ProfileId("lan")))

        composeTestRule.onNodeWithText(pickerHeading()).assertIsDisplayed()
    }

    @Test
    fun givenStorageCannotBeRead_whenAppStarts_thenItAsksForAGrownUpRatherThanInventingAChild() {
        // What an installed build does today, and the reason no fabricated profile appears in one.
        composeTestRule.setContent {
            HelloBeRoot(
                gateway = UnavailableProfileGateway(),
                content = FixtureContent(),
                onExitApp = {}
            )
        }

        composeTestRule.onNodeWithText(recoveryTitle()).assertIsDisplayed()
    }

    @Test
    fun givenTheLearningPathIsRoot_whenBackIsPressed_thenTheAppExitsToTheLauncher() {
        // The path is root after a profile resolves, so Back leaves Hello Bé rather than
        // returning to a picker the child has already answered.
        var exited = false
        setApp(profiles = listOf(ProfileId("minh")), onExitApp = { exited = true })

        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        composeTestRule.waitForIdle()

        assertThat(exited).isTrue()
    }

    @Test
    fun givenAProfileIsChosen_whenBackIsPressed_thenItStillExitsRatherThanReturningToThePicker() {
        var exited = false
        setApp(
            profiles = listOf(ProfileId("minh"), ProfileId("lan")),
            onExitApp = { exited = true }
        )

        composeTestRule.onNodeWithText(pickerHeading()).assertIsDisplayed()
        composeTestRule.onNodeWithText(FIRST_PROFILE).requestFocus()
        composeTestRule.onNodeWithText(FIRST_PROFILE)
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(continueLabel()).assertIsDisplayed()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        composeTestRule.waitForIdle()

        assertThat(exited).isTrue()
    }

    @Test
    fun givenTheLearningPathIsRoot_whenItIsDrawn_thenTheUnitIsAlreadyOnScreen() {
        // Child home used to sit in front of this, and reaching the unit cost a press. The two
        // screens answered the same question, so the path absorbed the other and the unit is
        // simply what a child arrives at.
        setApp(profiles = listOf(ProfileId("minh")))

        composeTestRule.onNodeWithText(unitKicker()).assertIsDisplayed()
        composeTestRule.onNodeWithText(continueLabel()).assertIsDisplayed()
    }

    @Test
    fun givenALesson_whenBackIsPressed_thenTheStopForNowDialogOpensRatherThanTheStackPopping() {
        // The screen's own Back handler is composed deeper than the host's and takes the press
        // first, which is what keeps a child from leaving a lesson by accident.
        var exited = false
        setApp(profiles = listOf(ProfileId("minh")), onExitApp = { exited = true })

        composeTestRule.onNodeWithText(RECOMMENDED_LESSON)
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(stopForNowTitle()).assertIsDisplayed()
        assertThat(exited).isFalse()
    }

    @Test
    fun givenACaregiverSession_whenTheAppLeavesTheForeground_thenTheSessionIsGone() {
        // The session is foreground-scoped, per the information architecture. A child who picks
        // the remote up after a caregiver walked away must not find settings behind Back, so
        // leaving the foreground drops everything behind the gate.
        // Built and moved on the main thread, which LifecycleRegistry insists on.
        lateinit var lifecycle: TestLifecycleOwner
        composeTestRule.runOnUiThread { lifecycle = TestLifecycleOwner() }
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycle) {
                HelloBeRoot(
                    gateway = FixtureProfileGateway(
                        ProfileSnapshot(
                            storageReadable = true,
                            validProfileIds = listOf(ProfileId("minh")),
                            rememberedProfileId = null
                        )
                    ),
                    content = FixtureContent(),
                    onExitApp = {}
                )
            }
        }

        composeTestRule.onNodeWithText(grownUps()).requestFocus()
        composeTestRule.onNodeWithText(grownUps()).performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(CORRECT_ANSWER).requestFocus()
        composeTestRule.onNodeWithText(CORRECT_ANSWER)
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(caregiverArea()).assertIsDisplayed()

        composeTestRule.runOnUiThread { lifecycle.stop() }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(caregiverArea()).assertDoesNotExist()
        composeTestRule.onNodeWithText(continueLabel()).assertIsDisplayed()
    }

    /** A lifecycle this test can move, so foreground scoping can be exercised at all. */
    private class TestLifecycleOwner : LifecycleOwner {
        private val registry = LifecycleRegistry(this)

        init {
            registry.currentState = Lifecycle.State.RESUMED
        }

        override val lifecycle: Lifecycle get() = registry

        fun stop() {
            registry.currentState = Lifecycle.State.CREATED
        }
    }

    @Test
    fun givenTheLearningPath_whenGrownUpsIsPressed_thenTheAdultGateOpens() {
        // The learning path is becoming the child's root, so this is the only door to the adult
        // gate. The host swallowed unknown path actions in an `else`, which would have compiled a
        // dead control and lost every caregiver surface behind it without failing anything.
        setApp(profiles = listOf(ProfileId("minh")))

        press(grownUpsLabel())

        // Substring: caregiver surfaces render bilingually, so the gate's heading arrives as
        // "Grown-ups only" followed by its Vietnamese half.
        composeTestRule.onNodeWithText(gateTitle(), substring = true).assertIsDisplayed()
    }

    @Test
    fun givenTheLearningPath_whenFreePlayIsPressed_thenFreePlayOpens() {
        setApp(profiles = listOf(ProfileId("minh")))

        press(freePlayLabel())

        composeTestRule.onNodeWithText(freePlayHeading()).assertIsDisplayed()
    }

    private fun press(label: String) {
        composeTestRule.onNodeWithText(label).requestFocus()
        composeTestRule.onNodeWithText(label).performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()
    }

    private fun grownUpsLabel() = string("home_grown_ups", learningPkg())

    private fun freePlayLabel() = string("home_free_play", learningPkg())

    private fun freePlayHeading() = string("free_play_title", learningPkg())

    /** The gate's heading rather than its sum, which is generated fresh each time. */
    private fun gateTitle() = string("gate_title", learningPkg())

    private fun setApp(profiles: List<ProfileId>, onExitApp: () -> Unit = {}) {
        composeTestRule.setContent {
            HelloBeRoot(
                gateway = FixtureProfileGateway(
                    ProfileSnapshot(
                        storageReadable = true,
                        validProfileIds = profiles,
                        rememberedProfileId = null
                    )
                ),
                content = FixtureContent(),
                onExitApp = onExitApp
            )
        }
    }

    private fun string(name: String, pkg: String): String {
        val id = resources.getIdentifier(name, "string", pkg)
        return resources.getString(id)
    }

    private fun learningPkg() = "com.nphkhiem.englishforyourchildren"

    private fun continueLabel() = string("home_continue", learningPkg())

    private fun learningPathLabel() = string("home_learning_path", learningPkg())

    private fun createProfileHeading() = string("create_question", learningPkg())

    private fun pickerHeading() = string("picker_welcome", learningPkg())

    private fun unitKicker() = "Unit 2 of 12"

    private fun stopForNowTitle() = string("stop_for_now_title", learningPkg())

    /** Read as the screen reads it: the recovery panel is a caregiver surface, so bilingual. */
    private fun recoveryTitle() = InstrumentationRegistry.getInstrumentation()
        .targetContext
        .caregiverText(CaregiverLanguage.BOTH, CaregiverR.string.recovery_db_title)

    private fun grownUps() = string("home_grown_ups", learningPkg())

    /**
     * What the caregiver frame actually says, which is both languages.
     *
     * Read the way the screen reads it rather than straight from resources. The string used to
     * carry both halves itself; it now carries the English and the caregiver's chosen language
     * decides what is shown beside it, which for the default is the Vietnamese.
     */
    private fun caregiverArea() = InstrumentationRegistry.getInstrumentation()
        .targetContext
        .caregiverText(CaregiverLanguage.BOTH, CaregiverR.string.caregiver_area)

    private companion object {
        const val FIRST_PROFILE = "Minh"
        const val RECOMMENDED_LESSON = "Hands and feet"
        const val CORRECT_ANSWER = "11"
    }
}
