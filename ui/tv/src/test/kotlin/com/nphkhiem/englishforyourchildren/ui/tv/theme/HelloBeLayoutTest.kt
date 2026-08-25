package com.nphkhiem.englishforyourchildren.ui.tv.theme

import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class HelloBeLayoutTest {
    @Test
    fun givenLayoutTokens_whenReferenceCanvasIsRead_thenItMatchesTheMdpiCanvas() {
        assertThat(HelloBeLayout.referenceWidth).isEqualTo(960.dp)
        assertThat(HelloBeLayout.referenceHeight).isEqualTo(540.dp)
        assertThat(HelloBeLayout.safeHorizontal).isEqualTo(58.dp)
        assertThat(HelloBeLayout.safeVertical).isEqualTo(28.dp)
    }

    @Test
    fun givenLayoutTokens_whenChildAndCaregiverControlSizesAreRead_thenMinimumsMatchTokenSpec() {
        assertThat(HelloBeLayout.childPrimaryActionMinHeight).isEqualTo(72.dp)
        assertThat(HelloBeLayout.childChoiceMinHeight).isEqualTo(120.dp)
        assertThat(HelloBeLayout.caregiverControlMinHeight).isEqualTo(56.dp)
        assertThat(HelloBeLayout.dialogMaxWidth).isEqualTo(690.dp)
        assertThat(HelloBeLayout.caregiverRailWidth).isEqualTo(196.dp)
    }

    @Test
    fun givenLayoutTokens_whenColumnGridIsRead_thenTwelveColumnsAtFiftyTwoDpAreDefined() {
        assertThat(HelloBeLayout.columns).isEqualTo(12)
        assertThat(HelloBeLayout.columnWidth).isEqualTo(52.dp)
        assertThat(HelloBeLayout.gutter).isEqualTo(20.dp)
    }

    @Test
    fun givenLayoutTokens_whenPipMinSizeIsRead_thenPipStaysRecognisableAcrossProfiles() {
        assertThat(HelloBeLayout.pipMinSize).isEqualTo(96.dp)
    }

    @Test
    fun givenLayoutTokens_whenChromeSizesAreRead_thenTheyAreReservedSizesNotSpacingSteps() {
        assertThat(HelloBeLayout.captionReservedHeight).isEqualTo(76.dp)
        assertThat(HelloBeLayout.trailSegmentWidth).isEqualTo(12.dp)
        assertThat(HelloBeLayout.trailSegmentCurrentWidth).isEqualTo(24.dp)
        assertThat(HelloBeLayout.trailSegmentHeight).isEqualTo(8.dp)
    }

    @Test
    fun givenTrailTokens_whenCompared_thenTheCurrentStepIsWiderThanTheRest() {
        assertThat(HelloBeLayout.trailSegmentCurrentWidth.value)
            .isGreaterThan(HelloBeLayout.trailSegmentWidth.value)
    }

    @Test
    fun givenLayoutTokens_whenLoadingPlaceholderIsRead_thenItIsAReservedSize() {
        assertThat(HelloBeLayout.loadingPlaceholderHeight).isEqualTo(20.dp)
    }
}
