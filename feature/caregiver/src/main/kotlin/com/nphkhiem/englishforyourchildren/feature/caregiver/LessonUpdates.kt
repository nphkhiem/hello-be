package com.nphkhiem.englishforyourchildren.feature.caregiver

import kotlinx.coroutines.flow.Flow

/**
 * New lessons, as the caregiver area can see and ask for them.
 *
 * Declared here and implemented by the app, because downloading lives in the data layer and a
 * feature module may not reach into it. The caregiver area only needs to know where things stand
 * and how to ask for the next step.
 */
interface LessonUpdates {
    /** False when this build has nowhere to get lessons from, and the Lessons group is not shown. */
    val offered: Boolean

    val state: Flow<TaskState>

    /** Looks for something newer than what is on this television. */
    suspend fun check()

    /** Fetches what [check] found. */
    suspend fun download()
}
