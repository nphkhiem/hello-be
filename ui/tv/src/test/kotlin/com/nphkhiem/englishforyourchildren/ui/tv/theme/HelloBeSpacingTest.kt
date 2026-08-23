package com.nphkhiem.englishforyourchildren.ui.tv.theme

import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class HelloBeSpacingTest {
    @Test
    fun givenSpacingScale_whenRawStepsAreRead_thenValuesMatchTheFourDpFoundation() {
        assertThat(HelloBeSpacing.space0).isEqualTo(0.dp)
        assertThat(HelloBeSpacing.space3).isEqualTo(8.dp)
        assertThat(HelloBeSpacing.space6).isEqualTo(24.dp)
        assertThat(HelloBeSpacing.space12).isEqualTo(96.dp)
    }

    @Test
    fun givenSpacingScale_whenSemanticAliasesAreRead_thenTheyMapToTheDocumentedStep() {
        assertThat(HelloBeSpacing.cardInternal).isEqualTo(HelloBeSpacing.space5)
        assertThat(HelloBeSpacing.cardGap).isEqualTo(HelloBeSpacing.space6)
        assertThat(HelloBeSpacing.sectionGap).isEqualTo(HelloBeSpacing.space7)
        assertThat(HelloBeSpacing.heroGap).isEqualTo(HelloBeSpacing.space8)
        assertThat(HelloBeSpacing.focusClearance).isEqualTo(HelloBeSpacing.space4)
        assertThat(HelloBeSpacing.dialogActionGap).isEqualTo(HelloBeSpacing.space5)
        assertThat(HelloBeSpacing.caregiverRowGap).isEqualTo(HelloBeSpacing.space3)
    }
}
