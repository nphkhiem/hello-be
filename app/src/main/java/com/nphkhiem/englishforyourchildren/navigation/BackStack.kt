package com.nphkhiem.englishforyourchildren.navigation

/**
 * What a Back press does to the stack.
 *
 * Screens that answer Back themselves, the lesson with its stop-for-now dialog and the celebration
 * with its immediate return, register their own handlers and never reach this. What is left is the
 * host's share: pop, or leave the app.
 */
sealed interface BackOutcome {
    /** Remove the top destination and show the one under it. */
    data object Pop : BackOutcome

    /** Nothing is underneath. Back leaves Hello Bé for the Android TV launcher. */
    data object ExitApp : BackOutcome
}

/**
 * Back at the root exits, and Back anywhere else goes back one.
 *
 * The information architecture says the same thing four times over: profile create, the launch
 * picker and child home all "exit app when root". Rather than each of those knowing it is root,
 * root is simply the destination with nothing beneath it.
 */
internal fun backOutcome(stack: List<HelloBeKey>): BackOutcome =
    if (stack.size > 1) BackOutcome.Pop else BackOutcome.ExitApp

/**
 * The stack after choosing or creating a profile.
 *
 * Child home replaces everything rather than being pushed onto it. The information architecture is
 * explicit that child home becomes the root so Back always leaves the app, which would not be true
 * if the picker stayed underneath it: a child pressing Back would land on a screen asking them to
 * choose a profile they had already chosen.
 */
internal fun afterProfileChosen(profileId: ProfileId): List<HelloBeKey> =
    listOf(HelloBeKey.ChildHome(profileId = profileId))

/**
 * The stack after the caregiver session ends.
 *
 * Everything behind the gate is dropped and the child surface that opened it becomes the root
 * again. The session is foreground-scoped, so this runs when the app leaves the foreground as well
 * as when a caregiver returns deliberately, and in both cases nothing caregiver-shaped may be left
 * on the stack for a child to press Back into.
 */
internal fun afterCaregiverSessionClosed(
    profileId: ProfileId?,
    returnTarget: ChildReturnTarget
): List<HelloBeKey> {
    if (profileId == null) {
        return listOf(HelloBeKey.ProfilePicker(mode = ProfilePickerMode.Launch))
    }
    val home = HelloBeKey.ChildHome(profileId = profileId)
    return when (returnTarget) {
        ChildReturnTarget.CHILD_HOME -> listOf(home)

        ChildReturnTarget.LEARNING_PATH ->
            listOf(home, HelloBeKey.LearningPath(profileId = profileId))

        ChildReturnTarget.FREE_PLAY -> listOf(home, HelloBeKey.FreePlay(profileId = profileId))
    }
}

/**
 * The stack after a lesson finishes and its celebration is done with.
 *
 * The celebration carries where it came from, so completion returns to its origin without a route
 * string or a callback, which the acceptance checklist asks for by name.
 */
internal fun afterCelebrationDone(
    profileId: ProfileId,
    returnTarget: LessonReturnTarget
): List<HelloBeKey> {
    val home = HelloBeKey.ChildHome(profileId = profileId)
    return when (returnTarget) {
        LessonReturnTarget.CHILD_HOME -> listOf(home)

        LessonReturnTarget.LEARNING_PATH ->
            listOf(home, HelloBeKey.LearningPath(profileId = profileId))
    }
}
