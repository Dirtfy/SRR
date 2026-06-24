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