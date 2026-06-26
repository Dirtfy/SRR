# Conversation Log — 2026-06-26g

**Session goal:** Fix "An internal error has occurred[CONFIGURATION_NOT_FOUND]" on Sign Up button click.

## Root Cause

Firebase Auth returns `CONFIGURATION_NOT_FOUND` because:
1. Email/Password sign-in is not enabled in the Firebase Console for project `shared-relative-rank`.
2. More importantly, the debug app had no Firebase emulator bootstrap — `RemoteUserAccountRepository` was calling production Firebase directly. The emulator redirect only existed inside `RemoteEvaluationRepositoryTest.setUpEmulators()` (`@BeforeClass`), invisible to the running app.

## Work Done

### Debator
- Wrote `debat_log/debat_2026-06-26e.md`
- CRITICAL: No `Application` class to call `Firebase.auth.useEmulator()` before any auth operation
- WARNING: Test file uses `"localhost"` as emulator host, which requires ADB reverse on AVD

### Developer — 3 files changed

**`app/build.gradle.kts`** — Added `buildConfig = true` under `buildFeatures`. AGP 8.10.1 does not generate `BuildConfig` by default; required for `BuildConfig.DEBUG` reference in the new Application class.

**`app/src/main/java/com/dirtfy/srr/SRRApplication.kt`** — New file. `Application` subclass that calls `Firebase.auth.useEmulator("10.0.2.2", 9099)` and `Firebase.firestore.useEmulator("10.0.2.2", 8080)` when `BuildConfig.DEBUG` is true. Production (release) builds connect to real Firebase normally.

**`app/src/main/AndroidManifest.xml`** — Added `android:name=".SRRApplication"` to `<application>`. Without this, Android never instantiates `SRRApplication` and the emulator redirect never runs.

### Tester
- Ran `./gradlew testDebugUnitTest --no-daemon`
- Unit tests: all pass (see `testing_log/test_2026-06-26g.md`)

## Files Changed
- `debat_log/debat_2026-06-26e.md` (new)
- `app/build.gradle.kts` (buildConfig = true)
- `app/src/main/java/com/dirtfy/srr/SRRApplication.kt` (new)
- `app/src/main/AndroidManifest.xml` (android:name=".SRRApplication")
- `testing_log/test_2026-06-26g.md` (new)
- `conversation_log/conversation_2026-06-26g.md` (this file)

## Next Step for User

To use Sign Up on an AVD: start the Firebase Local Emulator before launching the app:
```
firebase emulators:start --only auth,firestore
```
The debug app will now connect to the emulator at `10.0.2.2:9099` (Auth) and `10.0.2.2:8080` (Firestore) automatically.

For a **physical device**, run ADB reverse first:
```
adb reverse tcp:9099 tcp:9099
adb reverse tcp:8080 tcp:8080
```
Then the app can use `localhost` internally (but the current app is hardcoded to `10.0.2.2` — for physical devices, a `BuildConfig` field override would be the proper long-term fix).
