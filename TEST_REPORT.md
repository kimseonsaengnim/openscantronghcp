# Test Report

## Environment

- Repository started with only `README.md`.
- Build JDK: OpenJDK 21.0.13.
- Gradle wrapper: 8.13.
- Android SDK platform/build tools: API 36 / Build Tools 36.0.0.
- No emulator or physical Android device was connected.

## Results

- `./gradlew testDebugUnitTest :core:test --no-daemon --console=plain` — passed. The app unit-test task has no app-local test sources; the core suite executed 4 tests (3 grading, 1 CSV), with 0 failures and 0 errors.
- `./gradlew assembleDebug --no-daemon --console=plain` — passed. APK generated at `app/build/outputs/apk/debug/app-debug.apk` (12,024,275 bytes).
- `./gradlew lintDebug --no-daemon --console=plain` — passed with 15 non-blocking Gradle/dependency update suggestions; the report is at `app/build/reports/lint-results-debug.html`.
- No connected-device launch or instrumented tests were run because no emulator/device was attached.
- OMR and image-quality functions are not implemented, so scan accuracy is not applicable and no accuracy or dataset-size metrics are claimed.

## Required follow-up

With Android SDK 36 and a compatible JDK configured, rerun:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest :core:test
./gradlew lintDebug
```

Run instrumented tests on an emulator/device when available. OMR target percentages must be measured only after implementing the pipeline and running it against a documented, labeled synthetic and real-photo dataset.
