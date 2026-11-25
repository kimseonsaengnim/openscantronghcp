# Open Scantron GHC

Offline-first Android Scantron/OMR grading application.

## Current status

This repository is at the foundation/MVP stage. The working implementation includes local exam and answer-key persistence, manual submission grading, and RFC 4180 CSV export. Camera capture, OpenCV recognition, printable templates, PDF archives, roster import, and the remaining classroom workflows are not implemented yet; see [PROJECT_PLAN.md](PROJECT_PLAN.md) and [TEST_REPORT.md](TEST_REPORT.md).

## Requirements

- Android Studio with Android SDK 36 installed
- JDK 17 or newer

Open the repository in Android Studio or build from the command line:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest :core:test
```

The app stores its data in a local Room database. It does not require an account, network access, or external service. CSV exports use Android's document picker so the teacher chooses where to save each file.

## MVP workflow

1. Create an examination and choose its question count and answer labels.
2. Enter and save its answer key.
3. Add a manually transcribed submission, then review the computed score.
4. Export the examination's submissions as a UTF-8 CSV.

This manual-entry path is an interim workflow and is not a substitute for camera/OCR scanning.
