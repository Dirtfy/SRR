# SRR

A collaborative feature-strength evaluation app. Users order items by how strongly each item possesses a given feature; the orderings from all users are aggregated to produce a consensus score showing how strongly each item has each feature.

## Glossary

| Term            | Meaning                                                                                              |
|-----------------|------------------------------------------------------------------------------------------------------|
| **item**        | An object being evaluated (e.g. a product, place, candidate)                                         |
| **feature**     | A quality or characteristic being measured (e.g. durability, portability, taste)                     |
| **evaluation**  | One user's ordering of items for a single feature, from strongest to weakest feature presence        |
| **score**       | The aggregated consensus value representing how strongly an item possesses a feature, across all users |

## App Behavior

**User input:** For each feature, the user orders all items from strongest feature presence (index 0) to weakest (last). Evaluating every item is encouraged but not required — unevaluated items are excluded from that feature's score entirely.

Example — features: `durability`, `portability` / items: `A`, `B`, `C`
```
durability  → [B, C, A]   (B is most durable, A is least)
portability → [A, C, B]   (A is most portable, B is least)
```

**User view:** An aggregated score matrix showing each item's strength score per feature, and a map/scatter view for cross-feature comparison.

## Architecture

### Remote data service

**Firebase** is used as the remote service:
- **Firebase Authentication** — email-and-password user accounts
- **Cloud Firestore** — stores each user's evaluation (ordered item list) per feature

### Directory structure

`(new)` = file does not exist yet and must be created during the TODO phases.
All other files already exist (with mock data).

```
app/src/main/java/com/dirtfy/srr/
│
├── core/                                              (new) pure Kotlin — no Android, Firebase, or UI imports
│   ├── model/
│   │   ├── Item.kt                                    (new) data class Item(id: String, name: String)
│   │   ├── Feature.kt                                 (new) data class Feature(id: String, name: String)
│   │   ├── Evaluation.kt                              (new) one user's ordered item list for one feature
│   │   └── ScoreMatrix.kt                             (new) scores[itemId][featureId] and voteCounts
│   ├── repository/                                    (new) interfaces only — no Firebase imports anywhere in this package
│   │   ├── UserAccountRepository.kt                   (new) signIn / signUp / signOut / currentUserId
│   │   ├── EvaluationRepository.kt                    (new) submitEvaluation / getEvaluationsForFeature
│   │   ├── ItemRepository.kt                          (new) getAllItems
│   │   └── FeatureRepository.kt                       (new) getAllFeatures
│   ├── scoring/
│   │   ├── FeatureScoringEngine.kt                    (new) interface: computeScores(items, features, evaluations, threshold)
│   │   ├── EvaluationConverter.kt                     (new) ordered list → raw scores uniformly in [-1, +1]
│   │   ├── ScoreAggregator.kt                         (new) averages per-item scores; returns null below vote threshold
│   │   ├── ScoreRemapper.kt                           (new) raw [-1, +1] → display [0, 10]
│   │   └── DefaultFeatureScoringEngine.kt             (new) wires Converter + Aggregator + Remapper into FeatureScoringEngine
│   └── usecase/
│       └── LoadFeatureScoresUseCase.kt                (new) fetches items, features, evaluations then runs the scoring engine
│
├── remote/                                            (new) Firebase SDK calls — implements core/repository interfaces
│   ├── model/
│   │   └── EvaluationRecord.kt                        (new) Firestore document shape; all fields have defaults for deserialization
│   ├── RemoteUserAccountRepository.kt                 (new) Firebase Auth: signInWithEmailAndPassword / createUser / signOut
│   ├── RemoteEvaluationRepository.kt                  (new) Firestore path: evaluations/{featureId}/userEvaluations/{userId}
│   ├── RemoteItemRepository.kt                        (new) Firestore path: items/{itemId}
│   └── RemoteFeatureRepository.kt                     (new) Firestore path: features/{featureId}
│
└── ui/
    ├── performer/                                      pure Compose — no Fragment/Activity imports
    │   ├── base/
    │   │   ├── Item.kt                                 BottomNavigationItem data model; shared across tabs
    │   │   ├── Screen.kt                               SRRBaseScreen — Scaffold + CenterAlignedTopAppBar + NavigationBar
    │   │   └── theme/
    │   │       ├── Color.kt                            color palette (purple/pink; falls back on pre-Android 12)
    │   │       ├── Theme.kt                            SRRTheme — wraps MaterialTheme with dynamic color
    │   │       └── Type.kt                             typography scale
    │   │
    │   ├── login/
    │   │   ├── LoginUiState.kt                         data class: email, password, isLoading, error
    │   │   ├── LoginViewModel.kt                       onEmailChange / onPasswordChange / login() / signUp()
    │   │   └── Screen.kt                               LoginScreen (stateful, holds VM) + LoginContent (stateless, previewable)
    │   │
    │   ├── user/
    │   │   ├── UserUiState.kt                          sealed: Loading | Error(message) | Ready(+EvaluationEditorState)
    │   │   ├── UserViewModel.kt                        loads scores via use case; handles tab / detail / editor state
    │   │   ├── Screen.kt                               UserScreen — routes to items or features sub-screen based on UiState.activeTab
    │   │   ├── items/
    │   │   │   ├── Item.kt                             UI model for one cell in the item grid
    │   │   │   ├── Screen.kt                           UserItemsScreen — LazyVerticalGrid of items
    │   │   │   └── detail/
    │   │   │       ├── Item.kt                         UI model for one feature-score row inside item detail
    │   │   │       └── Screen.kt                       UserItemDetailScreen — score per feature; "—" if null
    │   │   └── features/
    │   │       ├── Item.kt                             UI model for one row in the feature list
    │   │       ├── Screen.kt                           UserFeaturesScreen — LazyColumn of features
    │   │       └── detail/
    │   │           ├── Item.kt                         UI model for one item-score row inside feature detail
    │   │           ├── Popup.kt                        EvaluationEditorPopup — drag-to-reorder item list + Submit button
    │   │           └── Screen.kt                       UserFeatureDetailScreen — items ranked by score; Evaluate button
    │   │
    │   └── compilation/
    │       ├── CompilationUiState.kt                   sealed: Loading | Error(message) | Ready
    │       ├── CompilationViewModel.kt                 loads scores; handles tab / item detail / feature detail / map popup
    │       ├── Screen.kt                               CompilationScreen — routes to ITEMS / FEATURES / MAP tab
    │       ├── ViewMode.kt                             enum: ITEMS | FEATURES | MAP
    │       ├── items/
    │       │   ├── Item.kt                             UI model for one cell in the item grid
    │       │   ├── Screen.kt                           CompilationItemsScreen — read-only item grid
    │       │   └── detail/
    │       │       ├── Item.kt                         UI model for one feature-score row
    │       │       └── Screen.kt                       CompilationItemDetailScreen — scores per feature; no edit
    │       ├── features/
    │       │   ├── Item.kt                             UI model for one row in the feature list
    │       │   ├── Screen.kt                           CompilationFeaturesScreen — read-only feature list
    │       │   └── detail/
    │       │       ├── Item.kt                         UI model for one item-score row
    │       │       └── Screen.kt                       CompilationFeatureDetailScreen — items ordered by score; no edit
    │       └── map/
    │           ├── Item.kt                             UI model for one point on the scatter plot (x, y, label)
    │           ├── Popup.kt                            MapItemPopup — score card shown when a point is tapped
    │           └── Screen.kt                           CompilationMapScreen — Canvas scatter plot; axes = two selected features
    │
    └── window/                                         Android framework layer — Fragment and Activity only
        ├── MainActivity.kt                             single Activity; FrameLayout host (id=10001); top-level Fragment routing
        └── component/
            ├── BaseFragment.kt                         abstract Fragment; inflates ComposeView; applies SRRTheme; renders SRRBaseScreen
            ├── LoginFragment.kt                        login screen host; does NOT extend BaseFragment (no bottom nav)
            ├── UserFragment.kt                         "My" tab host; owns UserViewModel
            └── CompilationFragment.kt                  "Result" tab host; owns CompilationViewModel
```

