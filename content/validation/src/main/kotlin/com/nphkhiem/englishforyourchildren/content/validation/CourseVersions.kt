package com.nphkhiem.englishforyourchildren.content.validation

import com.nphkhiem.englishforyourchildren.domain.model.CourseVersion

/**
 * Whether this version comes after [other], comparing dot-separated numbers: `2026.10` after
 * `2026.09`, `1.10.0` after `1.9.0`. A missing part counts as zero. A version that is not numbers
 * is never newer, so it can never replace one that is.
 *
 * Here because both sides must agree on it exactly: a television accepts only a newer version, and
 * the pipeline refuses to publish one a television would ignore.
 */
fun CourseVersion.isNewerThan(other: CourseVersion): Boolean {
    val mine = value.split('.').map { it.toIntOrNull() ?: return false }
    val theirs = other.value.split('.').map { it.toIntOrNull() ?: return true }
    for (index in 0 until maxOf(mine.size, theirs.size)) {
        val difference = mine.getOrElse(index) { 0 } - theirs.getOrElse(index) { 0 }
        if (difference != 0) return difference > 0
    }
    return false
}
