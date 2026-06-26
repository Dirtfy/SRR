# Conversation Log — 2026-06-26c

## Session Context

Continued from 2026-06-26b. User granted full autonomy — no need to ask for approval at each step.
User also clarified: delete files only when genuinely needed, not proactively for tidiness.

---

## Work Done

### Debator review 3 (`debat_log/debat_2026-06-26c.md`)
Reviewed: PrimaryTabRow change + broader audit of user/Screen, CompilationUiState, Fragment wiring.
**Verdict: APPROVE PrimaryTabRow; two CRITICALs found in map popup wiring**

**CRITICAL 1 — `onMapItemTap` silently dropped**
`CompilationTabContent` accepted but never forwarded `onMapItemTap` to `CompilationMapTab`. `CompilationViewModel.onMapItemTap()` was never called; `mapPopupItem` always stayed `null`. The popup appeared to work only because `MapScreen` had its own local `selectedPoint` state.

**CRITICAL 2 — Back-button mapPopupItem branches were dead code**
`CompilationFragment.shouldShowBackButton()` and `onBackClick()` checked `state.mapPopupItem != null`, which was always false.

### Fix decision: remove mapPopupItem from ViewModel (Option A)
`MapScreen` already handles the popup correctly via local `selectedPoint` remember state. `Dialog()` auto-dismisses on Android back press, so ViewModel state for the popup is unnecessary complexity.

### Changes (commit `2efc731`)
- `CompilationUiState.Ready`: removed `mapPopupItem: Item?`
- `CompilationViewModel`: removed `onMapItemTap()`, removed `mapPopupItem` from `onTabSelected` and `clearSelection()`
- `CompilationScreen` / `CompilationReadyContent` / `CompilationTabContent`: removed `onMapItemTap` from all signatures
- `CompilationFragment`: removed `onMapItemTap = viewModel::onMapItemTap`, removed `mapPopupItem` from back-button logic
- `user/Screen.kt` + `compilation/Screen.kt`: `TabRow` → `PrimaryTabRow`

### Tests
30/30 pass, no warnings.

---

## Commits (2026-06-26c)

| Commit | Description |
|--------|-------------|
| `2efc731` | fix map popup dead wiring and TabRow deprecation |

---

## Outstanding

| Item | Status |
|------|--------|
| Step 25: Firebase emulator integration tests | ⏭ Deferred |
| Step 26: Manual E2E | ⏭ Manual |
| Push to remote | Tester approved |
