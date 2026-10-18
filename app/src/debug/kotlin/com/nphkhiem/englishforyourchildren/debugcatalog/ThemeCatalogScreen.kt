@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.nphkhiem.englishforyourchildren.debugcatalog

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Border
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.FilterChip
import androidx.tv.material3.FilterChipDefaults
import androidx.tv.material3.Text
import com.nphkhiem.englishforyourchildren.R
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverConfirmationAction
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeFocusFrame
import com.nphkhiem.englishforyourchildren.ui.tv.component.helloBeFocusGroup
import com.nphkhiem.englishforyourchildren.ui.tv.component.rememberHelloBeFocusRestorer
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeShapes
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeSpacing
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeThemeMode

/** The three display settings the catalog can toggle, kept together so they travel as one. */
private data class CatalogModeState(
    val themeMode: HelloBeThemeMode = HelloBeThemeMode.DAY,
    val highContrast: Boolean = false,
    val reduceMotion: Boolean = false
)

@Composable
fun ThemeCatalogScreen() {
    var mode by remember { mutableStateOf(CatalogModeState()) }
    var dialogOpen by remember { mutableStateOf(false) }
    var walkOpen by remember { mutableStateOf(false) }
    var walkProfileCount by remember { mutableIntStateOf(1) }
    var walkRestarts by remember { mutableIntStateOf(0) }
    var confirmationOpen by remember { mutableStateOf(false) }
    var confirmationIndex by remember { mutableIntStateOf(0) }
    var confirmationAction by remember { mutableStateOf<CaregiverConfirmationAction?>(null) }
    var recoveryOpen by remember { mutableStateOf(false) }
    var recoveryIndex by remember { mutableIntStateOf(0) }
    var recoveryAction by remember { mutableStateOf<Any?>(null) }
    val catalogEntryFocus = remember { FocusRequester() }

    // The catalog asks for entry focus itself, one frame after composition.
    //
    // Every specimen below claims focus too, and each is right to: a screen owns its entry focus
    // and a dialog focuses its safe action on appearing, which is what they must do where they
    // really live. Composed together in a gallery they all claim at once.
    //
    // What this guarantees and what it does not, measured rather than assumed: asking a frame late
    // beats every claim made during the first composition. It does not beat a screen specimen whose
    // claim is gated on its own state settling, such as a learning path passing
    // `entryFocusReady = !state.loading`, so the catalog can still open scrolled to one of those.
    // That is untidy and a reviewer can simply press up out of it. What it must never do again is
    // open inside a dialog, which contains focus and cannot be left at all; the two dialog families
    // are gated behind openers for that reason, and tested.
    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { catalogEntryFocus.requestFocus() }
    }

    HelloBeTheme(
        themeMode = mode.themeMode,
        highContrast = mode.highContrast,
        reduceMotion = mode.reduceMotion
    ) {
        val colors = HelloBeTheme.colors
        val dialogRestorer = rememberHelloBeFocusRestorer()
        val confirmationRestorer = rememberHelloBeFocusRestorer()
        val recoveryRestorer = rememberHelloBeFocusRestorer()

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(colors.canvas)
                        .verticalScroll(rememberScrollState())
                        .padding(HelloBeSpacing.sectionGap),
                verticalArrangement = Arrangement.spacedBy(HelloBeSpacing.sectionGap)
            ) {
                Text(
                    text = stringResource(R.string.theme_catalog_title),
                    style = HelloBeTheme.typography.headlineLarge,
                    color = colors.textPrimary
                )

                CatalogModeControls(
                    mode = mode,
                    onModeChange = { mode = it },
                    entryFocus = catalogEntryFocus
                )

                CatalogSectionHeading(stringResource(R.string.theme_catalog_walk_heading))
                WalkTheAppSection(
                    profileCount = walkProfileCount,
                    onNextLaunch = {
                        walkProfileCount = (walkProfileCount + 1) % (MAX_LAUNCH_PROFILES + 1)
                        walkRestarts++
                    },
                    onOpen = { walkOpen = true }
                )

                CatalogSectionHeading(stringResource(R.string.theme_catalog_gate_heading))
                AdultGateCatalogSection()

                CatalogSectionHeading(stringResource(R.string.theme_catalog_shell_heading))
                CaregiverShellCatalogSection()

                CatalogSectionHeading(stringResource(R.string.theme_catalog_overview_heading))
                CaregiverOverviewCatalogSection()

                CatalogSectionHeading(stringResource(R.string.theme_catalog_settings_heading))
                CaregiverSettingsCatalogSection()

                CatalogSectionHeading(stringResource(R.string.theme_catalog_profiles_heading))
                ProfileManagementCatalogSection()

                CatalogSectionHeading(stringResource(R.string.theme_catalog_confirm_heading))
                CaregiverConfirmationCatalogSection(
                    index = confirmationIndex,
                    lastAction = confirmationAction,
                    restorer = confirmationRestorer,
                    onNextState = {
                        confirmationIndex = (confirmationIndex + 1) % CONFIRMATION_STATE_COUNT
                    },
                    onOpen = { confirmationOpen = true }
                )

                CatalogSectionHeading(stringResource(R.string.theme_catalog_recovery_heading))
                RecoveryCatalogSection(
                    index = recoveryIndex,
                    lastAction = recoveryAction,
                    restorer = recoveryRestorer,
                    onNextState = { recoveryIndex = (recoveryIndex + 1) % RECOVERY_STATE_COUNT },
                    onOpen = { recoveryOpen = true }
                )

                CatalogSectionHeading(stringResource(R.string.theme_catalog_stage_heading))
                StageSection()

                CatalogSectionHeading(stringResource(R.string.theme_catalog_pip_heading))
                PipSection()

                CatalogSectionHeading(stringResource(R.string.theme_catalog_chrome_heading))
                StageChromeSection()

                CatalogSectionHeading(stringResource(R.string.theme_catalog_feedback_heading))
                FeedbackAndLoadingSection()

                CatalogSectionHeading(stringResource(R.string.theme_catalog_picker_heading))
                ProfilePickerSection()

                CatalogSectionHeading(stringResource(R.string.theme_catalog_home_heading))

                CatalogSectionHeading(stringResource(R.string.theme_catalog_create_heading))
                CreateProfileSection()

                CatalogSectionHeading(stringResource(R.string.theme_catalog_path_heading))
                LearningPathSection()

                CatalogSectionHeading(stringResource(R.string.theme_catalog_lesson_heading))
                LessonSection()

                CatalogSectionHeading(stringResource(R.string.theme_catalog_matching_heading))
                MatchingSection()

                CatalogSectionHeading(stringResource(R.string.theme_catalog_letter_heading))
                LetterAndSoundSection()

                CatalogSectionHeading(stringResource(R.string.theme_catalog_say_heading))
                SayWithPipSection()

                CatalogSectionHeading(stringResource(R.string.theme_catalog_review_heading))
                ReviewSection()

                CatalogSectionHeading(stringResource(R.string.theme_catalog_free_play_heading))
                FreePlaySection()

                CatalogSectionHeading(stringResource(R.string.theme_catalog_celebration_heading))
                CelebrationSection()

                CatalogSectionHeading(stringResource(R.string.theme_catalog_dialog_heading))
                DialogSection(
                    focusRestorer = dialogRestorer,
                    onOpen = { dialogOpen = true }
                )

                CatalogSectionHeading(stringResource(R.string.theme_catalog_focus_lab_heading))
                FocusLabSection()

                CatalogSectionHeading(stringResource(R.string.theme_catalog_colors_heading))
                ColorSwatchSection()

                CatalogSectionHeading(stringResource(R.string.theme_catalog_typography_heading))
                TypographySampleSection()

                CatalogSectionHeading(stringResource(R.string.theme_catalog_spacing_heading))
                SpacingScaleSection()

                CatalogSectionHeading(stringResource(R.string.theme_catalog_shapes_heading))
                ShapeGallerySection()

                CatalogSectionHeading(stringResource(R.string.theme_catalog_elevation_heading))
                ElevationSection()

                CatalogSectionHeading(stringResource(R.string.theme_catalog_motion_heading))
                MotionSection()
            }

            // Hosted at the root rather than inside the scrolling column, so the scrim covers the
            // whole screen exactly as it would on a real stage.
            if (walkOpen) {
                CatalogWalkTheApp(
                    profileCount = walkProfileCount,
                    restarts = walkRestarts,
                    onExit = { walkOpen = false }
                )
            }

            if (dialogOpen) {
                CatalogDialog(
                    focusRestorer = dialogRestorer,
                    onClose = { dialogOpen = false }
                )
            }

            if (recoveryOpen) {
                CatalogRecovery(
                    index = recoveryIndex,
                    restorer = recoveryRestorer,
                    onAction = { action ->
                        recoveryAction = action
                        recoveryOpen = false
                        recoveryRestorer.restore()
                    }
                )
            }

            if (confirmationOpen) {
                CatalogCaregiverConfirmation(
                    index = confirmationIndex,
                    restorer = confirmationRestorer,
                    onAction = { action ->
                        confirmationAction = action
                        if (action != CaregiverConfirmationAction.RetryRequested) {
                            confirmationOpen = false
                            confirmationRestorer.restore()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun CatalogModeControls(
    mode: CatalogModeState,
    onModeChange: (CatalogModeState) -> Unit,
    entryFocus: FocusRequester
) {
    val colors = HelloBeTheme.colors

    Row(
        modifier = Modifier.helloBeFocusGroup(),
        horizontalArrangement = Arrangement.spacedBy(HelloBeSpacing.cardGap)
    ) {
        CatalogToggleChip(
            label = stringResource(R.string.theme_catalog_mode_day),
            selected = mode.themeMode == HelloBeThemeMode.DAY,
            onClick = { onModeChange(mode.copy(themeMode = HelloBeThemeMode.DAY)) },
            modifier = Modifier.focusRequester(entryFocus)
        )
        CatalogToggleChip(
            label = stringResource(R.string.theme_catalog_mode_night),
            selected = mode.themeMode == HelloBeThemeMode.NIGHT,
            onClick = { onModeChange(mode.copy(themeMode = HelloBeThemeMode.NIGHT)) }
        )
        CatalogToggleChip(
            label = stringResource(R.string.theme_catalog_high_contrast),
            selected = mode.highContrast,
            onClick = { onModeChange(mode.copy(highContrast = !mode.highContrast)) }
        )
        CatalogToggleChip(
            label = stringResource(R.string.theme_catalog_reduced_motion_label),
            selected = mode.reduceMotion,
            onClick = { onModeChange(mode.copy(reduceMotion = !mode.reduceMotion)) }
        )
    }
    Row {
        Text(
            text = stringResource(
                R.string.theme_catalog_mode_summary,
                mode.themeMode.name,
                mode.highContrast.toString(),
                mode.reduceMotion.toString()
            ),
            style = HelloBeTheme.typography.labelSmall,
            color = colors.textTertiary
        )
    }
}

@Composable
internal fun CatalogSectionHeading(title: String) {
    Text(
        text = title,
        style = HelloBeTheme.typography.headlineMedium,
        color = HelloBeTheme.colors.textPrimary
    )
}

/**
 * Uses the official [FilterChip] from `androidx.tv.material3` instead of a hand-rolled
 * clickable box. Google's own [FilterChipDefaults] models "focused" and "selected" as
 * independently combinable states (see `focusedSelectedContainerColor` etc.), which is the
 * state a hand-rolled implementation kept losing across earlier iterations. The focus ring
 * marks both focused and selected so the state is never lost.
 *
 * Selection uses the dedicated `action.selected` family rather than `accent.growth`: green is
 * reserved for confirmed success, so borrowing it made "this chip is on" look like "this answer
 * is correct". The selected chip also keeps its own outline, so selection survives on fill alone
 * only when the fill is unambiguous.
 */
@Composable
private fun CatalogToggleChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = HelloBeTheme.colors
    val focus = HelloBeTheme.focus
    val interactionSource = remember { MutableInteractionSource() }
    // Borders come from HelloBeFocusFrame rather than being rebuilt here, so the chip picks up
    // the same ring width, colour and offset as every other focusable control.
    val focusRing = HelloBeFocusFrame.ring(HelloBeShapes.large)
    val selectionOutline = HelloBeFocusFrame.selection(HelloBeShapes.large)

    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        interactionSource = interactionSource,
        shape = FilterChipDefaults.shape(shape = HelloBeShapes.large),
        colors =
            FilterChipDefaults.colors(
                containerColor = colors.actionSecondary,
                contentColor = colors.onSecondary,
                focusedContainerColor = colors.focusFill,
                focusedContentColor = colors.onFocusFill,
                pressedContainerColor = colors.actionPrimaryPressed,
                pressedContentColor = colors.onPrimary,
                selectedContainerColor = colors.actionSelected,
                selectedContentColor = colors.onSelected,
                disabledContainerColor = colors.surfaceMuted,
                disabledContentColor = colors.textTertiary,
                focusedSelectedContainerColor = colors.focusFill,
                focusedSelectedContentColor = colors.onFocusFill,
                pressedSelectedContainerColor = colors.actionPrimaryPressed,
                pressedSelectedContentColor = colors.onPrimary
            ),
        scale =
            FilterChipDefaults.scale(
                focusedScale = focus.scaleButton,
                focusedSelectedScale = focus.scaleButton
            ),
        border =
            FilterChipDefaults.border(
                border = Border.None,
                focusedBorder = focusRing,
                selectedBorder = selectionOutline,
                focusedSelectedBorder = focusRing
            )
    ) {
        Text(text = label, style = HelloBeTheme.typography.labelMedium)
    }
}

/** Launch counts the walk steps through, matching the entry-resolution table. */
private const val MAX_LAUNCH_PROFILES = 4

/** How many confirmation states the walker steps through. */
private const val CONFIRMATION_STATE_COUNT = 2

/** How many recovery states the walker steps through, including the caregiver one. */
private const val RECOVERY_STATE_COUNT = 6
