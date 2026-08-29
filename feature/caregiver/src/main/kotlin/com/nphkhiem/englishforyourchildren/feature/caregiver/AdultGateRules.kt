package com.nphkhiem.englishforyourchildren.feature.caregiver

/** Below this a challenge cannot offer a wrong answer to rest on, so it is not a gate. */
private const val MIN_ANSWERS = 2

/**
 * Whether a challenge can be presented at all.
 *
 * A gate has to fail closed. A challenge with one answer, or with a correct index pointing outside
 * its own answers, cannot promise that focus starts somewhere harmless, so it is not shown as a
 * gate at all rather than shown as one that opens on a single press.
 */
internal fun isChallengeUsable(challenge: GateChallenge): Boolean =
    challenge.answers.size >= MIN_ANSWERS &&
        challenge.correctIndex in challenge.answers.indices

/**
 * Where entry focus goes: the first answer that is not the correct one.
 *
 * This is the whole of the protection. A child pressing Select without moving is pressing a wrong
 * answer by construction, and because the challenge rotates on every wrong answer, walking the row
 * and pressing each one in turn gains nothing either: the next challenge puts the correct answer
 * somewhere else and focus lands off it again.
 *
 * Null when the challenge cannot supply a safe place to stand, which the screen renders as no
 * answers at all.
 */
internal fun gateFocusIndex(challenge: GateChallenge): Int? {
    if (!isChallengeUsable(challenge)) return null
    return challenge.answers.indices.first { it != challenge.correctIndex }
}
