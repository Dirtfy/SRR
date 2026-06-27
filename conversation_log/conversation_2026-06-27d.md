# Conversation Log — 2026-06-27d

**Session range:** continuation from summarized previous context  
**Commit pushed:** `2c254ed` (main)

---

## What Was Accomplished

### Root cause found and fixed — 60/60 tests now pass

**Problem 1: `deleteUrl()` never sent the HTTP DELETE**  
`HttpURLConnection` in Android is lazy — it does not transmit the request until a response is read. All three test files called `connect()` then `disconnect()` without reading `responseCode`. The emulator never received the DELETE. Documents from test N persisted into test N+1 causing 13 Firebase test failures.  
Fix: Changed `conn.connect()` → `conn.responseCode` in `MultiUserFlowTest`, `RemoteEvaluationRepositoryTest`, `RemoteItemFeatureRepositoryTest`.

**Problem 2: Firestore cache preventing Source.SERVER from seeing just-written docs**  
With persistence enabled, `add().await()` completes on local cache ack. `get(Source.SERVER)` then went to the server, which hadn't received the write yet.  
Fix: `SRRApplication.onCreate()` sets `setPersistenceEnabled(false)` BEFORE `useEmulator()` so all writes complete only after server ack.

**Problem 3: `@BeforeClass setFirestoreSettings Builder()` overwrote emulator URL**  
`FirebaseFirestoreSettings.Builder()` starts from DEFAULT production settings. Calling it in `@BeforeClass` after `useEmulator()` reset the host to production Firebase. Removed from all test classes; `SRRApplication.onCreate()` is the single source of truth for emulator settings.

**Problem 4: UI tests "No compose hierarchies found" (15/16 failures)**  
Stale compiled classes from incremental build after large UI changes. Fixed by `./gradlew clean` — not a code defect.

### Full test results after all fixes
```
OK (60 tests)
```
Unit tests: BUILD SUCCESSFUL  
Commit: `2c254ed` pushed to `main`  
APK reinstalled; ADB ports restored.

---

## Remaining Work

### 1. Deploy Firestore rules to production
```bash
firebase login      # must be run by user first
firebase deploy --only firestore:rules
```
Rules file is correct and committed. Just needs a logged-in firebase-tools session.

### 2. New feature — Item images (NOT STARTED)
User requested: items can have an image. Images should appear in:
- **Item detail page** (My tab → tap an item)
- **Map popup** (Result tab → Map → tap a point on the scatter plot)

Design decision needed before starting:
- Storage: Firebase Storage (recommended for binary blobs) vs. Firestore URL field (simpler, but user must supply a URL)
- Upload UX: camera picker / gallery picker / URL input?
- The `Item` model currently has only `id`, `name`, `createdBy`; needs an `imageUrl: String?` field

Relevant files:
- `core/model/Item.kt` — add `imageUrl: String?`
- `core/repository/ItemRepository.kt` — `createItem()` may need image param or separate `updateItemImage()`
- `remote/repository/RemoteItemRepository.kt` — persist imageUrl to Firestore; retrieve it in `getAllItems()`
- `ui/performer/user/Screen.kt` → `UserItemDetailContent` — show image
- `ui/performer/compilation/map/` — popup composable, add image display
- `firestore.rules` — allow `imageUrl` field in items create/update

---

## State of Working Tree at Session End
```
git status: clean (all committed)
git log HEAD: 2c254ed fix: test isolation + uniqueness enforcement + Source.SERVER reads
```
