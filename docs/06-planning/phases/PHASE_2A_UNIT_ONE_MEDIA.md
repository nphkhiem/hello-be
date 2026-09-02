# Phase 2A: Unit One Media Production Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans` and `android-build`.
> Content tasks additionally require the project owner's approval of every packaged asset before it
> is committed. Delegation requires explicit project-owner authorization.

**Goal:** Give unit one the media it already refers to, so that Hello Bé speaks and shows pictures
rather than printing words at a child who cannot read.

**Numbering:** 2A rather than a renumbering of Phase 3 onwards. It sits between M1 and Phase 3
because Phase 3's content tickets assume media production is a solved problem, and every existing
cross-reference to P3 and beyond stays valid.

**Architecture:** No runtime change. Assets are produced outside the app, packaged into
`:content:starter`, resolved by the `MediaAssetLocator` seam that shipped in P2-T7, and validated
before packaging by `CurriculumValidator`. The app gains no network permission and no new dependency.

**Spec:** `docs/03-design-brief/CONTENT_ID_REGISTRY.md` for the inventory and the naming grammar,
`docs/03-design-brief/ATTRIBUTION_LEDGER.md` for what every asset owes, and
`docs/03-design-brief/DESIGN_BRIEF.md` sections on audio and visual direction.

## What is actually missing

Every one of them. `content/starter/src/main/assets` holds three JSON files and nothing else.

| Kind | Count | Ids |
| --- | ---: | --- |
| Word illustrations | 16 | `img-eyes` through `img-teeth` |
| English word recordings | 16 | `aud-en-eyes` through `aud-en-teeth` |
| Letter-sound recordings | 8 | `aud-en-letter-e`, `-m`, `-h`, `-f`, `-b`, `-t`, `-a`, `-k` |
| Prompt recordings | 4 | `aud-en-prompt-where-is`, `-find-the`, `-say-with-me`, `-which-one-starts-with` |
| Vietnamese support recordings | 3 | `aud-vi-help-look-at-the-picture`, `-press-the-one-you-hear`, `-lets-try-together` |

Forty-seven assets, of which thirty-one are audio.

## Global constraints

- **No runtime fetching.** The app declares no permissions at all, "the lesson works without
  internet permission" is a P2-T4 acceptance criterion, and the profile screen tells a caregiver the
  child's data is stored only on this TV. Open sources may produce an asset; they may not be reached
  from a child's lesson.
- **Nothing ships without a ledger row.** The registry says so, and `attributions.json` is empty on
  purpose so that a release is blocked until it is not.
- **A placeholder says it is one.** Synthesized or provisional assets carry that in the ledger. The
  failure this avoids is a pilot recording quietly becoming the shipped voice.
- **Ids are already fixed.** The registry names every asset. Production supplies files for those
  names and never invents new ones.
- **One art direction.** The design brief names illustration inconsistency as the main risk of the
  chosen visual direction. Sixteen pictures from sixteen hands is that risk, realised.

---

### P2A-T1: Prove the media path with one recording and one picture

**Scope:** S
**Dependencies:** M1

Produce exactly one real `aud-en-eyes` and one real `img-eyes`, package them, and see them on a
television. Everything else in this phase is repetition; this is the part that can be wrong.

- [ ] Fix the image convention beside the audio one: `media/image/<assetId>.webp`, matching
      `media/audio/<assetId>.m4a` from P2-T7. A directory per kind, because ids are opaque.
- [ ] Package both assets and add their two ledger rows.
- [ ] Open lesson one on a device.

**Acceptance criteria:**

- [ ] The first question speaks, and its eyes card shows a picture rather than the word "eyes".
- [ ] `audioAvailable` stays true for that activity, so the caption and the unscored skip do not
      appear.
- [ ] Nothing else in the lesson changes behaviour, because nothing else has media yet.

---

### P2A-T2: Build ADR 0004 before the rest of the audio lands

**Scope:** M
**Dependencies:** P2A-T1
**Status: already built.** Checked on 2 September 2026 and found complete, with a test behind every
clause. The ticket was written believing ADR 0004 was approved and unbuilt. It was approved and
built, in pieces, across the design-build series and P2-T7, and nobody closed the ticket.

Tracked as issue 01 from the P2-T7 branch series. That issue is closed by this finding, not by a
change.

- [x] `LessonSessionState.soundingPrompt` says which recording is sounding, and null is silence
      rather than a flag beside the asset. `LessonReducer` starts it in `ask` and clears it in
      `promptFinished`, which compares what actually ended against what is sounding so a support
      phrase cannot answer for the question.
- [x] `LessonViewModel` routes `PlaybackEvent.Completed` to `reportPromptFinished`.
- [x] `answerAvailability` returns `DISABLED` while the phase is `PROMPTING`, and
      `HelloBeAvailability.isFocusable` is `this != DISABLED`, so the answers leave the focus order
      rather than sitting there inert. `lessonFocusTarget` rests focus on replay.
- [x] Focus stays where it is when the prompt ends. `StorybookScaffold` claims entry focus in a
      `LaunchedEffect(Unit)` keyed to the composition, so a state change mid-activity cannot reclaim
      it. This is the alternative ADR 0004 rejected, and it is refused by construction.