### Layer rules

| Layer | May import from | Must NOT import from |
|-------|----------------|----------------------|
| `core/` | nothing outside `core/` | `remote/`, `ui/`, Android SDK, Firebase SDK |
| `remote/` | `core/` | `ui/` |
| `ui/performer/` | `core/` | `remote/`, `window/` |
| `ui/window/` | `core/`, `ui/performer/` | `remote/` directly |

## Scoring Algorithm

### Approach

Each user's ordered list for a feature is converted to uniformly distributed raw scores in `[-1, +1]`: item at index 0 (strongest feature presence) → `+1`, last item (weakest) → `-1`. Items the user did not evaluate produce no score entry. Raw scores are then averaged across users and remapped to `[0, 10]` for display.

Example — two users evaluate feature `durability` with items `A`, `B`, `C`:
```
User 1: [B, C, A]  →  B=+1.0,  C= 0.0,  A=-1.0
User 2: [B, A, C]  →  B=+1.0,  A= 0.0,  C=-1.0
```

Average then remap to `[0, 10]`:

| item | raw average               | display score |
|------|---------------------------|---------------|
| A    | (-1.0 + 0.0) / 2 = -0.5  | **2.5**       |
| B    | (+1.0 + +1.0) / 2 = +1.0 | **10.0**      |
| C    | (0.0 + -1.0) / 2 = -0.5  | **2.5**       |

### Design decisions

| # | Issue | Decision | Rationale |
|---|-------|----------|-----------|
| 1 | Zero score ambiguous | Unevaluated items are **excluded**; they produce no score entry | `null` is the unambiguous "not evaluated" state, distinct from a genuine middle score |
| 2 | Raw `[-1, +1]` unintuitive | Remap to **`[0, 10]`** (1 decimal). Formula: `(raw + 1.0) / 2.0 × 10.0` | -1→0.0, 0→5.0, +1→10.0; universally understood scale |
| 3 | Bad actor contamination | **Deferred** post-launch | Vote threshold (decision 4) provides partial protection |
| 4 | Score visibility threshold | Hide score until **N = 3** users have evaluated that item-feature pair; show `—` in UI | Minimum sample to make a mean meaningful without blocking early usage |

### Score computation pipeline (on-demand, not stored)

Scores are never written to Firestore. Each read triggers:
1. Fetch all `Evaluation` records for the requested feature from Firestore
2. Per user: convert ordered list → score map (uniformly spaced in `[-1, +1]`; unevaluated items omitted)
3. Per item: average scores across users who evaluated it; if count < 3 → `null`
4. Remap each average from `[-1, +1]` to `[0, 10]` for display

---

## TODO

**Global decisions (already made)**
- Scores computed **on-demand** — not stored in Firestore
- Unevaluated items **excluded** from averages (not treated as 0)
- Display scale: **[0, 10]**, formula `(raw + 1.0) / 2.0 × 10.0`
- Vote threshold: **N = 3**; show `—` when below threshold
- Bad actor mitigation: **deferred**

---

### Phase 0 — Firebase Project Setup

**Step 1: Create Firebase project and enable services**

- [ ] Go to `console.firebase.google.com` and click **Add project**
- [ ] In the project overview, click **Add app → Android**, enter package name `com.dirtfy.srr`, and complete the wizard
- [ ] Download `google-services.json` and keep it ready (it is placed in `app/` in Step 12)
- [ ] Go to **Authentication → Sign-in method** and enable **Email/Password**
- [ ] Go to **Firestore Database → Create database**, choose your region, and select **Start in test mode** (security rules are locked down properly in Step 6)
- [ ] Verify the Firestore database appears in the console and shows an empty data view before continuing

**Step 2: Seed initial data in Firestore**

