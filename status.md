# Development Status

## Project overview

This is an Android application (`com.aistudio.procmanager.tzmqkv`) written in Kotlin with Jetpack Compose. It targets Android API 36 (minimum API 24) and presents device/process telemetry in a process-manager interface.

## Implemented functionality

- Live CPU, memory, process, and network telemetry with a trend chart.
- Running-process table with search, category/system/background filters, sorting, compact mode, detail views, multi-select, and bulk termination actions.
- Process kill history, export, system-information, navigation, and test-task dialogs.
- Network diagnostic UI for speed tests, ping, and traceroute.
- Unit, Robolectric, instrumented, and screenshot-test scaffolding.

## Current repository state

- Branch: `main`, tracking `origin/main`.
- Latest recorded change: `d115c53 feat: add network connectivity check to speed test`.
- The working tree contains pre-existing local changes that were not modified by this status document:
  - Modified: `gradle.properties` (enables Gradle tooling parallel sync).
  - Untracked: `.idea/`, Gradle wrapper files, `gradlew`, and `gradlew.bat`.
- No build or test run was performed as part of this status review.

## Development notes

- The app declares permissions for process queries/termination and network access; behavior requiring privileged Android capabilities should be verified on a device or emulator.
- Firebase AI and App Check dependencies are configured, with missing Google Services configuration set to warn rather than fail.

## Strict Git instructions

**Never ever stage, commit, or push.**

**Never fetch or pull.**

**Never rebase.**