**Acceptance criteria:**

- [x] A test proves a child cannot answer before the prompt has finished:
      `LessonReducerTest.givenTheQuestionIsStillSounding_whenTheChildAnswers_thenNothingIsRecorded`,
      with `givenPipIsStillAskingForTheWord_whenTheChildSaysTheyHaveFinished_thenNothingIsRecorded`
      covering the one family that has no answer to choose. Above it,
      `LessonRulesTest.givenThePromptIsPlaying_whenAnswerAvailabilityIsRead_thenAnswersAreOutOfTheFocusOrder`
      and `LessonScaffoldTest.givenThePromptIsPlaying_whenTheStageAppears_thenFocusRestsOnReplay`
      prove the child is never offered the press in the first place, and
      `StorybookScaffoldTest.givenChildMovedFocus_whenStageStateChanges_thenEntryFocusIsNotReclaimed`
      proves focus is not yanked when the prompt ends.
- [x] Reduced motion and the audio-unavailable path are unchanged. The unavailable path has its own
      tests, including `givenAQuestionIsSounding_whenTheLessonLearnsItHasNoSound_thenNothingIsSoundingAnyMore`.

**What this changes for the rest of the phase.** P2A-T3 lists T2 as its dependency, so recordings
are no longer blocked. The reason T2 was placed before the audio still holds and is now satisfied:
the day prompts play, a child cannot answer before hearing the question.

---

### P2A-T3: Produce the thirty-one recordings

**Scope:** M
**Dependencies:** P2A-T2

**Route, pending the owner's decision:** locally synthesized speech for the pilot. It is licence
clean, needs no network, gives one consistent voice across all thirty-one files, and can be
regenerated. It is explicitly not the shipping voice: the brief asks for warm human audio, and a
synthetic Pip is a placeholder that must be replaced before release.

- [ ] Generate all thirty-one to one specification: mono, AAC-LC in MP4, one loudness target.
- [ ] Ledger rows marking every one synthesized and provisional.
- [ ] Vietnamese support phrases reviewed by a native speaker before packaging, synthesized or not.

**Acceptance criteria:**

- [ ] `CurriculumValidator` reports zero missing audio for unit one.
- [ ] A lesson can be completed by listening and answering, with the unscored skip never offered.
- [ ] Every recording is intelligible on television speakers at a normal volume.

---

### P2A-T4: Produce the sixteen illustrations

**Scope:** L
**Dependencies:** P2A-T1

The hard one, and the one this plan does not pretend is cheap.

**Recommended route:** commission all sixteen from one illustrator against the Storybook Stage
direction. Open-licensed images are rejected as a shipping route: sixteen pictures from sixteen
artists is precisely the inconsistency the brief names as the main risk of this visual direction,
and for a pre-reader the picture is the content rather than the decoration.

If the pilot needs pictures before a commission lands, package obviously-provisional art, ledger it
as provisional, and treat replacing it as a release blocker rather than a nicety.

**Acceptance criteria:**

- [ ] Sixteen images, one art direction, legible at three metres on a television.
- [ ] Each one readable by a three-year-old without its label.
- [ ] `CurriculumValidator` reports zero missing images for unit one.

---

### P2A-T5: Make the attribution ledger the release gate it was designed to be

**Scope:** S
**Dependencies:** P2A-T3, P2A-T4

- [ ] `attributions.json` carries a row per packaged asset: id, source, licence, author, and whether
      it is provisional.
- [ ] A test asserts every packaged asset has a row and every row points at a packaged file.
- [ ] The existing test asserting the ledger is honestly empty is inverted rather than deleted, so
      the reason it existed stays in the history.

**Acceptance criteria:**

- [ ] A release build fails while any packaged asset lacks a row.
- [ ] A release build fails while any row is marked provisional.

---

### P2A-T6: Re-verify everything that was only true because there was no sound

**Scope:** M
**Dependencies:** P2A-T3

The consequence that is easiest to miss. A great deal of current behaviour, and several passing
tests, exist because `audioAvailable` is false for every session.

- [ ] `FirstLessonJourneyTest` walks the lesson by answering rather than by taking the unscored
      skip, which it currently uses on all six activities because skipping is the only way through.
- [ ] Captions no longer appear by default; the caption setting decides instead.
- [ ] The support ladder is reachable for the first time: a wrong answer is a supportive retry, not
      an unscored skip.
- [ ] Replay is enabled rather than announced as unavailable.

**Acceptance criteria:**

- [ ] The audio-unavailable path still works, proven by a test that removes one asset.
- [ ] No test asserts absent media as though it were the product's intended state.

---

## Phase 2A exit gate

- [ ] P2A-T1 through P2A-T6 are merged.
- [ ] Unit one packages forty-seven assets and validation reports zero missing, duplicate, dangling
      or unlicensed references.
- [ ] A child can complete lesson one by listening, with no caption and no skip offered.
- [ ] A child can still complete it when a recording is missing, by the unscored skip.
- [ ] Every packaged asset has a ledger row, and any provisional row blocks a release build.
- [ ] The full repository quality gate passes.