Firestore schema (collection → subcollection → document → fields):
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
    Document ID: {userId}         (must match the authenticated user's Firebase UID)
    Fields:
      orderedItemIds: Array        (e.g. ["item_b", "item_c", "item_a"])
      submittedAt:   Timestamp
```

- [ ] In **Firestore → Data**, click **Start collection**, name it `items`, and add at least 3 documents (each with a `name` string field)
- [ ] Create a second collection `features` with at least 2 documents (each with a `name` string field)
- [ ] The `evaluations` collection is created automatically on the first evaluation submission — no manual setup needed
- [ ] In **Firestore → Data**, verify that both `items` and `features` documents are readable before writing any code

---

### Phase 1 — Design

**Step 3: Define Kotlin domain models**

Create these files under `app/src/main/java/com/dirtfy/srr/core/model/` (data classes only, no logic):

- [ ] `Item.kt`
  ```kotlin
  data class Item(val id: String, val name: String)
  ```

- [ ] `Feature.kt`
  ```kotlin
  data class Feature(val id: String, val name: String)
  ```

- [ ] `Evaluation.kt`
  ```kotlin
  // One user's ordering of items for one feature.
  // orderedItemIds[0] = item with the strongest feature presence.
  // Items absent from this list were not evaluated by this user.
  data class Evaluation(
      val userId: String,
      val featureId: String,
      val orderedItemIds: List<String>
  )
  ```

- [ ] `ScoreMatrix.kt`
  ```kotlin
  data class ScoreMatrix(
      // scores[itemId][featureId] = display score in [0.0, 10.0],
      // or null if the item-feature pair has fewer than N evaluations
      val scores: Map<String, Map<String, Double?>>,
      // voteCounts[itemId][featureId] = how many users evaluated this pair
      val voteCounts: Map<String, Map<String, Int>>
  )
  ```

**Step 4: Define repository interfaces**

Create these files under `app/src/main/java/com/dirtfy/srr/core/repository/` (interface declarations only, no implementation):

- [ ] `UserAccountRepository.kt`
  ```kotlin
  interface UserAccountRepository {
      // Returns Result.failure if credentials are wrong or the network is unavailable.
      suspend fun signIn(email: String, password: String): Result<Unit>
      // Returns Result.failure if email already exists or password is too weak.
      suspend fun signUp(email: String, password: String): Result<Unit>
      fun signOut()
      // Returns null if no user is currently signed in.
      fun currentUserId(): String?
  }
  ```

- [ ] `EvaluationRepository.kt`
  ```kotlin
  interface EvaluationRepository {
      // Writes (or overwrites) this user's evaluation for the given feature.
      suspend fun submitEvaluation(evaluation: Evaluation): Result<Unit>
      // Returns all users' evaluations for the given feature.
      // Returns an empty list (not failure) if no evaluations exist yet.
      suspend fun getEvaluationsForFeature(featureId: String): Result<List<Evaluation>>
  }
  ```

- [ ] `ItemRepository.kt`
  ```kotlin
  interface ItemRepository {
      suspend fun getAllItems(): Result<List<Item>>
  }
  ```

- [ ] `FeatureRepository.kt`
  ```kotlin
  interface FeatureRepository {
      suspend fun getAllFeatures(): Result<List<Feature>>
  }
  ```

**Step 5: Define the scoring engine interface**

Create under `app/src/main/java/com/dirtfy/srr/core/scoring/`:

- [ ] `FeatureScoringEngine.kt`
  ```kotlin
  interface FeatureScoringEngine {
      // allItemIds and allFeatureIds define the full set of known items/features.
      // evaluationsByFeature may be missing entries for features with no evaluations yet.
      // minVoteThreshold: item-feature pairs with fewer votes produce null scores.
      fun computeScores(
          allItemIds: List<String>,
          allFeatureIds: List<String>,
          evaluationsByFeature: Map<String, List<Evaluation>>,
          minVoteThreshold: Int = 3
      ): ScoreMatrix
  }
  ```

**Step 6: Write and deploy Firestore security rules**

- [ ] Create `firestore.rules` in the project root:
  ```
  rules_version = '2';
  service cloud.firestore {
    match /databases/{database}/documents {

      // items and features are read-only for authenticated users;
      // only seeded via the console, never written from the app.
      match /items/{itemId} {
        allow read: if request.auth != null;
      }
      match /features/{featureId} {
        allow read: if request.auth != null;
      }

      // Any authenticated user may read all evaluations (needed for on-demand score computation).
      // A user may only write their own evaluation document.
      match /evaluations/{featureId}/userEvaluations/{userId} {
        allow read:  if request.auth != null;
        allow write: if request.auth != null && request.auth.uid == userId;
      }
    }
  }
  ```
- [ ] Deploy the rules: open **Firestore → Rules** in the Firebase console, paste the content above, and click **Publish**
  - Alternatively, run `firebase deploy --only firestore:rules` (requires the Firebase CLI: `npm install -g firebase-tools` then `firebase login`)
- [ ] Verify in **Firestore → Rules Playground**:
  - Authenticated user can `read` `/items/any-id` ✓
  - Authenticated user can `write` `/evaluations/f1/userEvaluations/{their own UID}` ✓
  - Authenticated user cannot `write` `/evaluations/f1/userEvaluations/{another user's UID}` ✗
  - Unauthenticated request cannot `read` anything ✗

**Step 7: Plan UiState state machines**

Define the states each screen can be in before writing any ViewModel code.

`LoginUiState` — simple form state:
```kotlin
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isLoginSuccess: Boolean = false,
    val error: String? = null          // null = no error shown
)
```

`UserUiState` — loading + tab navigation + evaluation editor:

> **Replace**, do not extend: the existing `UserUiState.kt` is a `data class`. Delete its entire content and substitute the sealed class below. After replacing, update all references in `UserViewModel.kt`, `UserFragment.kt`, and every `Screen.kt` under `user/` — they access the old flat fields (`isLoading`, `selectedItem`, etc.) which no longer exist on the sealed class.

```kotlin
sealed class UserUiState {
    object Loading : UserUiState()
    data class Error(val message: String) : UserUiState()
    data class Ready(
        // Item and Feature here are core.model.Item / core.model.Feature — NOT the UI models
        // in user/items/Item.kt or user/features/Item.kt
        val items: List<Item>,
        val features: List<Feature>,
        val scoreMatrix: ScoreMatrix,
        val currentUserId: String,
        val activeTab: Tab = Tab.ITEMS,
        val selectedItem: Item? = null,           // non-null = item detail is open
        val selectedFeature: Feature? = null,     // non-null = feature detail is open
        val evaluationEditor: EvaluationEditorState? = null  // non-null = editor is open
    ) : UserUiState()

    enum class Tab { ITEMS, FEATURES }

    data class EvaluationEditorState(
        val featureId: String,
        val orderedItemIds: List<String>,   // live order as user drags items
        val isSaving: Boolean = false,
        val saveError: String? = null
    )
}
```

`CompilationUiState` — loading + read-only tab navigation:

> **Replace**, do not extend: the existing `CompilationUiState.kt` is a `data class`. Delete its content and substitute the sealed class below. Update `CompilationViewModel.kt`, `CompilationFragment.kt`, and every `Screen.kt` under `compilation/`.

```kotlin
sealed class CompilationUiState {
    object Loading : CompilationUiState()
    data class Error(val message: String) : CompilationUiState()
    data class Ready(
        // Item and Feature are core.model.Item / core.model.Feature — NOT UI models
        val items: List<Item>,
        val features: List<Feature>,
        val scoreMatrix: ScoreMatrix,
        val activeTab: Tab = Tab.ITEMS,
        val selectedItem: Item? = null,
        val selectedFeature: Feature? = null,
        val mapPopupItem: Item? = null,   // non-null = popup visible on map tab
        // Map axis selection. Null means "use the list index default".
        // Default: features[0] on X, features[1] on Y (or features[0] if only one exists).
        val mapXFeatureId: String? = null,
        val mapYFeatureId: String? = null
    ) : CompilationUiState()

    enum class Tab { ITEMS, FEATURES, MAP }
}
```

- [ ] Update the **existing** `LoginUiState.kt`: rename field `username` → `email` and field `errorMessage` → `error` to match the definition shown above. (`isLoginSuccess: Boolean = false` already exists in the file — keep it.) No other changes to `LoginUiState.kt` are needed.
- [ ] **Replace** `UserUiState.kt` entirely with the sealed class above (delete the old `data class` content first)
- [ ] **Replace** `CompilationUiState.kt` entirely with the sealed class above
- [ ] Do not wire them to ViewModels yet — wiring happens in Phase 4

---

### Phase 2 — Core Logic Implementation

**Step 8: Implement EvaluationConverter**

File: `app/src/main/java/com/dirtfy/srr/core/scoring/EvaluationConverter.kt`

- [ ] Create the file:
  ```kotlin
  object EvaluationConverter {
      // orderedItemIds[0] has the strongest feature presence → score +1.0
      // orderedItemIds[last] has the weakest → score -1.0
      // Items not in the list are excluded (return no entry in the result map).
      fun convert(orderedItemIds: List<String>): Map<String, Double> {
          val n = orderedItemIds.size
          if (n == 0) return emptyMap()
          if (n == 1) return mapOf(orderedItemIds[0] to 0.0)
          val step = 2.0 / (n - 1)
          return orderedItemIds.mapIndexed { i, id ->
              id to (1.0 - i * step)
          }.toMap()
      }
  }
  ```
- [ ] Verify the output manually for n=1, n=2, n=3 before moving on:
  - `["A"]` → `{A: 0.0}`
  - `["A","B"]` → `{A: +1.0, B: -1.0}`
  - `["A","B","C"]` → `{A: +1.0, B: 0.0, C: -1.0}`

**Step 9: Implement ScoreAggregator**

File: `app/src/main/java/com/dirtfy/srr/core/scoring/ScoreAggregator.kt`

- [ ] Create the file:
  ```kotlin
  object ScoreAggregator {
      // Aggregates all evaluations for one feature into a per-item average.
      // allItemIds: the full item catalogue — ensures every item has an entry in the result,
      //             even if no one evaluated it.
      // Returns null for an item if fewer than minVotes users evaluated it.
      fun aggregate(
          evaluations: List<Evaluation>,
          allItemIds: List<String>,
          minVotes: Int
      ): Map<String, Double?> {
          val collectedScores: Map<String, MutableList<Double>> =
              allItemIds.associateWith { mutableListOf() }

          for (evaluation in evaluations) {
              val scores = EvaluationConverter.convert(evaluation.orderedItemIds)
              for ((itemId, score) in scores) {
                  collectedScores[itemId]?.add(score)
              }
          }

          return collectedScores.mapValues { (_, scores) ->
              if (scores.size < minVotes) null else scores.average()
          }
      }
  }
  ```

**Step 10: Implement ScoreRemapper**

File: `app/src/main/java/com/dirtfy/srr/core/scoring/ScoreRemapper.kt`

- [ ] Create the file:
  ```kotlin
  object ScoreRemapper {
      // [-1.0, +1.0] → [0.0, 10.0]
      // -1.0 → 0.0,  0.0 → 5.0,  +1.0 → 10.0
      fun remap(raw: Double): Double = (raw + 1.0) / 2.0 * 10.0
      fun remapOrNull(raw: Double?): Double? = raw?.let { remap(it) }
  }
  ```

**Step 11: Implement DefaultFeatureScoringEngine**

File: `app/src/main/java/com/dirtfy/srr/core/scoring/DefaultFeatureScoringEngine.kt`

- [ ] Create the file:
  ```kotlin
  class DefaultFeatureScoringEngine : FeatureScoringEngine {
      override fun computeScores(
          allItemIds: List<String>,
          allFeatureIds: List<String>,
          evaluationsByFeature: Map<String, List<Evaluation>>,
          minVoteThreshold: Int
      ): ScoreMatrix {
          val scores     = mutableMapOf<String, MutableMap<String, Double?>>()
          val voteCounts = mutableMapOf<String, MutableMap<String, Int>>()

          for (featureId in allFeatureIds) {
              val evaluations = evaluationsByFeature[featureId] ?: emptyList()
              val rawAverages = ScoreAggregator.aggregate(evaluations, allItemIds, minVoteThreshold)

              for (itemId in allItemIds) {
                  val displayScore = ScoreRemapper.remapOrNull(rawAverages[itemId])
                  val voteCount    = evaluations.count { itemId in it.orderedItemIds }

                  scores    .getOrPut(itemId) { mutableMapOf() }[featureId] = displayScore
                  voteCounts.getOrPut(itemId) { mutableMapOf() }[featureId] = voteCount
              }
          }

          return ScoreMatrix(
              scores     = scores.mapValues { it.value.toMap() },
              voteCounts = voteCounts.mapValues { it.value.toMap() }
          )
      }
  }
  ```
- [ ] Trace through manually with the README example (2 users, feature `durability`) and confirm output matches the table in the Scoring Algorithm section

---

### Phase 3 — Firebase Data Layer Implementation

**Step 12: Add Firebase dependencies**

- [ ] Check the latest Firebase BOM version at `firebase.google.com/docs/android/setup` and the latest Google Services plugin version at `developers.google.com/android/guides/google-services-plugin`
- [ ] In `gradle/libs.versions.toml`, add:
  ```toml
  [versions]
  firebase-bom    = "33.x.x"   # replace with latest
  google-services = "4.4.x"    # replace with latest
  reorderable     = "2.x.x"    # check latest at github.com/Calvin-LL/Reorderable

  [libraries]
  firebase-bom           = { module = "com.google.firebase:firebase-bom",              version.ref = "firebase-bom" }
  firebase-auth-ktx      = { module = "com.google.firebase:firebase-auth-ktx" }
  firebase-firestore-ktx = { module = "com.google.firebase:firebase-firestore-ktx" }
  reorderable            = { module = "sh.calvin.reorderable:reorderable",             version.ref = "reorderable" }

  [plugins]
  google-services = { id = "com.google.gms.google-services", version.ref = "google-services" }
  ```
- [ ] In project-level `build.gradle.kts`, add inside `plugins {}`:
  ```kotlin
  alias(libs.plugins.google.services) apply false
  ```
- [ ] In `app/build.gradle.kts`, add inside `plugins {}`:
  ```kotlin
  alias(libs.plugins.google.services)
  ```
  And inside `dependencies {}`:
  ```kotlin
  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.auth.ktx)
  implementation(libs.firebase.firestore.ktx)
  // firebase-firestore-ktx transitively includes kotlinx-coroutines-play-services,
  // which provides the .await() extension used on Firebase Tasks throughout this layer.
  implementation(libs.reorderable)   // drag-to-reorder in the evaluation editor
  implementation(libs.androidx.material.icons.extended)   // for ExitToApp icon (Step 20)
  ```
- [ ] Place `google-services.json` (downloaded in Step 1) in the `app/` directory
- [ ] Run `./gradlew assembleDebug` and confirm the build succeeds with no errors before writing implementation code

**Step 13: Implement RemoteUserAccountRepository**

File: `app/src/main/java/com/dirtfy/srr/remote/RemoteUserAccountRepository.kt`

- [ ] Create the class:
  ```kotlin
  import com.google.firebase.auth.ktx.auth
  import com.google.firebase.ktx.Firebase
  import kotlinx.coroutines.tasks.await

  class RemoteUserAccountRepository : UserAccountRepository {
      private val auth = Firebase.auth

      override suspend fun signIn(email: String, password: String): Result<Unit> =
          runCatching {
              auth.signInWithEmailAndPassword(email, password).await()
              // .await() throws FirebaseAuthInvalidCredentialsException on wrong password,
              // FirebaseAuthInvalidUserException if the email does not exist, etc.
              // runCatching wraps all exceptions into Result.failure.
          }

      override suspend fun signUp(email: String, password: String): Result<Unit> =
          runCatching {
              auth.createUserWithEmailAndPassword(email, password).await()
              // throws FirebaseAuthWeakPasswordException if password < 6 chars,
              // FirebaseAuthUserCollisionException if the email is already registered.
          }

      override fun signOut() {
          auth.signOut()
      }

      override fun currentUserId(): String? = auth.currentUser?.uid
  }
  ```
- [ ] Manual smoke test before wiring the UI: in a temporary scratch location, instantiate the repository, call `signUp("test@example.com", "password123")`, then `signIn`, then verify `currentUserId()` returns a non-null string, then `signOut()` and confirm `currentUserId()` returns null

**Step 14: Implement RemoteEvaluationRepository**

File: `app/src/main/java/com/dirtfy/srr/remote/RemoteEvaluationRepository.kt`

- [ ] Create the class:
  ```kotlin
  import com.google.firebase.Timestamp
  import com.google.firebase.firestore.ktx.firestore
  import com.google.firebase.ktx.Firebase
  import kotlinx.coroutines.tasks.await

  class RemoteEvaluationRepository : EvaluationRepository {
      private val db = Firebase.firestore

      override suspend fun submitEvaluation(evaluation: Evaluation): Result<Unit> =
          runCatching {
              val record = hashMapOf(
                  "orderedItemIds" to evaluation.orderedItemIds,
                  "submittedAt"    to Timestamp.now()
              )
              db.collection("evaluations")
                  .document(evaluation.featureId)
                  .collection("userEvaluations")
                  .document(evaluation.userId)   // document ID = user's Firebase UID
                  .set(record)                   // .set() overwrites the whole document;
                  .await()                       // re-submitting replaces the previous evaluation
          }

      @Suppress("UNCHECKED_CAST")
      override suspend fun getEvaluationsForFeature(featureId: String): Result<List<Evaluation>> =
          runCatching {
              db.collection("evaluations")
                  .document(featureId)
                  .collection("userEvaluations")
                  .get()
                  .await()
                  .documents
                  .map { doc ->
                      Evaluation(
                          userId         = doc.id,
                          featureId      = featureId,
                          orderedItemIds = (doc.get("orderedItemIds") as? List<String>)
                                              ?: emptyList()
                      )
                  }
              // .get() returns an empty QuerySnapshot (not an error) if the subcollection
              // does not exist yet, so .documents will be an empty list — which is correct.
          }
  }
  ```
- [ ] Manual smoke test via the Firestore console: call `submitEvaluation(Evaluation("uid123", "feature_durability", listOf("item_b","item_c","item_a")))` and verify a document appears at `evaluations/feature_durability/userEvaluations/uid123` with the correct `orderedItemIds` array

**Step 15: Implement RemoteItemRepository, RemoteFeatureRepository, and EvaluationRecord**

Files: `app/src/main/java/com/dirtfy/srr/remote/RemoteItemRepository.kt`, `RemoteFeatureRepository.kt`, and `remote/model/EvaluationRecord.kt`

- [ ] Create `remote/model/EvaluationRecord.kt` (Firestore document shape — placed here because it needs the Firebase SDK added in Step 12):
  ```kotlin
  import com.google.firebase.Timestamp

  // All fields must have defaults so Firestore can deserialize with a no-arg constructor.
  data class EvaluationRecord(
      val orderedItemIds: List<String> = emptyList(),
      val submittedAt: Timestamp = Timestamp.now()
  )
  ```
  Note: `EvaluationRecord` is only used internally in `RemoteEvaluationRepository` for deserialization. If you used `hashMapOf()` directly in Step 14's `submitEvaluation` (as shown), you do not need `EvaluationRecord` for writes — only for reads if you choose to use `toObject(EvaluationRecord::class.java)` instead of `doc.get(...)` field access.

- [ ] `RemoteItemRepository.kt`:
  ```kotlin
  import com.google.firebase.firestore.ktx.firestore
  import com.google.firebase.ktx.Firebase
  import kotlinx.coroutines.tasks.await

  class RemoteItemRepository : ItemRepository {
      private val db = Firebase.firestore

      override suspend fun getAllItems(): Result<List<Item>> =
          runCatching {
              db.collection("items")
                  .get()
                  .await()
                  .documents
                  .map { doc ->
                      Item(
                          id   = doc.id,
                          name = doc.getString("name") ?: ""
                      )
                  }
          }
  }
  ```

- [ ] `RemoteFeatureRepository.kt`:
  ```kotlin
  import com.google.firebase.firestore.ktx.firestore
  import com.google.firebase.ktx.Firebase
  import kotlinx.coroutines.tasks.await

  class RemoteFeatureRepository : FeatureRepository {
      private val db = Firebase.firestore

      override suspend fun getAllFeatures(): Result<List<Feature>> =
          runCatching {
              db.collection("features")
                  .get()
                  .await()
                  .documents
                  .map { doc ->
                      Feature(
                          id   = doc.id,
                          name = doc.getString("name") ?: ""
                      )
                  }
          }
  }
  ```
- [ ] Verify against the data seeded in Step 2: `getAllItems()` must return the exact items you inserted; `getAllFeatures()` must return the exact features

---

### Phase 4 — Use Cases and UI Wiring

#### Dependency wiring (no DI framework)

There is no Hilt, Koin, or other DI framework. Each ViewModel declares a `companion object { fun factory() }` that constructs all concrete dependencies inline. The Fragment uses `by viewModels { ViewModel.factory() }`.

`LoginViewModel` factory:
```kotlin
companion object {
    fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LoginViewModel(
                userAccountRepository = RemoteUserAccountRepository()
            ) as T
    }
}
```

`UserViewModel` factory — note `evaluationRepo` is created once and shared between the ViewModel and the use case so there is only one Firestore client instance:
```kotlin
companion object {
    fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val evaluationRepo = RemoteEvaluationRepository()
            return UserViewModel(
                userAccountRepository    = RemoteUserAccountRepository(),
                evaluationRepository     = evaluationRepo,
                loadFeatureScoresUseCase = LoadFeatureScoresUseCase(
                    itemRepository       = RemoteItemRepository(),
                    featureRepository    = RemoteFeatureRepository(),
                    evaluationRepository = evaluationRepo,
                    scoringEngine        = DefaultFeatureScoringEngine()
                )
            ) as T
        }
    }
}
```

`CompilationViewModel` factory:
```kotlin
companion object {
    fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CompilationViewModel(
                loadFeatureScoresUseCase = LoadFeatureScoresUseCase(
                    itemRepository       = RemoteItemRepository(),
                    featureRepository    = RemoteFeatureRepository(),
                    evaluationRepository = RemoteEvaluationRepository(),
                    scoringEngine        = DefaultFeatureScoringEngine()
                )
            ) as T
    }
}
```

Each Fragment wires in its factory:
```kotlin
// UserFragment.kt
private val viewModel: UserViewModel by viewModels { UserViewModel.factory() }

// CompilationFragment.kt
private val viewModel: CompilationViewModel by viewModels { CompilationViewModel.factory() }
```

`LoginFragment` passes the factory to the Compose-side `viewModel()` call:
```kotlin
// LoginFragment.kt  (inside setContent { SRRTheme { ... } })
LoginScreen(
    viewModel = viewModel(factory = LoginViewModel.factory()),
    onLoginSuccess = { (activity as? MainActivity)?.navigateToMine() }
)
```

---

**Step 16: Implement LoadFeatureScoresUseCase**

File: `app/src/main/java/com/dirtfy/srr/core/usecase/LoadFeatureScoresUseCase.kt`

This use case is shared by both `UserViewModel` and `CompilationViewModel` to avoid duplicated loading logic.

- [ ] Create the class:
  ```kotlin
  class LoadFeatureScoresUseCase(
      private val itemRepository: ItemRepository,
      private val featureRepository: FeatureRepository,
      private val evaluationRepository: EvaluationRepository,
      private val scoringEngine: FeatureScoringEngine
  ) {
      data class Output(
          val items: List<Item>,
          val features: List<Feature>,
          val scoreMatrix: ScoreMatrix,
          // Raw evaluations kept here so ViewModels can pre-populate the evaluation editor
          // without an extra Firestore round-trip. ScoreMatrix stores only computed scores.
          val evaluationsByFeature: Map<String, List<Evaluation>>
      )

      suspend fun execute(): Result<Output> = runCatching {
          val items    = itemRepository.getAllItems().getOrThrow()
          val features = featureRepository.getAllFeatures().getOrThrow()

          val evaluationsByFeature: Map<String, List<Evaluation>> =
              features.associate { feature ->
                  feature.id to evaluationRepository
                      .getEvaluationsForFeature(feature.id)
                      .getOrThrow()
              }

          val scoreMatrix = scoringEngine.computeScores(
              allItemIds           = items.map { it.id },
              allFeatureIds        = features.map { it.id },
              evaluationsByFeature = evaluationsByFeature,
              minVoteThreshold     = 3
          )

          Output(items, features, scoreMatrix, evaluationsByFeature)
      }
  }
  ```

**Step 17: Update LoginViewModel and LoginScreen**

File: `ui/performer/login/LoginViewModel.kt`

The existing `LoginViewModel` already uses `isLoginSuccess: Boolean` in `LoginUiState` and `LoginFragment` already observes it via a callback. Keep this pattern — do not change it to a callback in the ViewModel constructor.

- [ ] Add `UserAccountRepository` as a constructor parameter: `class LoginViewModel(private val userAccountRepository: UserAccountRepository) : ViewModel()`
- [ ] Add the `companion object { fun factory() }` (shown in the Dependency wiring section above)
- [ ] Rename `onUsernameChange` → `onEmailChange` to match `LoginUiState.email`
- [ ] Replace the mock `login()` body with real Firebase calls:
  ```kotlin
  fun login() {
      viewModelScope.launch {
          _uiState.update { it.copy(isLoading = true, error = null) }
          userAccountRepository.signIn(uiState.value.email, uiState.value.password)
              .onSuccess { _uiState.update { it.copy(isLoading = false, isLoginSuccess = true) } }
              .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message ?: "Sign-in failed") } }
      }
  }
  fun signUp() {
      viewModelScope.launch {
          _uiState.update { it.copy(isLoading = true, error = null) }
          userAccountRepository.signUp(uiState.value.email, uiState.value.password)
              .onSuccess { _uiState.update { it.copy(isLoading = false, isLoginSuccess = true) } }
              .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message ?: "Sign-up failed") } }
              // Firebase createUserWithEmailAndPassword auto-signs-in on success,
              // so isLoginSuccess = true is correct here too.
      }
  }
  fun resetLoginStatus() {
      _uiState.update { it.copy(isLoginSuccess = false) }
  }
  fun isAlreadySignedIn(): Boolean = userAccountRepository.currentUserId() != null
  ```
- [ ] In `onEmailChange` and `onPasswordChange`, clear `error` (not `errorMessage`): `_uiState.update { it.copy(email = newValue, error = null) }` — the field was renamed from `errorMessage` in the existing code
- [ ] Add the four functions shown in the code block above (`login()` replaces the mock body; `signUp()`, `resetLoginStatus()`, `isAlreadySignedIn()` are new — except `resetLoginStatus()` which already exists and just needs to be kept)
- [ ] In `LoginScreen` composable, add an auto-login `LaunchedEffect` before showing the form:
  ```kotlin
  LaunchedEffect(Unit) {
      if (viewModel.isAlreadySignedIn()) onLoginSuccess()
  }
  ```
  Firebase Auth caches the session locally, so this check is synchronous and does not hit the network.

File: `ui/window/component/LoginFragment.kt`

- [ ] Change `viewModel()` → `viewModel(factory = LoginViewModel.factory())` (the `onLoginSuccess` callback stays as-is — it already calls `(activity as? MainActivity)?.navigateToMine()`)
- [ ] `MainActivity.navigateToMine()` already exists — no change needed there
- [ ] Show `CircularProgressIndicator` while `isLoading = true`
- [ ] Show a `Text` with `uiState.error` below the form when it is non-null
- [ ] After `isLoginSuccess` triggers navigation, the existing `resetLoginStatus()` prevents re-triggering if the user ever navigates back — no additional code needed beyond keeping that method

**Step 18: Update UserViewModel and User screens**

File: `ui/performer/user/UserViewModel.kt`

- [ ] Add constructor parameters and factory (from the Dependency wiring section):
  ```kotlin
  class UserViewModel(
      private val userAccountRepository: UserAccountRepository,
      private val evaluationRepository: EvaluationRepository,
      private val loadFeatureScoresUseCase: LoadFeatureScoresUseCase
  ) : ViewModel() {
      // Raw evaluations kept in a private field, not in UiState, so the editor
      // can pre-populate without an extra Firestore call.
      private var cachedEvaluations: Map<String, List<Evaluation>> = emptyMap()
      // companion object { fun factory() } — see the Dependency wiring section above
  }
  ```
- [ ] Expose `StateFlow<UserUiState>` replacing the existing `StateFlow<UserUiState>` (old field types change)
- [ ] Replace `loadAllData()` body:
  ```kotlin
  fun loadAllData() {
      viewModelScope.launch {
          _uiState.value = UserUiState.Loading
          loadFeatureScoresUseCase.execute()
              .onSuccess { output ->
                  cachedEvaluations = output.evaluationsByFeature
                  _uiState.value = UserUiState.Ready(
                      items         = output.items,
                      features      = output.features,
                      scoreMatrix   = output.scoreMatrix,
                      currentUserId = userAccountRepository.currentUserId() ?: ""
                  )
              }
              .onFailure { e -> _uiState.value = UserUiState.Error(e.message ?: "Failed to load") }
      }
  }
  ```
- [ ] Add `fun onRetryTap()`: call `loadAllData()` (already launches in `viewModelScope`)
- [ ] Add `fun onTabSelected(tab: UserUiState.Tab)`: `(_uiState.value as? UserUiState.Ready)?.let { _uiState.value = it.copy(activeTab = tab) }`
- [ ] Add `fun onItemSelected(item: Item)` and `fun onFeatureSelected(feature: Feature)`: same `.copy()` pattern
- [ ] Add `fun onOpenEvaluationEditor(featureId: String)`:
  ```kotlin
  fun onOpenEvaluationEditor(featureId: String) {
      val state = _uiState.value as? UserUiState.Ready ?: return
      val existingOrder = cachedEvaluations[featureId]
          ?.find { it.userId == state.currentUserId }
          ?.orderedItemIds
          ?: state.items.map { it.id }   // default: all items in catalogue order
      _uiState.value = state.copy(
          evaluationEditor = UserUiState.EvaluationEditorState(featureId, existingOrder)
      )
  }
  ```
- [ ] Add `fun onEvaluationReorder(newOrder: List<String>)`:
  ```kotlin
  fun onEvaluationReorder(newOrder: List<String>) {
      val state = _uiState.value as? UserUiState.Ready ?: return
      val editor = state.evaluationEditor ?: return
      _uiState.value = state.copy(evaluationEditor = editor.copy(orderedItemIds = newOrder))
  }
  ```
- [ ] Add `fun onSubmitEvaluation()`:
  ```kotlin
  fun onSubmitEvaluation() {
      val state  = _uiState.value as? UserUiState.Ready ?: return
      val editor = state.evaluationEditor ?: return
      _uiState.value = state.copy(evaluationEditor = editor.copy(isSaving = true, saveError = null))
      viewModelScope.launch {
          evaluationRepository.submitEvaluation(
              Evaluation(state.currentUserId, editor.featureId, editor.orderedItemIds)
          )
          .onSuccess { loadAllData() }   // reloads and closes editor (Loading wipes editor state)
          .onFailure { e ->
              val s = _uiState.value as? UserUiState.Ready ?: return@onFailure
              _uiState.value = s.copy(evaluationEditor = editor.copy(isSaving = false, saveError = e.message))
          }
      }
  }
  ```
- [ ] Add `fun clearSelection()` replacing the existing one — now delegates to the sealed class:
  ```kotlin
  fun clearSelection() {
      val state = _uiState.value as? UserUiState.Ready ?: return
      _uiState.value = when {
          state.evaluationEditor != null -> state.copy(evaluationEditor = null)
          state.selectedFeature  != null -> state.copy(selectedFeature = null)
          else                           -> state.copy(selectedItem = null)
      }
  }
  ```

File: `ui/window/component/UserFragment.kt`

Back navigation is handled in the Fragment's existing `onBackClick()` override — do **not** add `BackHandler` in Compose. Update `onBackClick()` to call `viewModel.clearSelection()` (same as today, but the ViewModel method now understands the sealed class).

The existing ViewModel methods used by `UserFragment` must be renamed or replaced as follows:

| Old method (existing) | New method (Step 18) | Note |
|---|---|---|
| `toggleView()` | `onTabSelected(tab: UserUiState.Tab)` | replaces boolean toggle |
| `selectItem(item: GridItem)` | `onItemSelected(item: Item)` | type changes to `core.model.Item` |
| `selectFeature(feature: RatingItem)` | `onFeatureSelected(feature: Feature)` | type changes to `core.model.Feature` |
| `selectSubItem(subItem: DetailSubItem)` | `onOpenEvaluationEditor(featureId: String)` | different semantics entirely |
| `dismissSubItemPopup()` | removed — `clearSelection()` closes the editor | |
| `clearFeatureSelection()` | removed — `clearSelection()` handles all levels | |
| `clearSelection()` | `clearSelection()` | stays; body changes for sealed class |

- [ ] Change `by viewModels()` → `by viewModels { UserViewModel.factory() }`
- [ ] Replace `provideTitle()` to cast to `UserUiState.Ready`:
  ```kotlin
  @Composable
  override fun provideTitle(): String {
      val uiState by viewModel.uiState.collectAsStateWithLifecycle()
      val state = uiState as? UserUiState.Ready ?: return "My Dashboard"
      return when {
          state.evaluationEditor != null ->
              state.features.find { it.id == state.evaluationEditor.featureId }?.name ?: "Evaluate"
          state.selectedItem != null -> state.selectedItem.name
          state.selectedFeature != null -> state.selectedFeature.name
          else -> "My Dashboard"
      }
  }
  ```
- [ ] Replace `shouldShowBackButton()`:
  ```kotlin
  @Composable
  override fun shouldShowBackButton(): Boolean {
      val uiState by viewModel.uiState.collectAsStateWithLifecycle()
      val state = uiState as? UserUiState.Ready ?: return false
      return state.selectedItem != null || state.selectedFeature != null || state.evaluationEditor != null
  }
  ```
- [ ] Replace `onBackClick()` — reads `.value` directly (not Composable):
  ```kotlin
  override fun onBackClick() {
      val state = viewModel.uiState.value as? UserUiState.Ready
      if (state != null && (state.selectedItem != null || state.selectedFeature != null || state.evaluationEditor != null)) {
          viewModel.clearSelection()
      } else {
          super.onBackClick()
      }
  }
  ```

File: `ui/performer/user/` screens

The existing top-level composable is called `MineMainScreen` (in `user/Screen.kt`). Rename it to `UserScreen` (or keep the existing name — both work; just be consistent). Its callback parameters all change from the old ViewModel methods to the new ones:

| Old callback parameter | New callback parameter |
|---|---|
| `onToggleView: () -> Unit` | `onTabSelected: (UserUiState.Tab) -> Unit` |
| `onItemClick: (GridItem) -> Unit` | `onItemSelected: (Item) -> Unit` |
| `onFeatureClick: (RatingItem) -> Unit` | `onFeatureSelected: (Feature) -> Unit` |
| `onSubItemClick: (DetailSubItem) -> Unit` | `onOpenEvaluationEditor: (featureId: String) -> Unit` |
| `onDismissSubPopup: () -> Unit` | removed — back navigation handles this via `clearSelection()` |
| `onBackToGrid: () -> Unit` | removed — back navigation handles this via `clearSelection()` |

- [ ] Update `UserFragment.ScreenContent()` to pass the new lambdas to the renamed composable
- [ ] In the top-level composable: `when (uiState) { is UserUiState.Loading → spinner; is UserUiState.Error → error text + Retry button calling `onRetryTap()`; is UserUiState.Ready → tab content }`
- [ ] In item detail: display `scoreMatrix.scores[item.id]?.get(featureId)` formatted to 1 decimal (`"%.1f".format(score)`), or `"—"` if null
- [ ] In feature detail: items sorted by `scoreMatrix.scores[item.id]?.get(feature.id)` descending (nulls last); `"—"` for null; "Evaluate" button calls `onOpenEvaluationEditor(feature.id)`
- [ ] In evaluation editor (`features/detail/Popup.kt`): render a `ReorderableColumn` or `LazyColumn` with `ReorderableItem` from `sh.calvin.reorderable`; each row has a drag handle icon; **Submit** button calls `onSubmitEvaluation()`; spinner visible while `isSaving`; `saveError` shown as red text below the list

**Step 19: Update CompilationViewModel and Compilation screens**

File: `ui/performer/compilation/CompilationViewModel.kt`

- [ ] Add constructor parameter and factory:
  ```kotlin
  class CompilationViewModel(
      private val loadFeatureScoresUseCase: LoadFeatureScoresUseCase
  ) : ViewModel() {
      // companion object { fun factory() } — see the Dependency wiring section above
  }
  ```
- [ ] Expose `StateFlow<CompilationUiState>`; replace existing mock `loadAllData()` body with the same pattern as `UserViewModel` (emit Loading → call use case → emit Ready or Error); `evaluationsByFeature` from the output is not needed here — discard it
- [ ] Change `by viewModels()` in `CompilationFragment` → `by viewModels { CompilationViewModel.factory() }`
- [ ] Add `fun onRetryTap()`, `fun onTabSelected(tab)`, `fun onItemSelected(item)`, `fun onFeatureSelected(feature)`, `fun onMapItemTap(item: Item)` (sets `mapPopupItem`)
- [ ] Add `fun clearSelection()` that clears whichever detail/popup is open — one level at a time:
  ```kotlin
  fun clearSelection() {
      val state = _uiState.value as? CompilationUiState.Ready ?: return
      _uiState.value = when {
          state.mapPopupItem  != null -> state.copy(mapPopupItem = null)
          state.selectedItem  != null -> state.copy(selectedItem = null)
          else                        -> state.copy(selectedFeature = null)
      }
  }
  ```
- [ ] Add `fun onMapXFeatureSelected(featureId: String)` and `fun onMapYFeatureSelected(featureId: String)`: copy `Ready` state with the respective field updated
- [ ] `CompilationUiState.Ready` is **read-only** — no evaluation submission

File: `ui/window/component/CompilationFragment.kt`

- [ ] Change `by viewModels()` → `by viewModels { CompilationViewModel.factory() }`
- [ ] Replace `provideTitle()`:
  ```kotlin
  @Composable
  override fun provideTitle(): String {
      val uiState by viewModel.uiState.collectAsStateWithLifecycle()
      val state = uiState as? CompilationUiState.Ready ?: return "Results"
      return when {
          state.selectedItem    != null -> state.selectedItem.name
          state.selectedFeature != null -> state.selectedFeature.name
          else -> "Results"   // map popup is an overlay, not a new "page", so title stays
      }
  }
  ```
- [ ] Replace `shouldShowBackButton()`:
  ```kotlin
  @Composable
  override fun shouldShowBackButton(): Boolean {
      val uiState by viewModel.uiState.collectAsStateWithLifecycle()
      val state = uiState as? CompilationUiState.Ready ?: return false
      return state.selectedItem != null || state.selectedFeature != null || state.mapPopupItem != null
  }
  ```
- [ ] Replace `onBackClick()`:
  ```kotlin
  override fun onBackClick() {
      val state = viewModel.uiState.value as? CompilationUiState.Ready
      if (state != null && (state.selectedItem != null || state.selectedFeature != null || state.mapPopupItem != null)) {
          viewModel.clearSelection()
      } else {
          super.onBackClick()
      }
  }
  ```

File: `ui/performer/compilation/` screens

- [ ] Items tab: grid of items; tapping opens item detail showing scores per feature (null → `"—"`)
- [ ] Features tab: list of features; tapping opens feature detail showing items ordered by score (null → `"—"`)
- [ ] Map tab:
  - Resolve axes at render time:
    ```kotlin
    val xFeature = features.find { it.id == state.mapXFeatureId } ?: features.getOrNull(0)
    val yFeature = features.find { it.id == state.mapYFeatureId } ?: features.getOrNull(1) ?: features.getOrNull(0)
    ```
  - Show two `DropdownMenu`s at the top of the screen (one for X axis, one for Y) populated with all features; call `onMapXFeatureSelected` / `onMapYFeatureSelected` on selection
  - Each item point: `x = scoreMatrix.scores[item.id]?.get(xFeature.id) ?: 5.0`, `y = scoreMatrix.scores[item.id]?.get(yFeature.id) ?: 5.0` (5.0 = neutral midpoint for unscored pairs)
  - Plot coordinates are normalized: divide the `[0, 10]` display score by 10 to get a `[0f, 1f]` canvas fraction before multiplying by the canvas size
  - Tapping a point sets `mapPopupItem`; the popup shows the item name and its scores on all features
- [ ] All tabs: `"—"` wherever score is null; no edit controls anywhere in this screen

**Step 20: Add sign-out button**

File: `ui/performer/base/Screen.kt` (`SRRBaseScreen`)

- [ ] Add `actions: @Composable RowScope.() -> Unit = {}` parameter and wire it into `CenterAlignedTopAppBar`:
  ```kotlin
  fun SRRBaseScreen(
      title: String,
      showBackButton: Boolean,
      onBackClick: () -> Unit,
      navigationItems: List<Item>,
      currentRoute: String,
      onTabClick: (Item) -> Unit = {},
      actions: @Composable RowScope.() -> Unit = {},   // add this
      content: @Composable (Modifier) -> Unit
  ) {
      Scaffold(
          topBar = {
              CenterAlignedTopAppBar(
                  title = { Text(text = title) },
                  navigationIcon = { /* back button — unchanged */ },
                  actions = actions                     // add this
              )
          }, ...
      )
  }
  ```

File: `ui/window/component/BaseFragment.kt`

- [ ] Add an open `@Composable` function `TopBarActions` with a default no-op body, and pass it to `SRRBaseScreen`:
  ```kotlin
  @Composable
  open fun TopBarActions() {}   // subclasses override to add icons

  // In onCreateView, update SRRBaseScreen call:
  SRRBaseScreen(
      ...,
      actions = { TopBarActions() },
      ...
  )
  ```

File: `ui/window/component/UserFragment.kt` and `CompilationFragment.kt`

- [ ] Override `TopBarActions()` in both fragments:
  ```kotlin
  @Composable
  override fun TopBarActions() {
      IconButton(onClick = { (activity as? MainActivity)?.signOut() }) {
          Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sign out")
      }
  }
  ```
  `Icons.AutoMirrored.Filled.ExitToApp` is from the extended icons set — the `implementation(libs.androidx.material.icons.extended)` dependency added in Step 12 covers this.

File: `ui/window/MainActivity.kt`

- [ ] Add `fun signOut()`:
  ```kotlin
  fun signOut() {
      Firebase.auth.signOut()   // deliberate direct SDK call; no repo abstraction needed here
      logout()
  }
  ```
  Import: `com.google.firebase.ktx.Firebase`. This is a pragmatic exception to the layer rule — `signOut()` has no repository logic to abstract and going through a repository instance would require constructing one in `MainActivity`.

---

### Phase 5 — Quality

**Step 21: Unit test EvaluationConverter**

File: `app/src/test/java/com/dirtfy/srr/core/scoring/EvaluationConverterTest.kt`

- [ ] Test cases:
  - `convert(emptyList())` → empty map
  - `convert(listOf("A"))` → `{A: 0.0}` (single item gets neutral score)
  - `convert(listOf("A", "B"))` → `{A: +1.0, B: -1.0}`
  - `convert(listOf("A", "B", "C"))` → `{A: +1.0, B: 0.0, C: -1.0}`
  - `convert(listOf("A", "B", "C", "D"))` → `{A: +1.0, B: ≈+0.333, C: ≈-0.333, D: -1.0}` (compare within delta 0.001)
  - Items not in the input list are absent from the result map: `assertFalse(result.containsKey("Z"))`

**Step 22: Unit test ScoreAggregator**

File: `app/src/test/java/com/dirtfy/srr/core/scoring/ScoreAggregatorTest.kt`

- [ ] Test cases:
  - Zero evaluations, minVotes=3 → all items return `null`
  - Two evaluations (below threshold=3) → all items return `null`
  - Three identical evaluations `[A,B,C]` → `{A: +1.0, B: 0.0, C: -1.0}` (average equals individual score)
  - Three evaluations: User1=`[A,B,C]`, User2=`[C,B,A]`, User3=`[A,B,C]` → `{A: (1-1+1)/3, B: 0.0, C: (-1+1-1)/3}`
  - Item in `allItemIds` but absent from all evaluations → `null` (not an error or exception)
  - User evaluates only `[A, C]` (skips B): B gains zero vote count from this user; A and C each gain one vote count from this user

**Step 23: Unit test ScoreRemapper**

File: `app/src/test/java/com/dirtfy/srr/core/scoring/ScoreRemapperTest.kt`

- [ ] Test cases:
  - `remap(-1.0)` → `0.0`
  - `remap(0.0)` → `5.0`
  - `remap(+1.0)` → `10.0`
  - `remap(-0.5)` → `2.5`
  - `remap(+0.5)` → `7.5`
  - `remapOrNull(null)` → `null`
  - `remapOrNull(-1.0)` → `0.0`

**Step 24: Unit test DefaultFeatureScoringEngine**

File: `app/src/test/java/com/dirtfy/srr/core/scoring/DefaultFeatureScoringEngineTest.kt`

- [ ] Test cases:
  - Empty `evaluationsByFeature` → all `scores[item][feature]` are `null`, all `voteCounts` are `0`
  - Feature with exactly 3 identical evaluations `[A,B,C]` → `scores[A][feature]` = 10.0, `scores[B][feature]` = 5.0, `scores[C][feature]` = 0.0
  - Feature with only 2 evaluations (below threshold=3) → all scores for that feature are `null`, but `voteCounts` still reflect the 2 votes
  - Two features with independent evaluations → scores for each feature are computed independently
  - `voteCounts[itemId][featureId]` counts only users who included that item in their evaluation for that feature (not all users)

**Step 25: Integration test Firebase repositories**

File: `app/src/androidTest/java/com/dirtfy/srr/remote/RemoteEvaluationRepositoryTest.kt`

Use the **Firebase Local Emulator Suite** to avoid hitting production data and to enable fast, reliable teardown between tests.

- [ ] Install the Firebase CLI: `npm install -g firebase-tools` then `firebase login`
- [ ] Run `firebase init emulators` in the project root and enable **Authentication** and **Firestore** emulators
- [ ] Start emulators before running tests: `firebase emulators:start --only auth,firestore`
- [ ] In test setup (`@Before`), point the SDKs at the local emulator:
  ```kotlin
  // 10.0.2.2 = host machine address from inside the Android emulator
  Firebase.auth.useEmulator("10.0.2.2", 9099)
  Firebase.firestore.useEmulator("10.0.2.2", 8080)
  ```
- [ ] Test: `submitEvaluation` writes a document at the correct Firestore path
  - Call `submitEvaluation(Evaluation("uid1", "feat1", listOf("item_b","item_c","item_a")))`
  - Call `getEvaluationsForFeature("feat1")`
  - Assert result contains exactly one `Evaluation` with `orderedItemIds = ["item_b","item_c","item_a"]`
- [ ] Test: re-submitting overwrites the previous evaluation
  - Submit `["item_a","item_b"]`, then submit `["item_b","item_a"]`
  - `getEvaluationsForFeature` returns exactly one document with `orderedItemIds = ["item_b","item_a"]`
- [ ] Test: evaluations from multiple users are all returned
  - Sign up user A and submit; sign up user B and submit
  - `getEvaluationsForFeature` returns exactly 2 evaluations with the correct `userId` fields
- [ ] In teardown (`@After`): clear the Firestore emulator data via HTTP: `DELETE http://localhost:8080/emulator/v1/projects/{project-id}/databases/(default)/documents`

**Step 26: Manual end-to-end testing checklist**

Run on a physical device or emulator after completing Phase 4, pointed at the real Firebase project (not the emulator):

- [ ] Sign up with a new email → app lands on the user screen with items and features loaded from Firestore
- [ ] Sign in with existing credentials → same result
- [ ] Enter wrong password → error message appears below the form; loading indicator disappears
- [ ] Open an item detail → scores per feature shown; pairs with < 3 evaluations show `—`
- [ ] Open a feature detail → items listed ordered by score; `—` for low-vote pairs; "Evaluate" button visible
- [ ] Submit a new evaluation for a feature → scores update after submission; `—` disappears for pairs that now meet the threshold
- [ ] Re-open the evaluation editor for the same feature → previous ordering is pre-loaded
- [ ] Re-submit with a different order → scores update accordingly in Firestore and in the UI
- [ ] Switch to Compilation tab → same data displayed, read-only; no evaluate button visible anywhere
- [ ] Open Map tab → scatter plot shows items at positions derived from their scores; tapping a point shows the correct item name and scores
- [ ] Sign out → returns to login screen; pressing back does not re-enter the user screen without signing in again
- [ ] Kill the app and reopen → if already signed in (Firebase Auth persists sessions), lands on the user screen directly
- [ ] Disconnect network mid-session → error state appears with a Retry button; tapping Retry reloads successfully when connection is restored