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
import com.nphkhiem.englishforyourchildren.feature.learning.LessonFixtures
import com.nphkhiem.englishforyourchildren.feature.learning.ListenAndChooseActivity
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeAction
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeActionTone
import com.nphkhiem.englishforyourchildren.ui.tv.component.helloBeFocusGroup
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeLayout
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeSpacing
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme

/**
 * A real lesson, in every state it can be in, steppable with the remote.
 *
 * The states come from the feature's own fixtures rather than being rebuilt here, so what a
 * reviewer walks through is exactly what the tests assert against. No ViewModel, no audio and no
 * curriculum sits behind it: that is the point of the state carrying everything the screen needs.
 */
@Composable
internal fun LessonSection() {
    val states = remember { LessonFixtures.reviewStates() }
    var index by remember { mutableIntStateOf(0) }
    var lastAction by remember { mutableStateOf<LessonAction?>(null) }
    val (label, state) = states[index]

    Column(verticalArrangement = Arrangement.spacedBy(HelloBeSpacing.space4)) {
        Text(
            text = stringResource(R.string.theme_catalog_lesson_label),
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
                    label
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
            ListenAndChooseActivity(state = state, onAction = { lastAction = it })
        }
    }
}
