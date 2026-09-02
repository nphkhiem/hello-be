package com.nphkhiem.englishforyourchildren.data.curriculum

import kotlinx.serialization.Serializable

/**
 * The packaged course exactly as it is written on disk.
 *
 * These are the shape of the files, not the shape of the domain. Keeping them apart is what lets
 * the content format change without every screen learning about it, and it is where a malformed
 * bundle is caught rather than halfway through a lesson.
 */
@Serializable
data class CourseDto(
    val schemaVersion: Int,
    val id: String,
    val courseVersion: String,
    val supportedLocales: List<String>,
    val units: List<UnitRefDto>,
    val supportPhrases: List<SupportPhraseDto> = emptyList()
)

@Serializable
data class UnitRefDto(val id: String, val ordinal: Int, val file: String)

@Serializable
data class SupportPhraseDto(val id: String, val vi: String)

@Serializable
data class UnitDto(
    val schemaVersion: Int,
    val id: String,
    val ordinal: Int,
    val theme: String,
    val word: String,
    val scenery: String = "",
    val lessons: List<LessonDto>
)

@Serializable
data class LessonDto(
    val id: String,
    val ordinal: Int,
    val kind: String,
    val teaches: List<String> = emptyList(),
    val letters: List<String> = emptyList(),
    val activities: List<ActivityDto>
)

@Serializable
data class ActivityDto(
    val id: String,
    val ordinal: Int,
    val family: String,
    val prompt: String,
    val promptAsset: String,
    val choices: List<ChoiceDto> = emptyList(),
    val correctSkillId: String? = null,
    val letterSkillId: String? = null,
    val letterAsset: String? = null
)

@Serializable
data class ChoiceDto(val skillId: String, val label: String, val image: String, val audio: String)

@Serializable
data class AttributionsDto(val schemaVersion: Int, val entries: List<AttributionDto> = emptyList())

@Serializable
data class AttributionDto(
    val assetId: String,
    /** image, audio or font. */
    val kind: String,
    /** commissioned, licensed, public-domain, or development. */
    val provenance: String,
    /** Who made it, or where it came from. A person or organisation, not a URL alone. */
    val source: String,
    /** The exact licence name and version, or the contract reference for commissioned work. */
    val licence: String,
    /** Where the proof is kept: a file path, an invoice number, a signed release. */
    val licenceEvidence: String,
    /** The exact credit string, where the licence requires one. Null where it does not. */
    val attributionText: String? = null
)
