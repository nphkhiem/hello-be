package com.nphkhiem.englishforyourchildren.data.pack

import com.nphkhiem.englishforyourchildren.domain.model.CourseVersion
import com.nphkhiem.englishforyourchildren.domain.repository.CurriculumRepository
import com.nphkhiem.englishforyourchildren.domain.result.DomainResult
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Puts the newest downloaded course live, and clears away the ones it replaces.
 *
 * Only the caller knows when that is safe: a session pins its course version, and two versions must
 * never mix within one, so this runs only when no child is in a lesson (spec section 8). It reads
 * what is on disk rather than what a download remembers, so a pack staged before the television
 * was switched off still goes live at the next safe moment.
 *
 * It needs nothing from the network. A build with nowhere to fetch from simply never has a pack on
 * disk, and this does nothing.
 */
class PackActivation @Inject constructor(
    private val packs: ContentPackStore,
    private val curriculum: CurriculumRepository
) {
    /** The version now live, or null when nothing newer was waiting. */
    suspend fun activateNewest(): CourseVersion? = withContext(Dispatchers.IO) {
        val live = (curriculum.observeCourse().first() as? DomainResult.Success)?.value?.version
        val newest = packs.versionsOnDisk()
            .filter { live == null || it.isNewerThan(live) }
            .reduceOrNull { best, next -> if (next.isNewerThan(best)) next else best }
            ?: return@withContext null
        packs.activate(newest)
        packs.evictAllBut(newest)
        newest
    }
}
