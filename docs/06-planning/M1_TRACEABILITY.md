# M1 traceability record

What the Phase 2 exit gate asks for, and what actually stands behind each line. Written at the close
of P2-T10.

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
| Process-death journey | None | **Not met.** See below |
| Unsaved-state journey | `LessonSaveFailureJourneyTest`: a refused write says "Not saved yet" and does not move the child on. The refusal is injected at the DAO, so the repository, reducer and screen are all production code | Met |
| Repeated-input journey | `LessonReducerTest` and `LessonViewModelTest` both cover a duplicate press being answered once. There is no journey-level equivalent | **Partial.** Covered below the UI, not through it |
| API 28 and current TV device matrix | Current TV met on Television_4K at API 36. API 28 not met | **Partial.** ADR 0013 |
| Full phase quality gate | `spotlessCheck`, `build`, `lint` green with no new warnings | Met |

## The two gaps, stated plainly

**Process death.** Journeys run against an in-memory database so each test starts on a television
with nothing on it. An in-memory database dies with the process, so the one thing a process-death
test needs to survive is the thing this setup destroys. Doing it properly means on-disk storage with
a file per test and a teardown, and a way to drop the whole object graph rather than recreating an
activity, which keeps its ViewModels. It was left undone rather than approximated with
`ActivityScenario.recreate()`, which would have passed while proving nothing.

The behaviour underneath it is not untested: resume reads the checkpoint back from storage and is
covered at every level below the journey.

**API 28.** Not installable on this hardware. ADR 0013 has the detail.

## Not part of the gate, but true of the build M1 ships

One unit of a planned twelve. None of the roughly forty referenced media assets exist, so every
lesson runs silent and answer cards show written words to a pre-reader. The attribution ledger is
empty, deliberately, and stops a release.

Seven destinations have no live screen: the adult gate, everything behind it, both destructive
confirmations, and free play. Pressing them reaches a resting-page recovery rather than the screen.
That is Phase 3.
