package com.nphkhiem.englishforyourchildren.debugcatalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Text
import com.nphkhiem.englishforyourchildren.R
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeAction
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeActionTone
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeFocusRestorer
import com.nphkhiem.englishforyourchildren.ui.tv.component.StoryDialog
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeSpacing
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme

/**
 * Just the opener. The dialog itself is hosted at the catalog root, because a dialog embedded in a
 * scrolling column would scrim only its own slice of the page and misrepresent how it behaves.
 */
@Composable
internal fun DialogSection(focusRestorer: HelloBeFocusRestorer, onOpen: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(HelloBeSpacing.space3)) {
        Text(
            text = stringResource(R.string.theme_catalog_dialog_label),
            style = HelloBeTheme.typography.labelSmall,
            color = HelloBeTheme.colors.textTertiary
        )

        HelloBeAction(
            label = stringResource(R.string.theme_catalog_dialog_open),
            onClick = onOpen,
            tone = HelloBeActionTone.SECONDARY,
            modifier = Modifier.focusRequester(focusRestorer.returnTarget)
        )
    }
}

/** The dialog as a real screen would host it: above everything, scrimming the whole stage. */
@Composable
internal fun CatalogDialog(focusRestorer: HelloBeFocusRestorer, onClose: () -> Unit) {
    StoryDialog(
        title = stringResource(R.string.theme_catalog_dialog_title),
        description = stringResource(R.string.theme_catalog_dialog_description),
        pipDescription = stringResource(R.string.theme_catalog_dialog_pip),
        focusRestorer = focusRestorer,
        safeAction = { modifier ->
            HelloBeAction(
                label = stringResource(R.string.theme_catalog_dialog_keep),
                onClick = onClose,
                tone = HelloBeActionTone.POSITIVE,
                modifier = modifier
            )
        },
        secondaryAction = { modifier ->
            HelloBeAction(
                label = stringResource(R.string.theme_catalog_dialog_stop),
                onClick = onClose,
                tone = HelloBeActionTone.QUIET,
                modifier = modifier
            )
        }
    )
}
