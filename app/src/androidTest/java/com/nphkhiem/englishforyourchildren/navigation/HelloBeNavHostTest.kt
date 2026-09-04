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
    fun givenOneProfile_whenAppStarts_thenChildHomeIsRoot() {
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
    fun givenChildHomeIsRoot_whenBackIsPressed_thenTheAppExitsToTheLauncher() {
        // Child home is root after a profile resolves, so Back leaves Hello Bé rather than
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
    fun givenChildHome_whenTheLearningPathIsOpened_thenBackReturnsToHomeWithoutLeaving() {
        var exited = false
        setApp(profiles = listOf(ProfileId("minh")), onExitApp = { exited = true })

        composeTestRule.onNodeWithText(learningPathLabel()).requestFocus()
        composeTestRule.onNodeWithText(learningPathLabel())
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(unitKicker()).assertIsDisplayed()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        composeTestRule.waitForIdle()

        assertThat(exited).isFalse()
        composeTestRule.onNodeWithText(continueLabel()).assertIsDisplayed()
    }

    @Test
    fun givenALesson_whenBackIsPressed_thenTheStopForNowDialogOpensRatherThanTheStackPopping() {
        // The screen's own Back handler is composed deeper than the host's and takes the press
        // first, which is what keeps a child from leaving a lesson by accident.
        var exited = false
        setApp(profiles = listOf(ProfileId("minh")), onExitApp = { exited = true })

        composeTestRule.onNodeWithText(learningPathLabel()).requestFocus()
        composeTestRule.onNodeWithText(learningPathLabel())
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()
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

    private fun recoveryTitle() = string("recovery_db_title", learningPkg())

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
