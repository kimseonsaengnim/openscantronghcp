# Project Plan

## Goal and constraints

Build an offline Android OMR grader with Room as the source of truth, non-destructive recognition/answer correction, printable sheets, scan archiving, and reproducible exports. The starting repository contained only a one-line README and no Gradle project, Android SDK, or Android tooling. This plan records the staged implementation and avoids claiming unavailable features as complete.

## Architecture

- Android application module using Kotlin, Jetpack Compose/Material 3, Navigation Compose, Hilt, Room, and coroutines, plus a separate plain Kotlin `core` module.
- Keep grading rules and CSV serialization in `core` as pure Kotlin components with local unit tests.
- Room is authoritative for exams, versioned answer keys, students, and submissions.
- Keep scans, PDFs, and exports local; use SAF for teacher-selected exports.
- Use explicit Room migrations as the schema evolves; never use destructive migration fallback.

## Phases and completion log

| Phase | Scope | Status |
|---|---|---|
| 1. Inspect and plan | Inspect starter repository/build environment and define phased delivery | Complete |
| 2. Foundation | Android/Compose/Hilt/Room foundation, local repositories, error-safe UI | Complete |
| 3. Exams and keys | Exam creation, key validation/versioning, grading rules | MVP subset complete; editing/archive and advanced scoring pending |
| 4. Sheet generator | A4/Letter printable sheets, normalized layout, registration markers, QR | Not started |
| 5. OMR | OpenCV pipeline and synthetic-image recognition suite | Not started |
| 6. Camera/import | CameraX, quality analysis, gallery import | Not started |
| 7. Verification | Persistent machine predictions and non-destructive correction UI | Not started |
| 8. Archive and CSV | Individual PDF archive, automatic wide/audit datasets, SAF export | CSV foundation only |
| Priority 2 | Roster import, batch recovery, archive manager, reports and backup | Not started |
| Priority 3 | Weighted scoring, item analysis, calibration and advanced options | Not started |

## MVP delivery in this change

- Create exams and persist them in Room.
- Enter a complete answer key; activating a changed key creates a new version.
- Record manually transcribed submissions without overwriting prior submissions.
- Grade submissions using a deterministic domain service and pin them to the key version used.
- Export one examination's submissions in wide CSV format with RFC 4180 escaping, through SAF.
- Add unit tests for scoring and CSV serialization.

## Decisions and limitations

- Use UUID primary keys and immutable answer-key versions. Changing an active key does not silently rewrite prior scores.
- Multiple-mark answers and missing answers are represented distinctly; this MVP's manual form accepts a single label or `MULTIPLE`, `BLANK`, `INVALID`, or `PENDING_REVIEW`.
- The Android SDK was provisioned locally to build and lint the APK. No emulator or physical device is attached, so launch and instrumented-device validation remain pending.
- The target request describes a production-sized application. The remaining camera, CV, PDF archive, dataset synchronization, backup, reporting, and 24-screen workflow must be delivered in subsequent phases and are explicitly not represented by placeholders.

## Verification commands

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest :core:test
./gradlew lintDebug
./gradlew connectedDebugAndroidTest
```

Run connected tests only with a configured emulator/device. Record observed output and environment in `TEST_REPORT.md`; do not infer accuracy targets from synthetic assumptions.
