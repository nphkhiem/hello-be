package com.nphkhiem.englishforyourchildren.content

import com.nphkhiem.englishforyourchildren.data.pack.ContentUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * New lessons fetched while a child is busy with one, so the next is already here when they want it.
 *
 * Adults tolerate waiting and children do not, so a child never waits on the network: the fetch
 * runs behind a lesson they are already in, says nothing whatever happens, and anything it brings
 * goes live only later, when no lesson is open.
 *
 * Its own scope rather than a screen's, because the point is to keep going after the lesson that
 * started it has closed. [updater] is null in a build with nowhere to fetch from.
 */
class BackgroundLessons(private val updater: ContentUpdater?) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var running: Job? = null

    /** Starts a quiet refresh, unless one is already under way. */
    fun prefetch() {
        val updater = updater ?: return
        if (running?.isActive == true) return
        running = scope.launch { updater.refresh() }
    }
}
