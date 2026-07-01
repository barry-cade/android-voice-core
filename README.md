# android-voice-core-stt

Android STT subsystem split into:

- `stt`: production Android library module (AAR)
- `app`: demo harness that consumes `stt`

## STT Public API Policy

The `stt` module exposes a strict, versioned public surface.
Only these types are allowed as top-level public API:

- `SpeechToText`
- `SttConfig`
- `AudioCapture`
- `WhisperBridge`

All other implementation details must stay non-public.
This includes helper classes, JNI plumbing, native context/session handling, threading helpers, and any experimental flags.

## API Verification

The `stt` module enforces API shape checks via:

- Gradle task: `:stt:checkSttApiSurface`
- Lifecycle wiring: `:stt:check` depends on `checkSttApiSurface`

Run these before merging:

```bash
./gradlew :stt:check :stt:testDebugUnitTest :stt:assembleDebug :app:assembleDebug
```

Windows PowerShell:

```powershell
.\gradlew.bat :stt:check :stt:testDebugUnitTest :stt:assembleDebug :app:assembleDebug
```

## Notes for Contributors

- Keep app module usage at API level (`SpeechToText` and `SttConfig`) rather than direct native-handle flows.
- Keep JNI signatures aligned with package `dev.barrycade.voicecore.stt`.
- Avoid broadening public API without explicit versioning and review.
