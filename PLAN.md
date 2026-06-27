# SRR — Implementation Plan

## Architecture

### Remote data service

**Firebase** is used as the remote service:
- **Firebase Authentication** — email-and-password user accounts
- **Cloud Firestore** — stores items, features, and each user's evaluation (ordered item list) per feature

### Directory structure

`(new)` = file did not exist at project start.
All files under `ui/` existed with mock data and were updated during Phase 4.

```
app/src/main/java/com/dirtfy/srr/
│
├── core/                                              pure Kotlin — no Android, Firebase, or UI imports
│   ├── model/
│   │   ├── Item.kt                                    data class Item(id: String, name: String)
│   │   ├── Feature.kt                                 data class Feature(id: String, name: String)
│   │   ├── Evaluation.kt                              one user's ordered item list for one feature
│   │   └── ScoreMatrix.kt                             scores[itemId][featureId] and voteCounts
│   ├── repository/                                    interfaces only — no Firebase imports anywhere in this package
│   │   ├── UserAccountRepository.kt                   signIn / signUp / signOut / currentUserId
│   │   ├── EvaluationRepository.kt                    submitEvaluation / getEvaluationsForFeature
│   │   ├── ItemRepository.kt                          getAllItems / createItem
│   │   └── FeatureRepository.kt                       getAllFeatures / createFeature
│   ├── scoring/
│   │   ├── FeatureScoringEngine.kt                    interface: computeScores(items, features, evaluations, threshold)
│   │   ├── EvaluationConverter.kt                     ordered list → raw scores uniformly in [-1, +1]
│   │   ├── ScoreAggregator.kt                         averages per-item scores; returns null below vote threshold
│   │   ├── ScoreRemapper.kt                           raw [-1, +1] → display [0, 10]
│   │   └── DefaultFeatureScoringEngine.kt             wires Converter + Aggregator + Remapper into FeatureScoringEngine
│   └── usecase/
│       └── LoadFeatureScoresUseCase.kt                fetches items, features, evaluations then runs the scoring engine
│
├── remote/                                            Firebase SDK calls — implements core/repository interfaces
│   ├── model/
│   │   └── EvaluationRecord.kt                        Firestore document shape; all fields have defaults for deserialization
│   ├── RemoteUserAccountRepository.kt                 Firebase Auth: signInWithEmailAndPassword / createUser / signOut
│   ├── RemoteEvaluationRepository.kt                  Firestore path: evaluations/{featureId}/userEvaluations/{userId}
│   ├── RemoteItemRepository.kt                        Firestore path: items/{itemId}
│   └── RemoteFeatureRepository.kt                     Firestore path: features/{featureId}
│
└── ui/
    ├── performer/                                      pure Compose — no Fragment/Activity imports
    │   ├── base/
    │   │   ├── Item.kt                                 BottomNavigationItem data model
    │   │   ├── Screen.kt                               SRRBaseScreen — Scaffold + CenterAlignedTopAppBar + NavigationBar
    │   │   └── theme/
    │   │       ├── Color.kt
    │   │       ├── Theme.kt                            SRRTheme — wraps MaterialTheme with dynamic color
    │   │       └── Type.kt
    │   │
    │   ├── login/
    │   │   ├── LoginUiState.kt
    │   │   ├── LoginViewModel.kt
    │   │   └── Screen.kt
    │   │
    │   ├── user/
    │   │   ├── UserUiState.kt                          sealed: Loading | Error | Ready(+EvaluationEditorState+AddDialogStates)
    │   │   ├── UserViewModel.kt
    │   │   └── Screen.kt                               unified screen file (items, features, detail, editor, add dialogs)
    │   │
    │   └── compilation/
    │       ├── CompilationUiState.kt                   sealed: Loading | Error | Ready
    │       ├── CompilationViewModel.kt
    │       ├── Screen.kt
    │       ├── ViewMode.kt
    │       └── map/
    │           ├── Item.kt
    │           ├── Popup.kt
    │           └── Screen.kt
    │
    └── window/
        ├── MainActivity.kt
        └── component/
            ├── BaseFragment.kt
            ├── LoginFragment.kt
            ├── UserFragment.kt
            └── CompilationFragment.kt
```

