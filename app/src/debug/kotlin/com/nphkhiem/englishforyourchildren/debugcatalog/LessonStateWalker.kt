package com.nphkhiem.englishforyourchildren.debugcatalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Text
import com.nphkhiem.englishforyourchildren.R
import com.nphkhiem.englishforyourchildren.feature.learning.LessonAction
import com.nphkhiem.englishforyourchildren.feature.learning.LessonUiState
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeAction
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeActionTone
import com.nphkhiem.englishforyourchildren.ui.tv.component.helloBeFocusGroup
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeLayout
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeSpacing
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme

/**
 * Steps one activity through every state it can be in, with the remote.
 *
 * Shared by each activity family's catalog section rather than copied per family, because two
 * walkers would drift and a reviewer would end up comparing activities through slightly different
 * harnesses.
 *
 * The states come from the feature's own fixtures rather than being rebuilt here, so what a
 * reviewer walks through is exactly what the tests assert against. No ViewModel, no audio and no
 * curriculum sits behind it: that is the point of the state carrying everything the screen needs.
 */
@Composable
internal fun LessonStateWalker(
    label: String,
    states: List<Pair<String, LessonUiState>>,
    activity: @Composable (LessonUiState, (LessonAction) -> Unit) -> Unit
) {
    var index by remember { mutableIntStateOf(0) }
    var lastAction by remember { mutableStateOf<LessonAction?>(null) }
    // A dialog contains focus, which is correct on a television and would otherwise strand a
    // reviewer: with the dialog open, Next state cannot be reached. So the dialog's own actions
    // close it here, which is also the behaviour a real host would implement.
    var dialogDismissed by remember(index) { mutableStateOf(false) }
    val (stateName, rawState) = states[index]
    val state = if (dialogDismissed) rawState.copy(stopForNowVisible = false) else rawState

    Column(verticalArrangement = Arrangement.spacedBy(HelloBeSpacing.space4)) {
        Text(
            text = label,
            style = HelloBeTheme.typography.labelSmall,
            color = HelloBeTheme.colors.textTertiary
        )

        Row(
            modifier = Modifier.helloBeFocusGroup(),
            horizontalArrangement = Arrangement.spacedBy(HelloBeSpacing.cardGap)
        ) {
            HelloBeAction(
                label = stringResource(R.string.theme_catalog_lesson_next_state),
                onClick = { index = (index + 1) % states.size },
                tone = HelloBeActionTone.SECONDARY
            )
            Text(
                text = stringResource(
                    R.string.theme_catalog_lesson_showing,
                    index + 1,
                    states.size,
                    stateName
                ),
                style = HelloBeTheme.typography.labelSmall,
                color = HelloBeTheme.colors.textSecondary
            )
            lastAction?.let { action ->
                Text(
                    text = stringResource(
                        R.string.theme_catalog_lesson_last_action,
                        action.toString()
                    ),
                    style = HelloBeTheme.typography.labelSmall,
                    color = HelloBeTheme.colors.textTertiary
                )
            }
        }

        // Shown at the reference canvas height so the stage is reviewed at the shape a television
        // actually gives it, rather than squeezed into whatever the catalog has left over.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(HelloBeLayout.referenceHeight)
        ) {
            activity(state) { action ->
                lastAction = action
                if (action is LessonAction.KeepLearningRequested ||
                    action is LessonAction.StopForNowConfirmed
                ) {
                    dialogDismissed = true
                }
            }
        }
    }
}
