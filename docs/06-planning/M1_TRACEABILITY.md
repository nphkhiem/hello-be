# M1 traceability record

What the Phase 2 exit gate asks for, and what actually stands behind each line. Written at the close
of P2-T10.

Updated 2 September 2026 where a line has since been closed; the measurements below are the
P2-T10 ones and are not restated.

Measured on Television_4K (Android TV, API 36) with 402 unit tests and 201 instrumented tests:
`:feature:learning` 135, `:app` 30, `:data` 29, `:playback` 7.

## Exit gate

| Criterion | Evidence | Status |
| --- | --- | --- |
| P2-T1 through P2-T10 are merged | PRs #35 to #43 for T1 to T6 and T8 to T9, #44 for T7, #47 to #50 for T10 | **Partial.** T10 is short of the process-death journey and the API 28 leg, both below |
| One approved lesson is complete using only D-pad input | `FirstLessonJourneyTest`, which creates a child, answers all six activities and asserts the celebration and the stored completion | Met |
| Room migration 1 to 2 and checkpoint transaction tests pass | `Migration1To2Test`, `ProgressTransactionTest`, 29 instrumented tests in `:data` | Met |
| Playback fallback journey | `Media3PlaybackControllerTest` for the missing-asset failure, and `FirstLessonJourneyTest`, which walks the whole lesson on the unscored skip because no recording exists. That is the fallback, exercised end to end rather than simulated | Met |
| Back journey | `LessonRecoveryJourneyTest`: Back opens the stop question, and "Keep learning" returns to the same activity | Met |
| Resume journey | `LessonRecoveryJourneyTest`: stopping part way and reopening lands on the next question, not the first. Behaviour added in #47 | Met |
| Process-death journey | `ProcessDeathJourneyTest`: a confirmed checkpoint survives the Activity, its ViewModels and the open database all going away; a refused write does not | **Met, with one limit named below** |
| Unsaved-state journey | `LessonSaveFailureJourneyTest`: a refused write says "Not saved yet" and does not move the child on. The refusal is injected at the DAO, so the repository, reducer and screen are all production code | Met |
| Repeated-input journey | `RepeatedInputJourneyTest`: two presses record one attempt and move the child one question, whether the second lands while the first is being written down or after the lesson has moved on | Met. The defect this found is closed, below |
| API 28 and current TV device matrix | Current TV met on Television_4K at API 36. API 28 not met | **Partial.** ADR 0013 |
| Full phase quality gate | `spotlessCheck`, `build`, `lint` green with no new warnings | Met |

## The two gaps, stated plainly

**Process death.** Closed on 2 September 2026. What the original entry asked for is what was
built: on-disk storage with a file per test and a teardown, and a drop that takes the whole Activity
and its ViewModels rather than recreating one. `ActivityScenario.recreate()` is still not used, for
the reason first written here: it hands the new Activity the same `ViewModelStore`, so it would have
passed while proving nothing.

Two tests, because the milestone's claim has two halves. Confirmed work comes back: a question
answered and written down leaves the child on the next one after everything holding their position
in memory has gone. Pending work does not: a write `RefusingProgressDao` refuses leaves them on the
question they had already answered, so nothing resurrects progress the screen said was not saved.

**The limit, stated rather than left implied.** True process death cannot be exercised from an
instrumented test, because the runner lives in the process and dies with it. The Hilt component and
its singletons therefore survive the drop. That makes this stricter rather than weaker in the place
that matters: the repositories are bound `@Singleton`, so one caching the child's position would
pass a test that rebuilt everything and fails this one. What it does not cover is a genuinely cold
start with an empty process.

**API 28.** Not installable on this hardware. ADR 0013 has the detail.

## What writing the repeated-input journey found, and what closed it

Two rapid Select presses on an answer recorded two attempts, on two different activities, and moved
the child two questions on. Their stored history then said they answered a question that was never
put to them.

The guard that existed was not broken. `LessonUiAction.AnswerChosen` carries the activity number the
screen was showing, and a press whose lambda predates the advance is rejected. But the first press
advances the lesson, recomposition runs, and the second press arrives through the new card's own
lambda carrying the new number, which is a legitimate answer for the new question by every check the
app had. Both presses are real, both are aimed at what was genuinely on screen, and no ordering rule
can separate them: the second one is not stale.

Closed on 2 September 2026 with the one signal that does separate them. A question is held closed
for the first three hundred milliseconds it is on screen — the same window Android calls a double
tap — and a press landing inside it does not answer, because the child had not yet looked at what
they would have been answering. It lives in `LessonViewModel`, which is where time belongs; the
reducer stays a function of state and action. The two other presses that record an attempt, the
unscored skip and finishing a repetition, are held to the same moment for the same reason.

`JourneyDriver` now waits where a child would look, since a test drives a television faster than any
child can. The one place it deliberately does not is between the halves of `doublePress`, because
the missing gap is what makes that one gesture.

What is still not established is system key repeat: a held key may or may not fire a click per
repeat, and nothing here has measured it. The window closes the double press either way, but how a
held remote behaves on real hardware is unmeasured rather than known.

## Not part of the gate, but true of the build M1 ships

One unit of a planned twelve. None of the roughly forty referenced media assets exist, so every
lesson runs silent and answer cards show written words to a pre-reader. The attribution ledger is
empty, deliberately, and stops a release.

Destinations with no live screen, as Phase 3 works through them. Free play went live in #61. The
adult gate and caregiver settings went live in P3-T7. What remains is the caregiver overview,
profile management and both destructive confirmations, which are P3-T8. Pressing those still
reaches a resting-page recovery rather than the screen.

Solving the gate lands on the overview, which is where the information architecture puts a
caregiver who has just come through the door. It briefly landed on settings instead, while the
overview had no ViewModel and would have met them with a recovery panel. P3-T8 moved it back.

**The overview shows two summaries where the approved draft has three, and no co-play suggestion.**
The third summary names a unit to come back to and nothing derives one yet; the suggestion has no
source at all. `CaregiverOverviewUiState` allows a null suggestion and calls it the brief's
unavailable-content state, so this is the screen being honest rather than incomplete, but it is not
the finished screen and a suggestion source is still owed.