### Layer rules

| Layer | May import from | Must NOT import from |
|-------|----------------|----------------------|
| `core/` | nothing outside `core/` | `remote/`, `ui/`, Android SDK, Firebase SDK |
| `remote/` | `core/` | `ui/` |
| `ui/performer/` | `core/` | `remote/`, `window/` |
| `ui/window/` | `core/`, `ui/performer/` | `remote/` directly |

---

## Global decisions (already made)

- Scores computed **on-demand** — not stored in Firestore
- Unevaluated items **excluded** from averages (not treated as 0)
- Display scale: **[0, 10]**, formula `(raw + 1.0) / 2.0 × 10.0`
- Vote threshold: **N = 3**; show `—` when below threshold
- Bad actor mitigation: **deferred**
- Item/feature creation: any authenticated user may **create**; update/delete are blocked

---

## Phase 0 — Firebase Project Setup

**Step 1: Create Firebase project and enable services**

- [x] Go to `console.firebase.google.com` and click **Add project**
- [x] In the project overview, click **Add app → Android**, enter package name `com.dirtfy.srr`, and complete the wizard
- [x] Download `google-services.json` and place it in `app/`
- [x] Go to **Authentication → Sign-in method** and enable **Email/Password**
- [x] Go to **Firestore Database → Create database**, choose your region, and select **Start in test mode**
- [x] Verify the Firestore database appears in the console and shows an empty data view before continuing

**Step 2: Seed initial data in Firestore**

Firestore schema:
```
Collection: items
  Document ID: {itemId}          (e.g. "item_notebook")
  Fields:
    name: String                  (e.g. "Notebook")

Collection: features
  Document ID: {featureId}       (e.g. "feature_durability")
  Fields:
    name: String                  (e.g. "Durability")

Collection: evaluations
  Document ID: {featureId}
  Subcollection: userEvaluations
    Document ID: {userId}
    Fields:
      orderedItemIds: Array
      submittedAt:   Timestamp
```

- [x] In **Firestore → Data**, add at least 3 `items` documents (each with a `name` string field)
- [x] Create a `features` collection with at least 2 documents
- [x] The `evaluations` collection is created automatically on first submission — no manual setup needed

---

## Phase 1 — Design

**Step 3: Define Kotlin domain models**

`app/src/main/java/com/dirtfy/srr/core/model/`

- [x] `Item.kt` — `data class Item(val id: String, val name: String)`
- [x] `Feature.kt` — `data class Feature(val id: String, val name: String)`
- [x] `Evaluation.kt` — `data class Evaluation(val userId: String, val featureId: String, val orderedItemIds: List<String>)`
- [x] `ScoreMatrix.kt` — `data class ScoreMatrix(val scores: Map<String, Map<String, Double?>>, val voteCounts: Map<String, Map<String, Int>>)`

**Step 4: Define repository interfaces**

`app/src/main/java/com/dirtfy/srr/core/repository/`

- [x] `UserAccountRepository.kt` — signIn / signUp / signOut / currentUserId
- [x] `EvaluationRepository.kt` — submitEvaluation / getEvaluationsForFeature
- [x] `ItemRepository.kt` — getAllItems
- [x] `FeatureRepository.kt` — getAllFeatures

**Step 5: Define the scoring engine interface**

- [x] `core/scoring/FeatureScoringEngine.kt` — `fun computeScores(allItemIds, allFeatureIds, evaluationsByFeature, minVoteThreshold): ScoreMatrix`

**Step 6: Write and deploy Firestore security rules**

- [x] Create `firestore.rules` in the project root
- [x] Deploy via Firebase console or `firebase deploy --only firestore:rules`

**Step 7: Plan UiState state machines**

