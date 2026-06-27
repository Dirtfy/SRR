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

**Item and feature management:** Any signed-in user may add new items and features to the catalogue. Items and features are visible to all users once created and cannot be renamed or deleted through the app.

**User input:** For each feature, the user orders all items from strongest feature presence (index 0) to weakest (last). Evaluating every item is encouraged but not required — unevaluated items are excluded from that feature's score entirely.

Example — features: `durability`, `portability` / items: `A`, `B`, `C`
```
durability  → [B, C, A]   (B is most durable, A is least)
portability → [A, C, B]   (A is most portable, B is least)
```

**Compilation view:** An aggregated score matrix showing each item's strength score per feature, and a map/scatter view for cross-feature comparison.

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
| 5 | Score storage | Scores are computed **on-demand** — never written to Firestore | Avoids stale-score bugs; storage cost is low since re-computation is cheap |
| 6 | Item/feature write access | Any authenticated user may **create** items and features; update and delete are blocked | Enables crowd-sourced catalogue growth without moderation overhead |
