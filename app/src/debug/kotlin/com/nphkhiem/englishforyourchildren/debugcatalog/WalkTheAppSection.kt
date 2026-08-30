package com.nphkhiem.englishforyourchildren.debugcatalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Text
import com.nphkhiem.englishforyourchildren.R
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import com.nphkhiem.englishforyourchildren.navigation.FixtureContent
import com.nphkhiem.englishforyourchildren.navigation.FixtureProfileGateway
import com.nphkhiem.englishforyourchildren.navigation.HelloBeNavHost
import com.nphkhiem.englishforyourchildren.navigation.ProfileSnapshot
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeAction
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeActionTone
import com.nphkhiem.englishforyourchildren.ui.tv.component.helloBeFocusGroup
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeLayout
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeSpacing
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme

/**
 * The app itself, walkable, on fixture content.
 *
 * Every other section in this catalog shows one screen in one state. This one is the real
 * navigation host with the real screens, so an owner can walk a route rather than inspect a
 * gallery: choose a profile, open an adventure, start a lesson, press Back and meet the
 * stop-for-now dialog, finish and land on the celebration.
 *
 * It exists because an installed build cannot do this yet. There is no data layer, so the app
 * honestly reports that storage cannot be read and opens on the caregiver recovery. Fixture
 * content lives here in debug sources, where the rest of this project's fixtures live, rather
 * than being smuggled into an installed build to make a demo possible.
 *
 * The launch counts step through the entry-resolution table: no profiles opens profile creation,
 * one opens that child's home, several open the picker.
 */
@Composable
internal fun WalkTheAppSection() {
    var profileCount by remember { mutableIntStateOf(1) }
    var restarts by remember { mutableIntStateOf(0) }

    Column(verticalArrangement = Arrangement.spacedBy(HelloBeSpacing.space4)) {
        Text(
            text = stringResource(R.string.theme_catalog_walk_label),
            style = HelloBeTheme.typography.labelSmall,
            color = HelloBeTheme.colors.textTertiary
        )

        Row(
            modifier = Modifier.helloBeFocusGroup(),
            horizontalArrangement = Arrangement.spacedBy(HelloBeSpacing.cardGap)
        ) {
            HelloBeAction(
                label = stringResource(R.string.theme_catalog_lesson_next_state),
                onClick = {
                    profileCount = (profileCount + 1) % (MAX_LAUNCH_PROFILES + 1)
                    restarts++
                },
                tone = HelloBeActionTone.SECONDARY
            )
            HelloBeAction(
                label = stringResource(R.string.theme_catalog_walk_reset),
                onClick = { restarts++ },
                tone = HelloBeActionTone.QUIET
            )
            Text(
                text = stringResource(
                    R.string.theme_catalog_lesson_showing,
                    profileCount + 1,
                    MAX_LAUNCH_PROFILES + 1,
                    launchDescription(profileCount)
                ),
                style = HelloBeTheme.typography.labelSmall,
                color = HelloBeTheme.colors.textSecondary
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(HelloBeLayout.referenceHeight)
        ) {
            // Keyed so Start again rebuilds the host from its entry destination, which is the only
            // way back to launch once a walk has replaced the root.
            key(profileCount, restarts) {
                HelloBeNavHost(
                    gateway = FixtureProfileGateway(
                        ProfileSnapshot(
                            storageReadable = true,
                            validProfileIds = (1..profileCount).map { ProfileId("p$it") },
                            rememberedProfileId = null
                        )
                    ),
                    content = FixtureContent(),
                    onExitApp = { restarts++ }
                )
            }
        }
    }
}

private fun launchDescription(profileCount: Int): String = when (profileCount) {
    0 -> "no profiles, create is root"
    1 -> "one profile, child home is root"
    else -> "$profileCount profiles, picker is root"
}

private const val MAX_LAUNCH_PROFILES = 4
