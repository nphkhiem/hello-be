package com.nphkhiem.englishforyourchildren.debugcatalog

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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

@Composable
private fun CatalogToggleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = HelloBeTheme.colors
    val focus = HelloBeTheme.focus
    val motion = HelloBeTheme.motion
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val background =
        when {
            isFocused -> colors.actionPrimaryFocused
            selected -> colors.actionPrimary
            else -> colors.actionSecondary
        }
    val content = if (isFocused || selected) colors.onPrimary else colors.onSecondary
    val scale by
        animateFloatAsState(
            targetValue = if (isFocused) focus.scaleButton else 1f,
            animationSpec = tween(durationMillis = motion.focusTransitionMillis),
            label = "catalogChipFocusScale"
        )

    Box(
        modifier =
            Modifier
                .scale(scale)
                .then(
                    if (isFocused) {
                        Modifier.border(focus.ringWidth, colors.focusRing, HelloBeShapes.large)
                    } else {
                        Modifier
                    }
                )
                .padding(focus.ringWidth)
                .then(
                    if (isFocused) {
                        Modifier.border(focus.guardWidth, colors.focusGuard, HelloBeShapes.large)
                    } else {
                        Modifier
                    }
                )
                .padding(focus.guardWidth)
                .widthIn(min = 96.dp)
                .clip(HelloBeShapes.large)
                .background(background)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .padding(horizontal = HelloBeSpacing.space5, vertical = HelloBeSpacing.space3),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, style = HelloBeTheme.typography.labelMedium, color = content)
    }
}
