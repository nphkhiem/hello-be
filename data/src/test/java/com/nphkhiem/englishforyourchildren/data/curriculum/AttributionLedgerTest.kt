package com.nphkhiem.englishforyourchildren.data.curriculum

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * That nothing ships unaccounted for, and that nothing claims to account for what is not there.
 *
 * Both sides of the rule are empty today, so the shipped bundle proves nothing about it. These
 * check a bundle built to be wrong, which is the same reason `StarterContentTest` mutates a copy
 * of the real course rather than only asserting the real one is clean.
 */
class AttributionLedgerTest {
    private val ledger = AttributionLedger()

    @Test
    fun givenAPackagedFileNothingAccountsFor_whenTheLedgerIsChecked_thenItIsNamed() {
        val problems = ledger.checkShipping(packaged = setOf("img-eyes"), rows = emptyList())

        assertThat(problems.map { it.what }).contains("is packaged with no attribution row")
        assertThat(problems.single().where).isEqualTo("img-eyes")
    }

    @Test
    fun givenARowForAFileNobodyPackaged_whenTheLedgerIsChecked_thenItIsNamed() {
        // A ledger claiming coverage it does not have. Harmless on its own and the reason an audit
        // stops trusting the rest of it.
        val problems = ledger.checkShipping(
            packaged = emptySet(),
            rows = listOf(complete("img-ears"))
        )

        assertThat(problems.map { it.what }).contains("has a row but no packaged file")
    }

    @Test
    fun givenEveryFileAccountedFor_whenTheLedgerIsChecked_thenThereIsNothingToReport() {
        val problems = ledger.checkShipping(
            packaged = setOf("img-eyes", "aud-en-eyes"),
            rows = listOf(complete("img-eyes"), complete("aud-en-eyes"))
        )

        assertThat(problems).isEmpty()
    }

    @Test
    fun givenARowWithNoLicenceEvidence_whenTheLedgerIsChecked_thenItIsNamed() {
        // The column a fabricated row would be missing, which is why it is the one held to.
        val problems = ledger.checkShipping(
            packaged = setOf("img-eyes"),
            rows = listOf(complete("img-eyes").copy(licenceEvidence = ""))
        )

        assertThat(problems.map { it.what }).contains("has no licence evidence")
    }

    @Test
    fun givenALicenceRequiringNoCredit_whenTheLedgerIsChecked_thenTheEmptyCreditIsNotAFault() {
        // Every other column real and no attribution text, which is a complete row rather than an
        // incomplete one: plenty of licences ask for no visible credit.
        val problems = ledger.checkShipping(
            packaged = setOf("img-eyes"),
            rows = listOf(complete("img-eyes"))
        )

        assertThat(problems).isEmpty()
    }

    @Test
    fun givenADevelopmentRow_whenItAppearsInTheShippingLedger_thenItIsNamed() {
        // The failure this whole ticket is built against: a pilot recording quietly becoming the
        // shipped voice. Living in the debug source set is what stops it, and this is what catches
        // somebody moving it back.
        val problems = ledger.checkShipping(
            packaged = setOf("aud-en-eyes"),
            rows = listOf(development("aud-en-eyes"))
        )

        assertThat(problems.map { it.what }).contains("is a development asset and cannot ship")
    }

    @Test
    fun givenADevelopmentRowWithNoLicence_whenItsOwnLedgerIsChecked_thenItStands() {
        // Incomplete by definition, which is exactly why it may not live beside the shipping rows.
        val problems = ledger.checkDevelopment(
            packaged = setOf("aud-en-eyes"),
            rows = listOf(development("aud-en-eyes"))
        )

        assertThat(problems).isEmpty()
    }

    private fun development(id: String) = AttributionDto(
        assetId = id,
        kind = "audio",
        provenance = "development",
        source = "Synthesized for the pilot",
        licence = "",
        licenceEvidence = "",
        attributionText = null
    )

    private fun complete(id: String) = AttributionDto(
        assetId = id,
        kind = "image",
        provenance = "commissioned",
        source = "A named illustrator",
        licence = "Work for hire, contract HB-2026-01",
        licenceEvidence = "contracts/HB-2026-01.pdf",
        attributionText = null
    )
}