- [x] Replace `LoginUiState.kt` with data class containing `email`, `password`, `isLoading`, `isLoginSuccess`, `error`
- [x] Replace `UserUiState.kt` with sealed class `Loading | Error | Ready(+EvaluationEditorState)`
- [x] Replace `CompilationUiState.kt` with sealed class `Loading | Error | Ready`

---

## Phase 2 — Core Logic Implementation

**Step 8: Implement EvaluationConverter**

- [x] `core/scoring/EvaluationConverter.kt` — convert ordered list to `Map<String, Double>` in `[-1, +1]`
  - `["A"]` → `{A: 0.0}`, `["A","B"]` → `{A: +1.0, B: -1.0}`, `["A","B","C"]` → `{A: +1.0, B: 0.0, C: -1.0}`

**Step 9: Implement ScoreAggregator**

- [x] `core/scoring/ScoreAggregator.kt` — average per-item scores across evaluations; null when below `minVotes`

**Step 10: Implement ScoreRemapper**

- [x] `core/scoring/ScoreRemapper.kt` — `[-1, +1] → [0, 10]`; formula `(raw + 1.0) / 2.0 * 10.0`

**Step 11: Implement DefaultFeatureScoringEngine**

- [x] `core/scoring/DefaultFeatureScoringEngine.kt` — wires Converter + Aggregator + Remapper; builds `ScoreMatrix`

---

## Phase 3 — Firebase Data Layer Implementation

**Step 12: Add Firebase dependencies**

- [x] Add Firebase BOM, `firebase-auth-ktx`, `firebase-firestore-ktx`, `reorderable`, `material-icons-extended` to `app/build.gradle.kts`
- [x] Add `google-services` plugin to both `build.gradle.kts` files
- [x] Place `google-services.json` in `app/`
- [x] Verify `./gradlew assembleDebug` succeeds

**Step 13: Implement RemoteUserAccountRepository**

- [x] `remote/RemoteUserAccountRepository.kt` — Firebase Auth: signIn / signUp / signOut / currentUserId

**Step 14: Implement RemoteEvaluationRepository**

- [x] `remote/RemoteEvaluationRepository.kt` — Firestore path `evaluations/{featureId}/userEvaluations/{userId}`

**Step 15: Implement RemoteItemRepository, RemoteFeatureRepository, EvaluationRecord**

- [x] `remote/model/EvaluationRecord.kt`
- [x] `remote/RemoteItemRepository.kt` — Firestore collection `items`
- [x] `remote/RemoteFeatureRepository.kt` — Firestore collection `features`

---

## Phase 4 — Use Cases and UI Wiring

#### Dependency wiring (no DI framework)

Each ViewModel has a `companion object { fun factory() }` that constructs all concrete dependencies inline. The Fragment uses `by viewModels { ViewModel.factory() }`.

**Step 16: Implement LoadFeatureScoresUseCase**

- [x] `core/usecase/LoadFeatureScoresUseCase.kt` — shared by UserViewModel and CompilationViewModel; returns `Output(items, features, scoreMatrix, evaluationsByFeature)`

**Step 17: Update LoginViewModel and LoginFragment**

- [x] Inject `UserAccountRepository`; add `factory()`; implement real `login()` and `signUp()`; add `isAlreadySignedIn()` for auto-login `LaunchedEffect`
- [x] `LoginFragment` passes `viewModel(factory = LoginViewModel.factory())`; shows loading indicator and error text

**Step 18: Update UserViewModel and User screens**

- [x] Constructor: `UserAccountRepository`, `EvaluationRepository`, `LoadFeatureScoresUseCase`
- [x] Methods: `loadAllData`, `onRetryTap`, `onTabSelected`, `onItemSelected`, `onFeatureSelected`, `onOpenEvaluationEditor`, `onEvaluationReorder`, `onSubmitEvaluation`, `clearSelection`
- [x] Drag-to-reorder evaluation editor using `sh.calvin.reorderable`
- [x] `UserFragment`: `by viewModels { UserViewModel.factory() }`, wires all callbacks

**Step 19: Update CompilationViewModel and Compilation screens**

