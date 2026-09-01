package com.nphkhiem.englishforyourchildren.data.curriculum

/**
 * That every asset reaching a child's television is one this project may ship.
 *
 * `ATTRIBUTION_LEDGER.md` is the evidence an app store review, a licence audit or a future
 * maintainer reads, and this is the part of it a machine can hold to. It answers in
 * [ContentProblem]s rather than throwing, for the reason the curriculum validator does: somebody
 * fixing a bundle should be told everything that is wrong with it at once.
 *
 * What it deliberately does not have is a way to be told a row is cleared. The ledger defines
 * clearance as derived, "Yes only when every column above is real", so completeness is the whole
 * of it. A flag beside the columns would be a second answer to the same question, and the one that
 * could be set without gathering anything.
 */
class AttributionLedger {

    /**
     * The ledger that ships, held to everything.
     *
     * [packaged] is asset ids taken from the files actually present, not from what content refers
     * to. The difference is the point: content naming a file nobody made is the validator's
     * business, and a file nobody accounted for is this one's.
     */
    fun checkShipping(packaged: Set<String>, rows: List<AttributionDto>): List<ContentProblem> =
        covers(packaged, rows) + rows.flatMap { complete(it) + notDevelopment(it) }

    /**
     * The ledger that does not ship, held to coverage alone.
     *
     * A development row is incomplete by definition, having no licence to name, so holding it to
     * completeness would only mean nobody could record one honestly. What keeps it from shipping is
     * that it lives in a source set a release build does not read.
     */
    fun checkDevelopment(packaged: Set<String>, rows: List<AttributionDto>): List<ContentProblem> =
        covers(packaged, rows)

    private fun covers(packaged: Set<String>, rows: List<AttributionDto>): List<ContentProblem> {
        val claimed = rows.map { it.assetId }.toSet()
        return (
            (packaged - claimed).sorted().map { fault(it, PACKAGED_WITHOUT_ROW) } +
                (claimed - packaged).sorted().map { fault(it, ROW_WITHOUT_FILE) }
            )
    }

    /**
     * Every column real, except the credit string.
     *
     * A licence requiring no visible attribution legitimately has none, so an empty one there is a
     * complete row rather than an unfinished one.
     */
    private fun complete(row: AttributionDto): List<ContentProblem> = buildList {
        if (row.kind.isBlank()) add(fault(row.assetId, NO_KIND))
        if (row.provenance.isBlank()) add(fault(row.assetId, NO_PROVENANCE))
        if (row.source.isBlank()) add(fault(row.assetId, NO_SOURCE))
        if (row.licence.isBlank()) add(fault(row.assetId, NO_LICENCE))
        if (row.licenceEvidence.isBlank()) add(fault(row.assetId, NO_EVIDENCE))
    }

    private fun notDevelopment(row: AttributionDto): List<ContentProblem> {
        if (row.provenance != DEVELOPMENT) return emptyList()
        return listOf(fault(row.assetId, IS_DEVELOPMENT))
    }

    private fun fault(assetId: String, what: String) =
        ContentProblem(assetId, what, ContentProblem.Kind.UNLICENSED_ASSET)

    private companion object {
        const val DEVELOPMENT = "development"
        const val PACKAGED_WITHOUT_ROW = "is packaged with no attribution row"
        const val ROW_WITHOUT_FILE = "has a row but no packaged file"
        const val NO_KIND = "does not say what kind of asset it is"
        const val NO_PROVENANCE = "does not say where it came from"
        const val NO_SOURCE = "has no source"
        const val NO_LICENCE = "has no licence"
        const val NO_EVIDENCE = "has no licence evidence"
        const val IS_DEVELOPMENT = "is a development asset and cannot ship"
    }
}
