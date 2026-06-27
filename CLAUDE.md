# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew assembleDebug          # build debug APK
./gradlew assembleRelease        # build release APK
./gradlew test                   # run unit tests (JUnit 4 only)
./gradlew connectedAndroidTest   # run instrumented tests (requires connected device/emulator)
```

Toolchain: AGP 8.10.1, Kotlin 2.0.21, compileSdk 36, minSdk 24. All versions are centralized in `gradle/libs.versions.toml` (version catalog). Build files use Kotlin DSL throughout.

## Architecture

**Single-module, UI-only prototype.** No networking, database, or DI framework (no Retrofit, Room, Hilt, or Koin). All data is hardcoded mock data.

**Pattern: MVVM with MVI-style UiState**

Each screen owns a single `StateFlow<XUiState>` (immutable data class). The UI observes this flow via `collectAsStateWithLifecycle()`. Navigation state (selected item, open detail, active popup) is stored inside UiState — there is no NavController back stack for intra-screen navigation.

**Layer vocabulary (custom naming):**
- `ui/window/` — Android framework layer: `MainActivity`, Fragment subclasses
- `ui/performer/` — Pure Compose UI layer: screens, ViewModels, UiState, theme

**Navigation is a two-tier hybrid:**

1. **Fragment-level (top-level screens):** `MainActivity` (FragmentActivity) manages a `FrameLayout` created programmatically (ID `CONTAINER_ID = 10001`) and routes between `LoginFragment`, `UserFragment`, and `CompilationFragment` via `FragmentManager.commit { replace() }`. No NavController involved.

2. **Intra-screen (list → detail → popup):** Handled purely in Compose using `when` over UiState fields. No NavHost, no back stack — state drives what is rendered.

`BaseFragment` is an abstract Fragment that all tab screens extend. It inflates a `ComposeView`, applies `SRRTheme`, and renders `SRRBaseScreen` (a Scaffold with `CenterAlignedTopAppBar` and `NavigationBar`). Subclasses provide title, back-button visibility, and body content.

`LoginFragment` does **not** extend `BaseFragment` (no bottom nav needed).

**Stateless/stateful Composable split:**  
Each screen follows the pattern of a stateful composable (holds ViewModel, e.g. `LoginScreen`) that delegates to a stateless composable (accepts lambdas, e.g. `LoginContent`) — enabling `@Preview` without ViewModel dependencies.

## Key Packages

```
app/src/main/java/com/dirtfy/srr/
  ui/window/
    MainActivity.kt              # Single Activity; Fragment host; top-level nav
    component/
      BaseFragment.kt            # Abstract Fragment; Scaffold shell
      LoginFragment.kt           # Login screen; does not extend BaseFragment
      UserFragment.kt            # "My" tab; owns UserViewModel
      CompilationFragment.kt     # "Result" tab; owns CompilationViewModel

  ui/performer/
    base/                        # Shared composables (Screen scaffold, nav Item, theme)
    login/                       # Login screen (UiState, ViewModel, Screen)
    user/                        # "My" tab: item grid + feature ratings
      items/                     # Item grid + detail
      features/                  # Feature list + detail + popup
    compilation/                 # "Result" tab: 3-mode view (ITEMS, FEATURES, MAP)
      items/                     # Compilation item grid + detail
      features/                  # Aggregated feature list + detail
      map/                       # Canvas scatter plot with popup
```

## Notable Patterns

- **No XML layouts.** `MainActivity` creates its container programmatically. All Fragments use `ComposeView`. Zero XML layout files exist.
- **`navigation-compose` is declared but unused.** The dependency exists in `build.gradle.kts` but no `NavHost` or route definitions appear in code — it is a future intention.
- **Android system drawables as placeholders** (`android.R.drawable.ic_menu_agenda`, etc.) are used everywhere in place of real assets.
- **Dynamic color (Material You)** is enabled by default in `SRRTheme`; falls back to a purple/pink palette on pre-Android 12 devices.
- **`android.nonTransitiveRClass=true`** is set in `gradle.properties` — each library's R class is scoped to its own namespace.
- **Tests are scaffolding only.** `ExampleUnitTest` and `ExampleInstrumentedTest` are the unmodified Android Studio defaults. No real test coverage exists.

---

## Collaboration Rules (User-Defined)

These rules were set explicitly by the user and must be followed in every session without being asked.

### Teammate Roles

Every meaningful piece of work passes through three logical roles. Claude plays all of them, but in sequence — never skipping or merging steps.

| Role | Responsibility |
|------|---------------|
| **Debator** | Reviews completed code. Finds problems actively (not passively). Categorizes findings as CRITICAL / WARNING / MINOR. Writes `debat_log/debat_YYYY-MM-DD.md`. |
| **Developer** | Implements features and bug fixes. Must fix every CRITICAL finding before committing. |
| **Tester** | Gates commits with unit tests; gates pushes with instrumented tests. Writes `testing_log/test_YYYY-MM-DD.md`. Approves or rejects each gate. |

### Workflow — Required Order

```
Debator reviews → Developer fixes CRITICALs
→ Tester: unit tests pass → commit (locally) → write all logs
→ Tester: instrumented tests pass → push
```

1. **Before every commit:** Debator reviews the work and writes the debat log. Developer applies every CRITICAL fix.
2. **Commit gate (unit tests):** Tester runs `./gradlew testDebugUnitTest` and writes the test log. Commit locally only if unit tests pass. Never commit when unit tests fail.
3. **Commit threshold:** Commit locally when >100 lines have changed or a meaningful milestone is reached. Never accumulate changes across many features without committing.
4. **Push gate (instrumented tests):** Push to remote only after Tester runs instrumented tests and they pass. Do not push on unit-test pass alone.
5. **Instrumented tests:** Run `./gradlew connectedDebugAndroidTest` only when the user says a device or AVD is connected. If they pass, push all local commits.
   - **Device preference order:** Always use a connected physical device first. Only fall back to AVD if no physical device is available (`adb devices` shows no device).
   - Physical device: run `adb reverse tcp:8080 tcp:8080 && adb reverse tcp:9099 tcp:9099` first (Firebase emulator ADB forwarding).
   - AVD: use `10.0.2.2` instead of `localhost` to reach host emulators.

### Log Files — Mandatory

Three log directories must always exist and grow over time. **Never delete any log file or the README.**

| Directory | Written by | Naming |
|-----------|-----------|--------|
| `debat_log/` | Debator | `debat_YYYY-MM-DD.md` (add suffix letter if >1 per day: `debat_2026-06-26b.md`) |
| `testing_log/` | Tester | `test_YYYY-MM-DD.md` (same suffix rule) |
| `conversation_log/` | Claude proactively | `conversation_YYYY-MM-DD.md` — write this **before the user sends 10 messages** in a session, not only when asked |

### Autonomy

- Do not ask the user for approval at each step. Work autonomously through the full Debator → Developer → Tester cycle.
- Only pause and ask if following the rules would be violated or if a destructive/irreversible action is needed (e.g., deleting a branch, force-pushing).
- **Install tools freely.** If a build step, test, or CI task requires a missing tool (Node.js, Firebase CLI, JDK, etc.), install it with `winget` or `npm` without asking. The user has explicitly granted permission: *"if you need something then do not hesitate just install it!"*

### File Safety

- **Never delete files** unless the file is directly blocking the build and has no other solution.
- **`README.md` must never be deleted.** It is the source of truth for the implementation plan.
- **All log files must never be deleted.** They are the audit trail.
- If unexpected files or branches appear, investigate before deleting — they may be in-progress work.