- [x] Constructor: `LoadFeatureScoresUseCase`
- [x] Methods: `loadAllData`, `onRetryTap`, `onTabSelected`, `onItemSelected`, `onFeatureSelected`, `onMapXFeatureSelected`, `onMapYFeatureSelected`, `clearSelection`
- [x] Map tab: Canvas scatter plot + DropdownMenu axis selectors
- [x] `CompilationFragment`: `by viewModels { CompilationViewModel.factory() }`, wires all callbacks

**Step 20: Add sign-out button**

- [x] `SRRBaseScreen` gains `actions: @Composable RowScope.() -> Unit = {}` parameter
- [x] `BaseFragment` has open `TopBarActions()` composable; default is no-op
- [x] `UserFragment` and `CompilationFragment` override `TopBarActions()` with `ExitToApp` icon → `MainActivity.signOut()`

---

## Phase 5 — Quality

**Step 21: Unit test EvaluationConverter**

- [x] `EvaluationConverterTest.kt` — empty, single, two, three, four items; absent items absent from result

**Step 22: Unit test ScoreAggregator**

- [x] `ScoreAggregatorTest.kt` — zero evaluations, below-threshold, meet-threshold, partial evaluations

**Step 23: Unit test ScoreRemapper**

- [x] `ScoreRemapperTest.kt` — boundary values `-1.0 → 0.0`, `0.0 → 5.0`, `+1.0 → 10.0`; remapOrNull

**Step 24: Unit test DefaultFeatureScoringEngine**

- [x] `DefaultFeatureScoringEngineTest.kt` — empty, threshold, two independent features, voteCounts accuracy

**Step 25: Integration test Firebase repositories**

- [x] `RemoteEvaluationRepositoryTest.kt` — tests against Firebase Local Emulator Suite (Auth + Firestore)
  - submit writes correct path, re-submit overwrites, multi-user all returned, empty feature returns `[]`
  - teardown clears emulator data via HTTP DELETE

**Step 26: Manual end-to-end testing checklist**

- [ ] Sign up / sign in / wrong password error
- [ ] Item detail: scores per feature; `—` below threshold
- [ ] Feature detail: items sorted by score; Evaluate button
- [ ] Submit and re-submit evaluation; scores update
- [ ] Compilation tab: read-only; no Evaluate button
- [ ] Map tab: scatter plot; axis dropdowns; point tap popup
- [ ] Sign out → back does not re-enter without login
- [ ] Kill and reopen → auto-login if session cached
- [ ] Network disconnect → Error state with Retry

---

## Phase 6 — Item and Feature Management

**Step 27: Extend repository interfaces**

Files: `core/repository/ItemRepository.kt`, `core/repository/FeatureRepository.kt`

- [x] Add to `ItemRepository`:
  ```kotlin
  // Returns the newly created Item with its Firestore-generated ID.
  suspend fun createItem(name: String): Result<Item>
  ```
- [x] Add to `FeatureRepository`:
  ```kotlin
  suspend fun createFeature(name: String): Result<Feature>
  ```

**Step 28: Update Firestore security rules**

File: `firestore.rules`

- [x] Change items and features rules to allow `create` (but not `update` or `delete`) for authenticated users:
  ```
  match /items/{itemId} {
    allow read:   if request.auth != null;
    allow create: if request.auth != null
                  && request.resource.data.name is string
                  && request.resource.data.name.size() > 0;
  }
  match /features/{featureId} {
    allow read:   if request.auth != null;
    allow create: if request.auth != null
                  && request.resource.data.name is string
                  && request.resource.data.name.size() > 0;
  }
  ```
- [x] Deploy: **Firestore → Rules** in the Firebase console → Publish. Or `firebase deploy --only firestore:rules`

**Step 29: Implement createItem and createFeature in remote repositories**

Files: `remote/repository/RemoteItemRepository.kt`, `remote/repository/RemoteFeatureRepository.kt`

