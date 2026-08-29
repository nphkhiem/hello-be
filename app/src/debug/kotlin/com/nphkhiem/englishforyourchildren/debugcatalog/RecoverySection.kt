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
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverFixtures
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverRecovery
import com.nphkhiem.englishforyourchildren.feature.learning.ChildRecovery
import com.nphkhiem.englishforyourchildren.feature.learning.ChildRecoveryReason
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeAction
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeActionTone
import com.nphkhiem.englishforyourchildren.ui.tv.component.helloBeFocusGroup
import com.nphkhiem.englishforyourchildren.ui.tv.component.rememberHelloBeFocusRestorer
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeLayout
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeSpacing
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme

/**
 * All five recoveries in one place, which is the only way to check they behave like a family.
 *
 * Walk them asking the three questions the task sets: does each say what happened, what is still
 * safe, and where the focused action goes? Then check the two rules that hold across all of them.
 * The focused action is never the destructive one, and no child variant carries a diagnostic code.
 *
 * The last entry is the caregiver one. It is the only recovery with a code on it, and the only one
 * that lives behind the adult gate.
 */
@Composable
internal fun RecoveryCatalogSection() {
    val reasons = remember { ChildRecoveryReason.entries.toList() }
    var index by remember { mutableIntStateOf(0) }
    var lastAction by remember { mutableStateOf<Any?>(null) }
    val restorer = rememberHelloBeFocusRestorer()
    val caregiverIndex = reasons.size
    val showing = if (index == caregiverIndex) "caregiver database" else reasons[index].name

    Column(verticalArrangement = Arrangement.spacedBy(HelloBeSpacing.space4)) {
        Text(
            text = stringResource(R.string.theme_catalog_recovery_label),
            style = HelloBeTheme.typography.labelSmall,
            color = HelloBeTheme.colors.textTertiary
        )

        Row(
            modifier = Modifier.helloBeFocusGroup(),
            horizontalArrangement = Arrangement.spacedBy(HelloBeSpacing.cardGap)
        ) {
            HelloBeAction(
                label = stringResource(R.string.theme_catalog_lesson_next_state),
                onClick = { index = (index + 1) % (reasons.size + 1) },
                tone = HelloBeActionTone.SECONDARY
            )
            Text(
                text = stringResource(
                    R.string.theme_catalog_lesson_showing,
                    index + 1,
                    reasons.size + 1,
                    showing
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
            if (index == caregiverIndex) {
                CaregiverRecovery(
                    state = CaregiverFixtures.databaseRecovery(),
                    onAction = { lastAction = it }
                )
            } else {
                ChildRecovery(
                    reason = reasons[index],
                    focusRestorer = restorer,
                    onAction = { lastAction = it }
                )
            }
        }
    }
}
