package com.nphkhiem.englishforyourchildren.ui.tv.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeShapes
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme

/**
 * The single thing a lesson is currently about, shown but never chosen.
 *
 * Picture matching's fixed source, the letter pair, and the say-with-Pip target object are all
 * this component: `DESIGN_BRIEF.md` lists the large focal object in the shared lesson composition,
 * so it lives here rather than being rebuilt inside each activity family.
 *
 * There is deliberately no `onClick`, no `availability` and no `selected` parameter. A learning
 * object is not an answer, and a component with no click cannot be wired to one later by a caller
 * in a hurry, so "the object never takes focus" holds structurally rather than by convention.
 * That is the same reason `StorybookScaffold` owns the safe area instead of asking screens to
 * remember an inset.
 *
 * It renders the Default row of the component state matrix - primary surface, secondary border -
 * because a thing that is never focused, selected or answered has no other state to be in.
 *
 * [labelVisible] withholds the drawn word while keeping it as the accessible name, for activities
 * whose prompt already names the object in text.
 *
 * [labelStyle] lets an activity give the object the weight its content needs. Letter and sound
 * passes `learningGlyph`, so the letter carries the emphasis itself rather than the container
 * acquiring a heavier treatment that would compete with the focus grammar.
 *
 * The card sizes to its content and takes its width and height from its parent, so a board can
 * align it to a grid of choices without a dedicated dimension token.
 */
@Composable
fun LearningObjectCard(
    label: String,
    modifier: Modifier = Modifier,
    labelVisible: Boolean = true,
    labelStyle: TextStyle = HelloBeTheme.typography.titleMedium,
    illustration: (@Composable () -> Unit)? = null
) {
    val colors = HelloBeTheme.colors
    val shape = HelloBeShapes.extraLarge

    Surface(
        modifier = modifier.semantics {
            // Only when the word is not drawn. The Text below is already the accessible name, so
            // setting both would make a screen reader announce the object twice.
            if (!labelVisible) contentDescription = label
        },
        shape = shape,
        colors = SurfaceDefaults.colors(
            containerColor = colors.surfacePrimary,
            contentColor = colors.textPrimary
        ),
        border = HelloBeFocusFrame.resting(shape)
    ) {
        Column(
            modifier = Modifier.padding(HelloBeTheme.spacing.cardInternal),
            horizontalAlignment = Alignment.CenterHorizontally,
            // Centred on the column's own arrangement rather than by aligning it in the surface.
            // Surface propagates its minimum constraints, so the column is already stretched to
            // the full card height and there is nothing left for align to position.
            verticalArrangement = Arrangement.spacedBy(
                space = HelloBeTheme.spacing.space3,
                alignment = Alignment.CenterVertically
            )
        ) {
            illustration?.invoke()
            if (labelVisible) {
                Text(text = label, style = labelStyle)
            }
        }
    }
}
