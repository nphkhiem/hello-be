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
import com.nphkhiem.englishforyourchildren.feature.profiles.CreateProfileAction
import com.nphkhiem.englishforyourchildren.feature.profiles.CreateProfileFixtures
import com.nphkhiem.englishforyourchildren.feature.profiles.CreateProfileScreen
import com.nphkhiem.englishforyourchildren.feature.profiles.ProfileAction
import com.nphkhiem.englishforyourchildren.feature.profiles.ProfilePickerFixtures
import com.nphkhiem.englishforyourchildren.feature.profiles.ProfilePickerScreen
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeAction
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeActionTone
import com.nphkhiem.englishforyourchildren.ui.tv.component.helloBeFocusGroup
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeLayout
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeSpacing
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme

/**
 * The first screen anyone sees, in every state it can be in.
 *
 * Deliberately not routed through [LessonStateWalker]: that one is typed to lesson state, and the
 * picker is not a lesson. Sharing it would mean widening it to something generic enough to hold
 * both, which is how the shared thing stops meaning anything.
 */
@Composable
internal fun ProfilePickerSection() {
    val states = remember { ProfilePickerFixtures.reviewStates() }
    var index by remember { mutableIntStateOf(0) }
    var lastAction by remember { mutableStateOf<ProfileAction?>(null) }
    val (stateName, state) = states[index]

    Column(verticalArrangement = Arrangement.spacedBy(HelloBeSpacing.space4)) {
        Text(
            text = stringResource(R.string.theme_catalog_picker_label),
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(HelloBeLayout.referenceHeight)
        ) {
            ProfilePickerScreen(state = state, onAction = { lastAction = it })
        }
    }
}

/**
 * The screen Add child leads to. Worth walking with the press count in mind: the default path is
 * two, and every state here is a departure from it.
 */
@Composable
internal fun CreateProfileSection() {
    val states = remember { CreateProfileFixtures.reviewStates() }
    var index by remember { mutableIntStateOf(0) }
    var lastAction by remember { mutableStateOf<CreateProfileAction?>(null) }
    val (stateName, state) = states[index]

    Column(verticalArrangement = Arrangement.spacedBy(HelloBeSpacing.space4)) {
        Text(
            text = stringResource(R.string.theme_catalog_create_label),
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(HelloBeLayout.referenceHeight)
        ) {
            CreateProfileScreen(state = state, onAction = { lastAction = it })
        }
    }
}
