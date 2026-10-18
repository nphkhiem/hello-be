package com.nphkhiem.englishforyourchildren.debugcatalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
internal fun WalkTheAppSection(profileCount: Int, onNextLaunch: () -> Unit, onOpen: () -> Unit) {
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
                onClick = onNextLaunch,
                tone = HelloBeActionTone.SECONDARY
            )
            HelloBeAction(
                label = stringResource(R.string.theme_catalog_walk_open),
                onClick = onOpen,
                tone = HelloBeActionTone.SECONDARY
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
    }
}

/**
 * The app itself, walkable, filling the stage.
 *
 * Hosted at the catalog root and only while it is asked for, which is what makes Back work here.
 *
 * Back goes to the most recently registered handler, and registration order is by the moment a
 * handler mounts rather than by where it sits in the source. The walk used to be composed with the
 * page, as its first section, so every lesson, free play and celebration specimen mounted after it
 * and outranked it. Measured on a device rather than assumed: the celebration specimen answered the
 * first Back and this host answered later ones, both off screen, so a reviewer pressing Back saw
 * nothing happen while the adult gate in front of them said Back returns to the child screen.
 *
 * Mounting on request inverts that. Whatever the page registered during its first composition is
 * older than this, so the walk always wins while it is open, and registers nothing while it is not.
 * At the walked app's own root Back leaves, which closes the walk and returns to the catalog.
 */
@Composable
internal fun CatalogWalkTheApp(profileCount: Int, restarts: Int, onExit: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().testTag(WALK_TAG)) {
        // Keyed so a fresh launch count rebuilds the host from its entry destination, which is the
        // only way back to launch once a walk has replaced the root.
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
                onExitApp = onExit
            )
        }
    }
}

/** Identifies the raised walk, which is otherwise the same screens the gallery already shows. */
internal const val WALK_TAG = "catalog-walk"

private fun launchDescription(profileCount: Int): String = when (profileCount) {
    0 -> "no profiles, create is root"
    1 -> "one profile, child home is root"
    else -> "$profileCount profiles, picker is root"
}

private const val MAX_LAUNCH_PROFILES = 4
