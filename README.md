# English for Your Children

An offline-first Android TV application for Vietnamese children aged 3–5 who are beginning English. The product is free, account-free, advertising-free, and designed for optional caregiver participation.

## Current state

The repository contains the verified Android Studio foundation and a minimal TV launcher screen. Curriculum, profiles, progress storage, playback behavior, and caregiver experiences will be added as test-driven vertical slices after their design artifacts are approved.

## Platform and tooling

- Android TV with `minSdk 28`, `compileSdk 37`, and `targetSdk 37`
- Kotlin, Coroutines, and Flow
- Jetpack Compose with Compose for TV
- MVVM and Clean Architecture boundaries
- Hilt dependency injection
- Room progress storage and Preferences DataStore
- Media3 ExoPlayer integration boundary
- Gradle convention plugins and a centralized version catalog
- JUnit 6 tests named with `givenXXX_whenYYY_thenZZZ`

Android modules use AGP 9 built-in Kotlin. The obsolete `org.jetbrains.kotlin.android` plugin is intentionally not applied.

## Modules

| Module | Responsibility |
| --- | --- |
| `:app` | TV launcher, root navigation, lifecycle, and Hilt composition root |
| `:feature:profiles` | Local child profile creation and selection |
| `:feature:learning` | Learning path, lessons, activities, review, and celebration |
| `:feature:caregiver` | Adult gate, progress summary, settings, and profile management |
| `:ui:tv` | Reusable D-pad, focus, semantics, safe-area, theme, and motion mechanics |
| `:playback` | Media3 playback adapter and lifecycle behavior |
| `:domain` | Pure Kotlin models, contracts, use cases, lesson rules, and progression |
| `:data` | Room, DataStore, repository implementations, mappings, and content parsing |
| `:content:starter` | Versioned packaged curriculum and licensed local media |
| `:test-support` | Deterministic fakes, builders, dispatchers, clocks, and fixtures |

Feature modules depend on domain contracts and capability modules. They never depend on `:data`. The project deliberately has no generic `core`, `common`, or `utils` module.

## Open in Android Studio

1. Install Android Studio with JDK 17 or newer.
2. Install Android SDK Platform 37 and Build Tools 36.0.0 or newer.
3. Open the repository root as an Android Studio project.
4. Let Gradle sync using the checked-in wrapper.
5. Select an Android TV emulator or physical Android TV running API 28 or newer.
6. Run the `app` configuration.

Do not commit `local.properties`; Android Studio creates it for the local SDK path.

## Verification

```shell
./gradlew spotlessApply
./gradlew spotlessCheck lint test assembleDebug bundleRelease
```

Spotless and Android lint are the initial stable static gates. Detekt is intentionally deferred because Detekt 1.23.8 does not support the Kotlin 2.4 and AGP 9.3 toolchain, while Detekt 2 is still prerelease.

## Documentation boundary

Brainstorming, planning, system-design, and handoff documents are intentionally local-only and are not stored in this repository. The repository contains application project files and this contributor-facing README only.

## Privacy boundary

The first release has no accounts, advertisements, analytics, cloud synchronization, microphone access, runtime curriculum API, or child-data telemetry. Lessons and media are reviewed and packaged with the application.
