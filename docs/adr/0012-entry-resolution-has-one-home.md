# Entry resolution has one home, and a start destination resolver is not it

P2-T10 specifies creating `app/startup/StartDestinationResolver.kt`, covering no profiles, a selected
valid profile, a stale selected profile, a confirmed active checkpoint, a pending unsaved checkpoint
after process death, an invalid lesson id, and an invalid course version.

`navigation/EntryResolver.kt` already answers where the app opens. It is a pure function over a
`ProfileSnapshot`, it is the information architecture's entry-resolution table written out, and
`EntryResolverTest` holds every row of it. The first three cases in that list are already there.

The rest are not entry resolution at all. The information architecture fixes launch as
profile-count based: no profiles opens profile creation, one opens Child Home, more than one opens
the launch picker, and unreadable storage opens recovery. Selecting or creating a profile replaces
the entry destination with Child Home, which becomes the root. A child returns to unfinished work
through Child Home's Continue and the learning path, never by the app launching them into the middle
of a lesson. A second resolver would either restate that table or quietly encode the rule the
architecture rejected, and two functions answering "where does the app open" is how they drift.

What those remaining cases describe is resuming a lesson: which activity a child lands on when they
open one they have already started. That is a question about a lesson, and it belongs with the
lesson, which is where it now lives.

## Consequences

T10 does not create the file its plan names, and its start-resolution cases are split between two
places that already exist: profile cases stay in `EntryResolver`, and checkpoint cases become lesson
resume in `LessonViewModel`. Anyone looking for `StartDestinationResolver` will not find it, which is
why this is written down rather than left as an absence.

If launch should ever open a lesson directly, that is a change to `resolveEntry` and to the
information architecture's table together, deliberately and in one place, rather than a second
component growing its own opinion beside the first.
