package com.nphkhiem.englishforyourchildren.ui.tv.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class HelloBeShapesTest {
    @Test
    fun givenLargeShape_whenCompared_thenCornerRadiusMatchesEighteenDp() {
        assertThat(HelloBeShapes.large).isEqualTo(RoundedCornerShape(18.dp))
    }

    @Test
    fun givenDialogShape_whenCompared_thenCornerRadiusMatchesTwentyEightDp() {
        assertThat(HelloBeShapes.dialog).isEqualTo(RoundedCornerShape(28.dp))
    }

    @Test
    fun givenStoryPageShape_whenCompared_thenTopAndBottomCornersDiffer() {
        assertThat(HelloBeShapes.storyPage).isEqualTo(
            RoundedCornerShape(
                topStart = 30.dp,
                topEnd = 30.dp,
                bottomEnd = 16.dp,
                bottomStart = 16.dp
            )
        )
    }

    @Test
    fun givenFullShape_whenReferencedByFeatureCode_thenItIsTheCircleShapeSingleton() {
        assertThat(HelloBeShapes.full).isSameInstanceAs(CircleShape)
    }
}