- [x] `RemoteItemRepository.createItem`:
  ```kotlin
  override suspend fun createItem(name: String): Result<Item> =
      runCatching {
          val ref = db.collection("items").add(hashMapOf("name" to name)).await()
          Item(id = ref.id, name = name)
      }
  ```
- [x] `RemoteFeatureRepository.createFeature`:
  ```kotlin
  override suspend fun createFeature(name: String): Result<Feature> =
      runCatching {
          val ref = db.collection("features").add(hashMapOf("name" to name)).await()
          Feature(id = ref.id, name = name)
      }
  ```
  Firestore `.add()` generates a random document ID; the returned `DocumentReference.id` is used as the model ID.

**Step 30: Extend UserUiState with add-dialog states**

File: `ui/performer/user/UserUiState.kt`

- [x] Add two nested state classes inside `UserUiState.Ready` (following the same pattern as `EvaluationEditorState`):
  ```kotlin
  data class AddItemDialogState(
      val name: String = "",
      val isSaving: Boolean = false,
      val error: String? = null
  )
  data class AddFeatureDialogState(
      val name: String = "",
      val isSaving: Boolean = false,
      val error: String? = null
  )
  ```
- [x] Add fields to `UserUiState.Ready`:
  ```kotlin
  val addItemDialog: AddItemDialogState? = null,
  val addFeatureDialog: AddFeatureDialogState? = null,
  ```

**Step 31: Update UserViewModel**

File: `ui/performer/user/UserViewModel.kt`

- [x] Add constructor parameters `itemRepository: ItemRepository` and `featureRepository: FeatureRepository`
- [x] Update `factory()` to inject `RemoteItemRepository()` and `RemoteFeatureRepository()`
- [x] Add methods:
  - `onOpenAddItemDialog()` — sets `addItemDialog = AddItemDialogState()`
  - `onOpenAddFeatureDialog()` — sets `addFeatureDialog = AddFeatureDialogState()`
  - `onAddItemNameChange(name: String)` — copies name into `addItemDialog`
  - `onAddFeatureNameChange(name: String)` — copies name into `addFeatureDialog`
  - `onDismissAddDialog()` — clears both `addItemDialog` and `addFeatureDialog`
  - `onAddItem()` — calls `itemRepository.createItem(name)`, on success calls `loadAllData()` (which resets to Loading and closes dialog)
  - `onAddFeature()` — calls `featureRepository.createFeature(name)`, on success calls `loadAllData()`

**Step 32: Update UserFragment**

File: `ui/window/component/UserFragment.kt`

- [x] Pass new callbacks into `UserScreen`:
  `onOpenAddItemDialog`, `onOpenAddFeatureDialog`, `onAddItemNameChange`, `onAddFeatureNameChange`, `onDismissAddDialog`, `onAddItem`, `onAddFeature`

**Step 33: Update User Screen**

File: `ui/performer/user/Screen.kt`

- [x] Add new callback parameters to `UserScreen` signature
- [x] Show `AddItemAlertDialog` when `state.addItemDialog != null`; show `AddFeatureAlertDialog` when `state.addFeatureDialog != null` (both overlay on the current content, shown inside the `Ready` branch)
- [x] In `ItemsTabContent`: wrap content in `Box`; add `FloatingActionButton` at `Alignment.BottomEnd` (`Icons.Default.Add`) calling `onOpenAddItemDialog`
- [x] In `FeaturesTabContent`: same FAB pattern calling `onOpenAddFeatureDialog`
- [x] `AddItemAlertDialog` / `AddFeatureAlertDialog`: `AlertDialog` with `OutlinedTextField`, confirm button enabled only when `name.isNotBlank() && !isSaving`, spinner in confirm button while `isSaving`, error text below field when `error != null`

---

## Phase 7 — Creator Tracking, Delete, and Bug Fixes

**Step 34: Preserve active tab across data reloads**

File: `ui/performer/user/UserViewModel.kt`

- [x] Capture `activeTab` from current `Ready` state before emitting `Loading` in `loadAllData()`; restore it in the new `Ready` state so the UI stays on the Features tab after adding a feature

