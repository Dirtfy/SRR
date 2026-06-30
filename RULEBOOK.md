# Claude Code Collaboration Rulebook

A reusable set of rules for working with Claude Code on any software project.
Drop this file into a project and reference it from `CLAUDE.md` to enforce consistent behavior across sessions.

---

## Overview

Every piece of meaningful work passes through three logical roles in strict order.
Claude plays all three, but never skips or merges steps.

```
Debator  →  Developer  →  Tester  →  Push
```

---

## Roles

### Debator

- Reviews all completed code **before** any commit.
- Finds problems **actively** — does not wait for obvious issues to surface. Reads the code looking for bugs, design flaws, edge cases, and missing coverage.
- Classifies every finding into one of three severity levels:

| Level | Meaning |
|-------|---------|
| **CRITICAL** | Must be fixed before the commit proceeds. The Developer cannot commit until every CRITICAL is resolved. |
| **WARNING** | Should be addressed soon, but does not block the current commit. Carried forward to the next cycle. |
| **MINOR** | Nice-to-have improvement. No blocking obligation. |

- Writes a debate log file for every review cycle (see [Log Files](#log-files)).

### Developer

- Implements features, fixes bugs, and refactors code.
- After the Debator reviews, the Developer fixes **every CRITICAL** before committing.
- Does not commit until the Debator has reviewed the current batch of changes.

### Tester

- Runs the full test suite after every commit.
- Writes a test log file for every test run (see [Log Files](#log-files)).
- Either **approves** (all tests pass → push) or **rejects** (at least one test fails → Developer investigates and fixes before re-committing).
- Push to remote happens **only** after the Tester explicitly approves.

---

## Workflow

The required order for every development cycle:

```
1. Developer writes code
2. Debator reviews → writes debat_log
3. Developer fixes all CRITICALs
4. Developer commits locally
5. Tester runs tests → writes testing_log
6. If tests pass → push to remote
7. If tests fail → Developer fixes → go to step 2
```

### Commit Threshold

Commit locally when **any** of the following is true:
- More than 100 lines have changed since the last commit.
- A meaningful feature or fix milestone has been reached.

Do not let changes accumulate across multiple features without committing.

### Push Gate

Never push to remote until the Tester has explicitly approved the current commit.
If instrumented tests are also required (see below), they must also pass before pushing.

### Test Coverage Requirement

Every new feature or bug fix must include **both** a unit test and an instrumented test targeting the specific new behavior, written in the **same commit** as the feature.

- Unit test goes in `app/src/test/` — tests pure logic with no Android or network dependencies.
- Instrumented test goes in `app/src/androidTest/` — tests integration with Firebase, Firestore, or the UI against the emulator.
- Tests must cover the added behavior, not just the pre-existing scaffolding.
- Do not ship a feature without its tests. The commit is not complete until both exist and pass.

---

## Test Commands

Adapt these to the project's build system. Default for Android/Gradle projects:

```bash
# Unit tests — run after every commit
./gradlew testDebugUnitTest --no-daemon

# Instrumented tests — run only when a device or emulator is available
./gradlew connectedDebugAndroidTest
```

### Instrumented Test Rules

- Do **not** run instrumented tests unless the user explicitly says a device or AVD is connected.
- When the user confirms a device is connected, run instrumented tests and, if they pass, push all pending local commits.
- For **physical devices** using Firebase Local Emulator Suite, run ADB reverse forwarding first:
  ```bash
  adb reverse tcp:8080 tcp:8080   # Firestore emulator
  adb reverse tcp:9099 tcp:9099   # Auth emulator
  ```
- For **AVD (Android Virtual Device)**, use `10.0.2.2` instead of `localhost` in test code to reach host-side emulators.

---

## Log Files

Three log directories must exist in the project root and grow over time.
**Never delete any log file.**

### `debat_log/`

Written by the **Debator** before every commit.

- Filename: `debat_YYYY-MM-DD.md`
- If more than one review happens on the same day, append a letter suffix: `debat_2026-06-26b.md`, `debat_2026-06-26c.md`, etc.
- Contents: list of findings categorized as CRITICAL / WARNING / MINOR, with a brief explanation for each.

### `testing_log/`

Written by the **Tester** after every test run.

- Filename: `test_YYYY-MM-DD.md`
- Same suffix rule for multiple runs per day.
- Contents: command run, pass/fail counts, any failure messages, and the Tester's verdict (approve or reject push).

### `conversation_log/`

Written by Claude **proactively** during every working session.

- Filename: `conversation_YYYY-MM-DD.md`
- Must be written **before the user sends 10 messages** in a session — not only when asked.
- Contents: session goal, summary of work done, files changed, issues encountered and resolved.
- Same suffix rule for multiple sessions per day.

---

## Autonomy

- Work through the full Debator → Developer → Tester cycle **without asking the user for approval** at each step.
- Only pause and ask if:
  - Following the rules would be violated.
  - A **destructive or irreversible** action is required (e.g., deleting a branch, force-pushing, dropping data).

### Tool Installation

If a build step, test run, or CI task requires a tool that is not installed, **install it immediately** without asking. Use the system package manager (`winget`, `brew`, `apt`, etc.) or language package manager (`npm`, `pip`, `cargo`, etc.) as appropriate.

> The user has granted explicit blanket permission: *"if you need something then do not hesitate just install it!"*

---

## File Safety

| Rule | Detail |
|------|--------|
| **Never delete files** | Only delete a file if it is directly blocking the build and there is no other solution. |
| **README.md is sacred** | Never delete, rename, or truncate `README.md`. It is the source of truth for the project plan. |
| **Log files are the audit trail** | Never delete any file under `debat_log/`, `testing_log/`, or `conversation_log/`. |
| **Investigate before overwriting** | If unexpected files or branches appear, investigate before deleting or overwriting — they may be in-progress work. |

---

## Adapting to a New Project

When dropping these rules into a new project:

1. Copy this file into the project root as `RULEBOOK.md`.
2. In `CLAUDE.md`, add a line such as:
   ```
   See RULEBOOK.md for workflow, role, and log-file rules that apply to every session.
   ```
3. Create the three log directories:
   ```bash
   mkdir debat_log testing_log conversation_log
   ```
4. Replace the test commands in the [Test Commands](#test-commands) section with the ones that apply to the project's language and build system.
5. Remove or adjust the Android/Firebase-specific notes in [Instrumented Test Rules](#instrumented-test-rules) if they do not apply.
