# Conversation Log — 2026-06-28

**Session start:** picked up from conversation_2026-06-27d — continuing item image feature

---

## Work Completed

### 1. Firebase Storage upload UI (Screen.kt)

Replaced the "Image URL (optional)" text field in the Add Item dialog with a tap-to-upload image picker:
- Android Photo Picker (`PickVisualMedia.ImageOnly`) via `rememberLauncherForActivityResult`
- 160dp image preview area; tapping opens system picker
- Upload starts immediately on pick (background coroutine in ViewModel)
- Spinner overlay while `isUploadingImage == true`; Add + Cancel buttons disabled
- `RemoteStorageRepository` accepts `Context` to resolve MIME type via `ContentResolver.getType(uri)`

### 2. Storage permission bug (two-part fix)

**Wrong initial fix:** Removed `contentType.matches('image/.*')` from `storage.rules` — improved but didn't fix root cause.

**Root cause:** Auth emulator (port 9099) issues tokens that production Firebase Storage rejects. Original `SRRApplication.kt` only wired Auth and Firestore to emulators; Storage hit production where `request.auth` is null.

Fix:
- `SRRApplication.kt`: added `Firebase.storage.useEmulator("localhost", 9199)`
- `firebase.json`: added `"storage": {"port": 9199}` to emulators section
- ADB reverse now requires port 9199 in addition to 8080 and 9099
- Previous emulators ran `--only auth,firestore` (no Storage) — had to kill PIDs and restart

### 3. Debug variant indicator

Red banner "DEBUG BUILD  •  Firebase Emulator" at top of login screen when `BuildConfig.DEBUG == true`. In `LoginContent` so it appears in Compose previews.

### 4. Tests added

**`UserScreenUiTest.kt`** — 5 new UI tests for Add Item dialog image picker states:
- placeholder shown with no image, Add/Cancel disabled while uploading, error text displayed, Add enabled after upload completes

**`StorageUploadTest.kt`** (new) — 4 integration tests against Storage emulator (port 9199):
- success returns HTTP URL, URL path contains "items/", unauthenticated upload fails, two uploads produce distinct URLs

---

## ADB Ports Required (physical device, serial R3CX50BXZVD)

```
adb -s R3CX50BXZVD reverse tcp:8080 tcp:8080
adb -s R3CX50BXZVD reverse tcp:9099 tcp:9099
adb -s R3CX50BXZVD reverse tcp:9199 tcp:9199
```

---

## Commits

| Hash | Message |
|------|---------|
| `a8d9596` | feat: replace URL input with Firebase Storage image upload |
| `a668493` | log: instrumented test results 2026-06-28c |
| `81841bd` | fix: resolve Storage upload permission denied error |
| `b4e320d` | log: instrumented test results 2026-06-28d |
| `c1fc8bb` | fix: wire Storage to emulator + add debug banner on login |
| pending  | test: add image upload UI tests + Storage integration tests |