**Step 35: Fix debug auto-login undoing sign-out**

Files: `ui/window/component/LoginFragment.kt`, `ui/performer/login/Screen.kt`, `ui/window/MainActivity.kt`

- [x] Add `LoginFragment.newInstance(autoLogin: Boolean)` factory; store flag in `arguments`
- [x] `LoginScreen` accepts `autoLogin: Boolean`; only calls `debugAutoLogin()` when `true`
- [x] `MainActivity.signOut()` uses `LoginFragment.newInstance(autoLogin = false)`

**Step 36: Add `createdBy` field to domain models and Firestore**

Files: `core/model/Item.kt`, `core/model/Feature.kt`, `remote/repository/RemoteItemRepository.kt`, `remote/repository/RemoteFeatureRepository.kt`

- [x] `Item(id, name, createdBy: String = "")` — default for backward compat with existing docs
- [x] `Feature(id, name, createdBy: String = "")`
- [x] `RemoteItemRepository.getAllItems()` — read `createdBy` field
- [x] `RemoteFeatureRepository.getAllFeatures()` — read `createdBy` field
- [x] `RemoteItemRepository.createItem(name, createdBy)` — write `createdBy` to Firestore
- [x] `RemoteFeatureRepository.createFeature(name, createdBy)` — write `createdBy` to Firestore

**Step 37: Add delete to repository interfaces and remote implementations**

Files: `core/repository/ItemRepository.kt`, `core/repository/FeatureRepository.kt`, `remote/repository/RemoteItemRepository.kt`, `remote/repository/RemoteFeatureRepository.kt`

- [x] `ItemRepository.deleteItem(id: String): Result<Unit>`
- [x] `FeatureRepository.deleteFeature(id: String): Result<Unit>`
- [x] `RemoteItemRepository.deleteItem` — `db.collection("items").document(id).delete().await()`
- [x] `RemoteFeatureRepository.deleteFeature` — `db.collection("features").document(id).delete().await()`

**Step 38: Update Firestore security rules**

File: `firestore.rules`

- [x] Add `allow delete: if request.auth != null && resource.data.createdBy == request.auth.uid` to `items` and `features` rules

**Step 39: Wire delete in UserViewModel**

File: `ui/performer/user/UserViewModel.kt`

- [x] Pass `currentUserId` to `createItem` and `createFeature` calls
- [x] Add `onDeleteItem(id: String)` — calls `itemRepository.deleteItem(id)`, on success `loadAllData()`
- [x] Add `onDeleteFeature(id: String)` — calls `featureRepository.deleteFeature(id)`, on success `loadAllData()`

**Step 40: Update User Screen UI**

Files: `ui/performer/user/Screen.kt`, `ui/window/component/UserFragment.kt`

- [x] Pass `currentUserId` into `ItemsTabContent` and `FeaturesTabContent`
- [x] `ListItem` for items: `supportingContent = "Added by you"` when owned; trailing `IconButton(Icons.Default.Delete)` when owned
- [x] `ListItem` for features: same pattern; count and delete button coexist in trailing `Row`
- [x] Wire `onDeleteItem` and `onDeleteFeature` from `UserFragment`

**Step 41: Add instrumented test for evaluation threshold**

File: `app/src/androidTest/.../RemoteEvaluationRepositoryTest.kt`

- [x] `threeUsersEvaluate_scoreExceedsThreshold`: create 3 users, each submits evaluation for same feature, retrieve all 3, run `DefaultFeatureScoringEngine` inline, assert computed score is non-null

---

## Phase 8 — Backend Hardening + Production Deployment

### Firebase Backend Data Model

#### `/items/{itemId}`
| Field | Type | Notes |
|-------|------|-------|
| `name` | string | Display name, 1–100 chars |
| `nameLower` | string | `name.trim().lowercase()` — used for case-insensitive uniqueness query |
| `createdBy` | string | `auth.uid` of creator — enforced by security rule on create |
| `createdAt` | timestamp | Server timestamp; used for chronological ordering |

