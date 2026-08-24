@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.nphkhiem.englishforyourchildren.debugcatalog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Border
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.FilterChip
import androidx.tv.material3.FilterChipDefaults
import androidx.tv.material3.Text
import com.nphkhiem.englishforyourchildren.R
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeShapes
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeSpacing
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeThemeMode

@Composable
fun ThemeCatalogScreen() {
    var themeMode by remember { mutableStateOf(HelloBeThemeMode.DAY) }
    var highContrast by remember { mutableStateOf(false) }

    HelloBeTheme(themeMode = themeMode, highContrast = highContrast) {
        val colors = HelloBeTheme.colors

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
                themeMode = themeMode,
                onThemeModeChange = { themeMode = it },
                highContrast = highContrast,
                onHighContrastChange = { highContrast = it }
            )

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
    }
}

@Composable
private fun CatalogModeControls(
    themeMode: HelloBeThemeMode,
    onThemeModeChange: (HelloBeThemeMode) -> Unit,
    highContrast: Boolean,
    onHighContrastChange: (Boolean) -> Unit
) {
    val colors = HelloBeTheme.colors

    Row(horizontalArrangement = Arrangement.spacedBy(HelloBeSpacing.cardGap)) {
        CatalogToggleChip(
            label = stringResource(R.string.theme_catalog_mode_day),
            selected = themeMode == HelloBeThemeMode.DAY,
            onClick = { onThemeModeChange(HelloBeThemeMode.DAY) }
        )
        CatalogToggleChip(
            label = stringResource(R.string.theme_catalog_mode_night),
            selected = themeMode == HelloBeThemeMode.NIGHT,
            onClick = { onThemeModeChange(HelloBeThemeMode.NIGHT) }
        )
        CatalogToggleChip(
            label = stringResource(R.string.theme_catalog_high_contrast),
            selected = highContrast,
            onClick = { onHighContrastChange(!highContrast) }
        )
    }
    Row {
        Text(
            text = "${themeMode.name} · highContrast=$highContrast",
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
 * state a hand-rolled implementation kept losing across earlier iterations. The gold focus
 * ring (`colors.focusRing`) marks both focused and selected so the state is never lost.
 */
@Composable
private fun CatalogToggleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = HelloBeTheme.colors
    val focus = HelloBeTheme.focus
    val interactionSource = remember { MutableInteractionSource() }
    val goldRing =
        Border(
            border = BorderStroke(focus.ringWidth, colors.focusRing),
            shape = HelloBeShapes.large
        )

    FilterChip(
        selected = selected,
        onClick = onClick,
        interactionSource = interactionSource,
        shape = FilterChipDefaults.shape(shape = HelloBeShapes.large),
        colors =
            FilterChipDefaults.colors(
                containerColor = colors.actionSecondary,
                contentColor = colors.onSecondary,
                focusedContainerColor = colors.actionPrimaryFocused,
                focusedContentColor = colors.onPrimary,
                pressedContainerColor = colors.actionPrimaryPressed,
                pressedContentColor = colors.onPrimary,
                selectedContainerColor = colors.successContainer,
                selectedContentColor = colors.successContent,
                disabledContainerColor = colors.surfaceMuted,
                disabledContentColor = colors.textTertiary,
                focusedSelectedContainerColor = colors.actionPrimaryFocused,
                focusedSelectedContentColor = colors.onPrimary,
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
                focusedBorder = goldRing,
                selectedBorder = Border.None,
                focusedSelectedBorder = goldRing
            )
    ) {
        Text(text = label, style = HelloBeTheme.typography.labelMedium)
    }
}
