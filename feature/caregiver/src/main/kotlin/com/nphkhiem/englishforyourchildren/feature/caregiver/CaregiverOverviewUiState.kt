package com.nphkhiem.englishforyourchildren.feature.caregiver

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import com.nphkhiem.englishforyourchildren.domain.model.CoPlayIdea
import com.nphkhiem.englishforyourchildren.domain.model.read

/**
 * One plain-language line about practice.
 *
 * [value] is a string rather than a number because not every summary counts something: the draft's
 * third card names a unit to practise. A number here would force the one summary that is a name
 * into a shape it does not have.
 *
 * [note] is what stops a value reading as a score. The draft writes "Not a test score" under a
 * count for exactly that reason, and the design brief bans ranking language outright.
 */
@Immutable
data class OverviewSummary(val label: String, val value: String, val note: String)

/** The one thing to try away from the television. */
@Immutable
data class CoPlaySuggestion(val title: String, val instruction: String)

/**
 * A content author's idea, read the way the rest of this area is read.
 *
 * The idea arrives carrying both languages, so this is a choice between them rather than a lookup.
 * It sits beside [caregiverText] because it answers the same question that function does, and a
 * caregiver reading a suggestion in one language and its heading in another would be the bug.
 */
@Composable
@ReadOnlyComposable
fun coPlaySuggestion(idea: CoPlayIdea): CoPlaySuggestion {
    val language = LocalCaregiverLanguage.current
    return CoPlaySuggestion(
        title = language.read(idea.title, idea.titleVietnamese),
        instruction = language.read(idea.instruction, idea.instructionVietnamese)
    )
}

/**
 * What there is to say about a profile's practice.
 *
 * A sealed type rather than a list that might be empty, because "no sessions yet" and "nothing this
 * week" are different things to a caregiver and want different words. A list cannot tell them
 * apart, and a boolean beside a list would allow the pair that means neither.
 */
sealed interface OverviewProgress {
    /** The profile exists but has never had a session. */
    data object NewProfile : OverviewProgress

    /** There is practice behind this profile, but none inside the period being shown. */
    data object NothingRecent : OverviewProgress

    /** Practice happened in this period. */
    data class Practiced(val summaries: List<OverviewSummary>, val recentWords: List<String>) :
        OverviewProgress
}

/**
 * Everything the caregiver overview needs to draw itself.
 *
 * There is no action here. Everything a caregiver can press on this screen belongs to the shell
 * around it, and the suggestion is deliberately not pressable: it is a thing to do with a real
 * chair, not a destination.
 */
@Immutable
data class CaregiverOverviewUiState(
    val profileName: String,
    val period: String,
    val progress: OverviewProgress,
    /** Null when no suggestion can be offered, which is the brief's unavailable-content state. */
    val suggestion: CoPlaySuggestion?,
    val pendingSave: Boolean
)
