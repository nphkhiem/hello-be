@file:OptIn(ExperimentalComposeUiApi::class)

package com.nphkhiem.englishforyourchildren.ui.tv.component

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.tv.material3.Text
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeLayout
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeShapes
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme

/**
 * The child-safe dialog shell: centred Pip, title, description and two large actions.
 *
 * Three guarantees make this safe for a child holding a remote, and each is structural rather
 * than something a caller has to remember.
 *
 * The safe choice is focused the moment the dialog appears, so pressing Select immediately cannot
 * take the destructive path. [safeAction] receives the modifier carrying that focus rather than
 * arranging it itself, because a caller who forgot would ship a dialog where the first press ends
 * the lesson.
 *
 * Focus cannot leave the dialog while it is open. The scrim says the stage behind is unreachable,
 * so D-pad movement has to agree with it: without containment a child could walk focus onto a
 * control they can barely see and act on it.
 *
 * Focus returns to whatever opened the dialog when it leaves the composition, through the shared
 * seam from ADR 0001. [focusRestorer] is required for the same reason [safeAction] is pushed a
 * modifier: an optional restorer is an optional guarantee. Restoration is tied to disposal rather
 * than to a dismiss callback so that however the dialog closes, focus comes back; if the screen
 * behind it has changed meanwhile, restoring is a no-op rather than a crash.
 *
 * Fills the space it is given, which is the documented exception to sizing from the caller: a
 * scrim that covered only part of the stage would misrepresent what is still reachable.
 *
 * This is the shell only. The Stop-for-now copy and its pending-save variant belong to HB-D09.
 */
@Composable
fun StoryDialog(
    title: String,
    description: String,
    pipDescription: String,
    focusRestorer: HelloBeFocusRestorer,
    safeAction: @Composable (Modifier) -> Unit,
    secondaryAction: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    pip: @Composable () -> Unit = {
        PipGuide(
            pose = PipPose.GREETING,
            contentDescription = pipDescription,
            modifier = Modifier.size(HelloBeLayout.pipMinSize)
        )
    }
) {
    val colors = HelloBeTheme.colors
    val elevation = HelloBeTheme.elevation.high
    val safeActionFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        safeActionFocus.requestFocus()
    }

    // Keyed to the composition rather than to the restorer: keying on the restorer would dispose
    // and restore focus mid-life if a caller rebuilt it, throwing focus behind the scrim while the
    // dialog was still open.
    DisposableEffect(Unit) {
        onDispose { focusRestorer.restore() }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.scrim)
            // Focus cannot leave while the dialog is open, so D-pad movement agrees with what
            // the scrim says: the stage behind is unreachable.
            // focusProperties applies to the focus modifier that follows it, so the group must
            // come second or the containment silently does nothing.
            .focusProperties { onExit = { cancelFocusChange() } }
            .focusGroup()
            .semantics { dialog() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = HelloBeLayout.dialogMaxWidth)
                .shadow(
                    elevation = elevation.blurRadius,
                    shape = HelloBeShapes.dialog,
                    ambientColor = elevation.color,
                    spotColor = elevation.color
                )
                .background(colors.surfaceRaised, HelloBeShapes.dialog)
                .padding(HelloBeTheme.spacing.sectionGap),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.cardGap)
        ) {
            pip()

            Text(
                text = title,
                style = HelloBeTheme.typography.headlineLarge,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = description,
                style = HelloBeTheme.typography.bodyLarge,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.dialogActionGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                safeAction(Modifier.focusRequester(safeActionFocus))
                secondaryAction()
            }
        }
    }
}