#### `/features/{featureId}`
Same structure as `items`.

#### `/evaluations/{featureId}/userEvaluations/{userId}`
| Field | Type | Notes |
|-------|------|-------|
| `userId` | string | Matches document path `userId` and `auth.uid` |
| `featureId` | string | Matches parent document path `featureId` |
| `orderedItemIds` | string[] | Item IDs ordered strongest→weakest; must be non-empty |
| `submittedAt` | timestamp | Server timestamp on create/update |

### Security Rules Design
```
items / features:
  read:   any auth'd user
  create: auth required + createdBy == auth.uid + name 1–100 chars
  delete: creator only (resource.data.createdBy == auth.uid)

evaluations/{featureId}/userEvaluations/{userId}:
  read:   any auth'd user
  write:  auth required + auth.uid == userId + orderedItemIds non-empty
```

### Build Variants
| Variant | Firebase target | Signing | Use case |
|---------|----------------|---------|----------|
| `debug` | Local Emulator Suite (localhost) | debug key | Development + automated tests |
| `staging` | Production Firebase | debug key | Manual testing with real data |
| `release` | Production Firebase | release key | Distribution |

`USE_EMULATOR` BuildConfig flag (true only in `debug`) drives `SRRApplication.useEmulator()`.

### Indexes
Firestore auto-creates single-field indexes. No composite indexes needed for current queries:
- `items.createdAt ASC` — auto index (for `getAllItems` ordering)
- `items.nameLower` — auto index (for uniqueness `whereEqualTo` query)
- `features.createdAt ASC` — auto index
- `features.nameLower` — auto index

### Future Backend Work (not yet implemented)
- **Cascade deletes** — when item deleted, remove its orderedItemIds from all evaluations (Cloud Function trigger)
- **User profiles** — `/users/{uid}` collection with display name
- **Real-time listeners** — replace one-shot `get()` with `addSnapshotListener()` in repositories
- **Firebase App Check** — protect against unauthorized clients
- **Atomic uniqueness** — replace check-then-create with Firestore transaction + `/item_names/{nameLower}` index doc to eliminate race condition

**Step 42: Enforce name uniqueness (case-insensitive)**

Files: `remote/repository/RemoteItemRepository.kt`, `remote/repository/RemoteFeatureRepository.kt`

- [x] Store `nameLower = name.trim().lowercase()` in Firestore on create
- [x] `whereEqualTo("nameLower", nameLower)` before `add()` — throw `IllegalArgumentException` if duplicate found
- [x] Error propagates to `onAddItem` / `onAddFeature` failure handler → shown in dialog `error` field
- Note: items and features do NOT share a uniqueness constraint (an item and a feature may have the same name)

**Step 43: Harden Firestore security rules**

File: `firestore.rules`

- [x] Add `request.resource.data.createdBy == request.auth.uid` to items/features `create` rule
- [x] Add `request.resource.data.name.size() <= 100` length cap
- [x] Add `request.resource.data.orderedItemIds is list && orderedItemIds.size() > 0` to evaluation `write` rule

**Step 44: Add `staging` build type**

File: `app/build.gradle.kts`, `app/src/main/java/.../SRRApplication.kt`

- [x] Add `staging` build type: `initWith(debug)` + `buildConfigField("Boolean", "USE_EMULATOR", "false")`
- [x] Add `debug { buildConfigField("Boolean", "USE_EMULATOR", "true") }` and `release { ... "false" }`
- [x] Replace `BuildConfig.DEBUG` with `BuildConfig.USE_EMULATOR` in `SRRApplication.kt`
- Install with: `./gradlew installStaging`

**Step 45: Deploy to production Firebase**

- [ ] `firebase deploy --only firestore:rules` — publish hardened rules to production
- [ ] Verify **Authentication → Sign-in method → Email/Password** is enabled in Firebase console
- [ ] `./gradlew installStaging` — install production-Firebase-connected APK on device
- [ ] Manual sign-up + item/feature creation smoke test on real Firebase data
