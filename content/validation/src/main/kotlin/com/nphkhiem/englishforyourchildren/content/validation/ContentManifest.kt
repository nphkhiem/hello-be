package com.nphkhiem.englishforyourchildren.content.validation

import kotlinx.serialization.Serializable

/**
 * What one published course version is made of: the file a television checks for lessons.
 *
 * Published beside a detached signature over its exact bytes (ADR 0016), and read only after that
 * signature has held. The content pipeline writes it and the app reads it, which is why it lives
 * here rather than in either.
 */
@Serializable
data class ManifestDto(
    val schemaVersion: Int,
    val courseVersion: String,
    /** The content schema an app must read to use this version; see [SUPPORTED_SCHEMA_VERSION]. */
    val minAppSchemaVersion: Int,
    val packs: List<PackRefDto>
)

/**
 * One archive of a version's files.
 *
 * Entries are paths inside the course bundle, laid out as the seed in the APK is, so every pack of a
 * version unpacked into one directory is that version's whole bundle. The course-level files travel
 * in a pack of their own, which is why a pack is named by [id] rather than by the unit it carries.
 */
@Serializable
data class PackRefDto(
    val id: String,
    /** Relative to the manifest's own address. */
    val url: String,
    val sizeBytes: Long,
    /** Lowercase hex SHA-256 of the archive exactly as downloaded. */
    val sha256: String
)
