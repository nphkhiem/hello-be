package com.nphkhiem.englishforyourchildren.debugcatalog

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.nphkhiem.englishforyourchildren.R
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeElevationLevel
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeShapes
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeSpacing
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme
import com.nphkhiem.englishforyourchildren.ui.tv.theme.helloBeMotion

@Composable
internal fun ColorSwatchSection() {
    val colors = HelloBeTheme.colors
    val swatches =
        listOf(
            "canvas" to colors.canvas,
            "surface.primary" to colors.surfacePrimary,
            "surface.raised" to colors.surfaceRaised,
            "action.primary" to colors.actionPrimary,
            "action.secondary" to colors.actionSecondary,
            "focus.ring" to colors.focusRing,
            "accent.pip" to colors.accentPip,
            "accent.warm" to colors.accentWarm,
            "accent.growth" to colors.accentGrowth,
            "accent.calm" to colors.accentCalm,
            "accent.story" to colors.accentStory,
            "status.success" to colors.successContainer,
            "status.warning" to colors.warningContainer,
            "status.error" to colors.errorContainer,
            "status.info" to colors.infoContainer
        )

    LazyRow(horizontalArrangement = Arrangement.spacedBy(HelloBeSpacing.cardGap)) {
        items(swatches) { (label, color) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier =
                        Modifier
                            .size(64.dp)
                            .background(color, HelloBeShapes.medium)
                )
                Text(
                    text = label,
                    style = HelloBeTheme.typography.labelSmall,
                    color = colors.textSecondary
                )
            }
        }
    }
}

@Composable
internal fun TypographySampleSection() {
    val colors = HelloBeTheme.colors
    val typography = HelloBeTheme.typography
    val sampleEn = stringResource(R.string.theme_catalog_sample_en)
    val sampleVi = stringResource(R.string.theme_catalog_sample_vi)

    val roles =
        listOf(
            "display.hero" to typography.displayHero,
            "display.lessonPrompt" to typography.displayLessonPrompt,
            "headline.large" to typography.headlineLarge,
            "title.large" to typography.titleLarge,
            "body.medium" to typography.bodyMedium,
            "label.large" to typography.labelLarge,
            "caption" to typography.caption,
            "learningGlyph" to typography.learningGlyph
        )

    Column(verticalArrangement = Arrangement.spacedBy(HelloBeSpacing.cardInternal)) {
        roles.forEach { (label, style) ->
            Column {
                Text(
                    text = label,
                    style = HelloBeTheme.typography.labelSmall,
                    color = colors.textTertiary
                )
                Text(text = sampleEn, style = style, color = colors.textPrimary)
                Text(text = sampleVi, style = style, color = colors.textPrimary)
            }
        }
    }
}

@Composable
internal fun SpacingScaleSection() {
    val colors = HelloBeTheme.colors
    val steps =
        listOf(
            "space.0" to HelloBeSpacing.space0,
            "space.3" to HelloBeSpacing.space3,
            "space.5" to HelloBeSpacing.space5,
            "space.6" to HelloBeSpacing.space6,
            "space.7" to HelloBeSpacing.space7,
            "space.8" to HelloBeSpacing.space8,
            "space.10" to HelloBeSpacing.space10,
            "space.12" to HelloBeSpacing.space12
        )

    Column(verticalArrangement = Arrangement.spacedBy(HelloBeSpacing.space3)) {
        steps.forEach { (label, value) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(HelloBeSpacing.space5)
            ) {
                Text(
                    text = label,
                    style = HelloBeTheme.typography.labelSmall,
                    color = colors.textSecondary,
                    modifier = Modifier.width(72.dp)
                )
                Box(
                    modifier =
                        Modifier
                            .height(HelloBeSpacing.space5)
                            .width(if (value > 0.dp) value else 2.dp)
                            .background(colors.accentPip, HelloBeShapes.small)
                )
            }
        }
    }
}

@Composable
internal fun ShapeGallerySection() {
    val colors = HelloBeTheme.colors
    val shapes: List<Pair<String, Shape>> =
        listOf(
            "small" to HelloBeShapes.small,
            "medium" to HelloBeShapes.medium,
            "large" to HelloBeShapes.large,
            "extraLarge" to HelloBeShapes.extraLarge,
            "dialog" to HelloBeShapes.dialog,
            "storyPage" to HelloBeShapes.storyPage,
            "full" to HelloBeShapes.full
        )

    Row(horizontalArrangement = Arrangement.spacedBy(HelloBeSpacing.cardGap)) {
        shapes.forEach { (label, shape) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(56.dp).clip(shape).background(colors.actionPrimary))
                Text(
                    text = label,
                    style = HelloBeTheme.typography.labelSmall,
                    color = colors.textSecondary
                )
            }
        }
    }
}

@Composable
internal fun ElevationSection() {
    val colors = HelloBeTheme.colors
    val elevation = HelloBeTheme.elevation
    val levels =
        listOf(
            "low" to elevation.low,
            "medium" to elevation.medium,
            "high" to elevation.high,
            "focusGlow" to elevation.focusGlow
        )

    Row(horizontalArrangement = Arrangement.spacedBy(HelloBeSpacing.cardGap)) {
        levels.forEach { (label, level) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ElevationSwatch(level = level)
                Text(
                    text = label,
                    style = HelloBeTheme.typography.labelSmall,
                    color = colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun ElevationSwatch(level: HelloBeElevationLevel) {
    val colors = HelloBeTheme.colors
    Box(
        modifier =
            Modifier
                .size(72.dp)
                .shadow(
                    elevation = level.blurRadius,
                    shape = HelloBeShapes.large,
                    ambientColor = level.color,
                    spotColor = level.color
                ).background(colors.surfaceRaised, HelloBeShapes.large)
    )
}

@Composable
internal fun MotionSection() {
    val colors = HelloBeTheme.colors

    Row(horizontalArrangement = Arrangement.spacedBy(HelloBeSpacing.cardGap)) {
        MotionDemoColumn(
            label = stringResource(R.string.theme_catalog_standard_motion_label),
            reduceMotion = false
        )
        MotionDemoColumn(
            label = stringResource(R.string.theme_catalog_reduced_motion_label),
            reduceMotion = true
        )
    }
    Text(
        text = "focus=${helloBeMotion(
            false
        ).focusTransitionMillis}ms vs ${helloBeMotion(true).focusTransitionMillis}ms",
        style = HelloBeTheme.typography.labelSmall,
        color = colors.textTertiary
    )
}

@Composable
private fun MotionDemoColumn(label: String, reduceMotion: Boolean) {
    val colors = HelloBeTheme.colors
    val motion = remember(reduceMotion) { helloBeMotion(reduceMotion) }
    var focused by remember { mutableStateOf(false) }
    val scale by
        animateFloatAsState(
            targetValue = if (focused) HelloBeTheme.focus.scaleButton else 1f,
            animationSpec = tween(durationMillis = motion.focusTransitionMillis),
            label = "catalogMotionDemo"
        )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier =
                Modifier
                    .size((72 * scale).dp)
                    .clip(HelloBeShapes.large)
                    .background(colors.actionPrimary)
                    .clickable { focused = !focused }
        )
        Text(text = label, style = HelloBeTheme.typography.labelSmall, color = colors.textSecondary)
    }
}
