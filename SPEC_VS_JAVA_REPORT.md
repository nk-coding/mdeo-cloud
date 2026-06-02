# aCPSO Spec ↔ Java Reference Comparison Report

**Scope:** This report documents every difference between the formal atomic
consistency-preserving search operator (aCPSO) tables in
[`spec.md`](spec.md) and the operator-**selection** logic of the original Java
reference implementation in
[`context/libraries/rulegen`](context/libraries/rulegen).

It addresses item **3** of the "Remaining High-Priority Gaps" in
[`CPO_SPEC_COMPARISON_REPORT.md`](CPO_SPEC_COMPARISON_REPORT.md):

> *Some formal-table rows still disagree with the Java reference … `spec.md` is
> stricter than the Java reference in several areas, especially around omitted
> create-node cases and some delete-node cases.*

The analysis below pins down exactly which rows disagree, in which direction,
and why.

---

## 1. How to read the tables

Both sources describe the multiplicity pattern between a node **A** and a
neighbour node **B** connected by a reference `A —ref→ B`:

| Symbol | Meaning | Java accessor |
|--------|---------|---------------|
| `n` | lower bound of `A —ref→ B` (the edge itself) | `edge.getLowerBound()` |
| `m` | upper bound of `A —ref→ B` (`*` = unbounded) | `edge.getUpperBound()` (`-1` = `*`) |
| `k` | lower bound of the opposite end `B —opp→ A` | `edge.getEOpposite().getLowerBound()` |
| `l` | upper bound of the opposite end `B —opp→ A` (`*` = unbounded) | `edge.getEOpposite().getUpperBound()` (`-1` = `*`) |

The Java selection lives in
[`SpecsGenerator.java`](context/libraries/rulegen/src/main/java/uk/ac/kcl/inf/mdeoptimiser/libraries/rulegen/generator/specs/SpecsGenerator.java):

- `generateNodeCreateRules` (create node)
- `generateNodeDeleteRules` (delete node)
- `generateEdgeAddRules` / `generateEdgeRemoveRules` (edge add / remove)

Spec operator shorthand used below:

- `c A` / `Create A` → emit a plain `CREATE`.
- `add n B` / `f#l A` → mandatory-child creation / a forbid (NAC) that the
  neighbour already holds `l` links. In Java these are *not* separate repair
  specs — they are decided inside the rule-generation command, so at the
  **selection** layer they collapse into a single `CREATE`.
- `lb r single B` → `CREATE_LB_REPAIR`.
- `lb r many B` → `CREATE_LB_REPAIR_MANY`.
- `Delete A r lb sg B` → `DELETE_LB_REPAIR`; `d A r lb mm B` → `DELETE_LB_REPAIR_MANY`.
- Edge: `Add edge`/`Remove edge` → `ADD`/`REMOVE`; `Change edge` → `CHANGE`;
  `Swap edge` → `SWAP`. The `NAC …` / `PAC …` annotations are guard variants
  that Java derives automatically (see §6).

A row is marked ✅ when the spec and Java agree on the **set of repair-spec
types** produced, and ⚠️ when they disagree.

---

## 2. Create Node operators

### Java logic (`generateNodeCreateRules`)

```
if EOpposite == null                          -> { CREATE }
else if n == 0                                -> { CREATE }
else if n != m:                                       // not fixed
    if k >= 0 && k != l                       -> add CREATE
    if n >= 1 && l != *                        -> add CREATE_LB_REPAIR
    if n  > 1 && l != *                        -> add CREATE_LB_REPAIR_MANY
else if n == m:                                       // fixed source
    if k != l                                 -> { CREATE }
```

### Spec group 1 — `k >= 0, l > k, l < *` (opposite **bounded, not fixed**)

| Case | spec.md | Java | |
|------|---------|------|---|
| `n = 0` | `c A` → CREATE | CREATE | ✅ |
| `n = 1, m > n` | CREATE + LB single | CREATE (`k!=l`) + LB single (`l≠*`) | ✅ |
| `n > 1, m > n` | CREATE + LB single + LB many | CREATE + LB single + LB many (`n>1`) | ✅ |
| `n = m` | CREATE | CREATE (`k!=l`) | ✅ |

### Spec group 2 — `k >= 0, l = *` (opposite **unbounded**)

| Case | spec.md | Java | |
|------|---------|------|---|
| `n = 1, m > n` | CREATE | CREATE (`k!=l`; no LB because `l=*`) | ✅ |
| `n = 0` | *(none specified)* | **CREATE** (`n==0` branch) | ⚠️ **D1** |
| `n > 1, m > n` | *(none specified)* | **CREATE** (`k!=l`; no LB/many because `l=*`) | ⚠️ **D2** |
| `n = m` | *(none specified)* | **CREATE** (`k!=l`) | ⚠️ **D3** |

### Spec group 3 — `k = l` (opposite **fixed**)

| Case | spec.md | Java | |
|------|---------|------|---|
| `n = 1, m > n` | LB single | LB single only (no CREATE because `k==l`) | ✅ |
| `n > 1, m > n` | **CREATE** + LB single + LB many | LB single + LB many **only** (no CREATE because `k==l`) | ⚠️ **D4** |
| `n = m` | N/A | ∅ (no CREATE because `k==l`; no LB because `n==m`) | ✅ |
| `n = 0` | *(none specified)* | **CREATE** (`n==0` branch fires first) | ⚠️ **D5** |

### Create-node differences

- **D1 / D2 / D3 — Java emits `CREATE` where the spec is silent (opposite
  unbounded, `l=*`).** Whenever `n==0`, or the source is non-fixed/fixed with a
  *different* opposite (`k≠l`, which always holds when `l=*` and `k≥0`), Java
  produces a plain `CREATE`. The spec only lists the `n=1, m>n` row for `l=*`
  and explicitly notes *"No operators specified for n=0, n>1 and m>n, or n=m"*.
  **Direction:** Java is broader (more permissive) than the spec.
- **D4 — Spec lists a plain `CREATE` for fixed opposite `k=l` with `n>1`, Java
  does not.** Java only adds `CREATE` in the `n!=m` branch when `k != l`; with
  `k==l` it emits *only* the LB-repair variants. The spec row
  `Create A add n B | … lb r single B | … lb r many B` therefore over-lists one
  operator relative to Java. **Direction:** the spec is broader here (the lone
  case where the spec exceeds Java for create).
- **D5 — Java emits `CREATE` for `n=0` under `k=l`, the spec does not.** The
  Java `n == 0` short-circuit fires before the `k==l` discrimination, so a
  fixed opposite with an optional source still yields a plain `CREATE`.
  **Direction:** Java broader.

---

## 3. Delete Node operators

### Java logic (`generateNodeDeleteRules`)

```
if EOpposite == null || k == 0                -> { DELETE }
else:
    if k > 0 && (l > k || l == *)             -> add DELETE
    if k == 1 && l == 1 && n != m             -> add DELETE_LB_REPAIR
    if k == l && k > 1 && n != m:
        add DELETE_LB_REPAIR
        if n == 0 || n > 1                     -> add DELETE_LB_REPAIR_MANY
```

### Spec group — `k = 0`

| Case | spec.md | Java | |
|------|---------|------|---|
| `m > n, m < *` | DELETE | DELETE | ✅ |
| `m = *` | *(none specified)* | **DELETE** | ⚠️ **D6** |
| `n = m` | *(none specified)* | **DELETE** | ⚠️ **D6** |

### Spec group — `k > 0, l > k`

| Case | spec.md | Java | |
|------|---------|------|---|
| `m > n, m < *` | DELETE (require each B still has `#k` A) | DELETE (PAC computed) | ✅ |
| `m = *` | *(none specified)* | **DELETE** (`l>k` ⇒ branch fires) | ⚠️ **D7** |
| `n = m` | *(none specified)* | **DELETE** | ⚠️ **D7** |

### Spec group — `k = l = 1`

| Case | spec.md | Java | |
|------|---------|------|---|
| `m > n, m < *` | LB single (`f#m A`) | DELETE_LB_REPAIR (`k==1,l==1,n≠m`) | ✅ |
| `m = *` | LB single | DELETE_LB_REPAIR (`n≠m` since `m=*`) | ✅ |
| `n = m` | N/A | ∅ (`n==m` blocks repair; `l>k` false) | ✅ |

### Spec group — `k = l > 1`

| Case | spec.md | Java | |
|------|---------|------|---|
| `m > n, m < *`, with `n = 0` or `n > 1` | LB single + LB many | LB single + LB many | ✅ |
| `m > n, m < *`, with **`n = 1`** | LB single + LB many | **LB single only** (`n==0||n>1` is false) | ⚠️ **D8** |
| `m = *`, with `n = 0` or `n > 1` | LB single + LB many | LB single + LB many | ✅ |
| `m = *`, with **`n = 1`** | LB single + LB many | **LB single only** | ⚠️ **D8** |
| `n = m` | N/A | ∅ | ✅ |

### Delete-node differences

- **D6 — `k = 0`: Java emits `DELETE` for `m = *` and `n = m`, the spec is
  silent.** The Java guard depends only on the opposite end (`k == 0`), never on
  `m`, so deletion is always allowed when the neighbour does not require the
  node. **Direction:** Java broader.
- **D7 — `k > 0, l > k`: Java emits `DELETE` for `m = *` and `n = m`, the spec
  is silent.** Same reasoning — the branch condition is `k > 0 && (l > k || l ==
  *)`, independent of `m`/`n`. **Direction:** Java broader.
- **D8 — `k = l > 1` with `n = 1`: the spec lists both `LB single` and
  `LB many`, Java emits only `LB single`.** Java gates `DELETE_LB_REPAIR_MANY`
  on `n == 0 || n > 1`; the value `n == 1` falls through that guard. So an edge
  such as `1..3` against a fixed opposite `2..2` gets only the single-neighbour
  delete repair in Java. **Direction:** the spec is broader (lists a "many"
  variant Java suppresses for `n=1`).

---

## 4. Edge Add operators

### Java logic (`generateEdgeAddRules`)

```
if EOpposite == null || k == 0:
    n == m ? SWAP : ADD
else:                                  // k > 0
    if n == m            -> SWAP
    else if k == l       -> CHANGE
    else                 -> ADD
```

### Spec vs Java

| Spec group | Row | spec.md | Java | |
|------------|-----|---------|------|---|
| `l < *` (non-fixed) | `m < *` | ADD (NAC A B) | ADD | ✅ |
| `l < *` | `m = *` | ADD (NAC B) | ADD | ✅ |
| `l < *` | `n = m` | SWAP | SWAP | ✅ |
| `l = *` (non-fixed) | `m < *` | ADD (NAC A) | ADD | ✅ |
| `l = *` | `m = *` | ADD | ADD | ✅ |
| `l = *` | `n = m` | SWAP | SWAP | ✅ |
| `k = l` (fixed) | `m < *` | CHANGE | CHANGE (if `k>0`) | ✅ / ⚠️ **D9** |
| `k = l` | `m = *` | CHANGE | CHANGE (if `k>0`) | ✅ / ⚠️ **D9** |
| `k = l` | `n = m` | SWAP | SWAP | ✅ |

At the **selection** level the choice of `ADD` / `CHANGE` / `SWAP` agrees with
the spec, modulo two points:

- **D9 — degenerate `k = l = 0`.** The spec's `k = l` group would prescribe
  `CHANGE`, but Java routes `k == 0` into the first branch and emits `ADD`
  (or `SWAP` if `n==m`). `CHANGE` in Java therefore requires `k > 0`. This only
  bites for the degenerate fixed-empty opposite `0..0`.
- **NAC granularity (see §6).** The spec distinguishes `Add edge NAC A B`,
  `NAC B`, `NAC A`, and bare `Add edge` per `(m, l)`. Java produces a single
  `ADD` repair spec and derives the needed NACs automatically, so these
  sub-variants are an AST-level concern, not a selection difference.

---

## 5. Edge Remove operators

### Java logic (`generateEdgeRemoveRules`)

```
if EOpposite == null || k == 0:
    n == m ? SWAP : REMOVE
else:                                  // k > 0
    if n == m            -> SWAP
    else if k == l       -> CHANGE
    else                 -> REMOVE
```

### Spec vs Java

| Spec group | Row | spec.md | Java | |
|------------|-----|---------|------|---|
| `k = 0` | `n = 0` | REMOVE | REMOVE | ✅ |
| `k = 0` | `n > 0` | REMOVE (PAC A) | REMOVE | ✅ |
| `k = 0` | `n = m` | SWAP | SWAP | ✅ |
| `k > 0` (non-fixed) | `n = 0` | REMOVE (PAC B) | REMOVE | ✅ |
| `k > 0` | `n > 0` | REMOVE (PAC AB) | REMOVE | ✅ |
| `k > 0` | `n = m` | SWAP | SWAP | ✅ |
| `k = l` (fixed) | `n = 0` | CHANGE | CHANGE (if `k>0`) | ✅ / ⚠️ **D9** |
| `k = l` | `n > 0` | CHANGE | CHANGE (if `k>0`) | ✅ / ⚠️ **D9** |
| `k = l` | `n = m` | SWAP | SWAP | ✅ |

Edge removal mirrors edge addition: the `REMOVE` / `CHANGE` / `SWAP` selection
agrees with the spec, with the same degenerate `k=l=0` caveat (**D9**) and the
same PAC-granularity caveat (§6).

---

## 6. NAC / PAC granularity (cross-cutting)

The spec encodes *which* negative/positive application condition a rule carries
directly in the operator name (`NAC A B`, `NAC B`, `NAC A`, `PAC A`, `PAC B`,
`PAC AB`, `f#l A`, `f#m A`, "require each B still has `#k` A"). The Java
reference does **not** branch on these at selection time. Instead every edge
command applies bound checks generically:

- `AddEdgeRuleCommand` / `RemoveEdgeRuleCommand` / `ChangeEdgeRuleCommand` each
  call `new LowerBoundCheckGenerator(rule).generate()` and
  `new UpperBoundCheckGenerator(rule).generate()`.
- `CreateNodeRuleCommand` / `DeleteNodeRuleCommand` call
  `LowerBoundManyRepairCheckGenerator` and `UpperBoundManyRepairCheckGenerator`.

Consequently the spec's fine-grained NAC/PAC operator variants are **not**
distinct repair-spec types in Java; they are a downstream rendering of the same
ADD/REMOVE/CHANGE/SWAP/CREATE/DELETE selection plus automatically-derived guards.
This is a presentation difference rather than a behavioural disagreement.

---

## 7. Summary of differences

| ID | Area | Condition | spec.md | Java | Direction |
|----|------|-----------|---------|------|-----------|
| D1 | Create | `k≥0, l=*`, `n=0` | none | CREATE | Java broader |
| D2 | Create | `k≥0, l=*`, `n>1, m>n` | none | CREATE | Java broader |
| D3 | Create | `k≥0, l=*`, `n=m` | none | CREATE | Java broader |
| D4 | Create | `k=l`, `n>1, m>n` | CREATE + LB single + LB many | LB single + LB many | spec broader |
| D5 | Create | `k=l`, `n=0` | none | CREATE | Java broader |
| D6 | Delete | `k=0`, `m=*` or `n=m` | none | DELETE | Java broader |
| D7 | Delete | `k>0, l>k`, `m=*` or `n=m` | none | DELETE | Java broader |
| D8 | Delete | `k=l>1`, `n=1` | LB single + LB many | LB single only | spec broader |
| D9 | Add/Remove | `k=l=0` (degenerate) | CHANGE | ADD/REMOVE | divergent edge case |
| — | Add/Remove | NAC/PAC variants | per-row NAC/PAC | generic guards | presentation only |

### Interpretation

- **The spec is generally *stricter* than Java for node operators** (D1–D3,
  D5–D7): Java emits a plain `CREATE`/`DELETE` in several boundary cases
  (`n=0`, `n=m`, or an unbounded `m`/`l`) that the spec leaves unlisted. Whether
  these are genuine Java over-generation or deliberate "always-allowed base
  operator" behaviour is a design question — they are *safe* operators
  (creating/deleting in a context the multiplicity already permits), so Java's
  choice is defensible.
- **Two rows have the spec *broader* than Java** (D4, D8): the spec lists an
  extra operator (`CREATE` under fixed opposite `n>1`; `LB many` delete for
  `n=1`) that Java suppresses. D4 looks like a spec table inconsistency (a plain
  `CREATE` under `k=l` contradicts the `k=l, n=1` row which omits it). D8 is a
  deliberate Java guard (`n==0 || n>1`) — a single-element source (`n=1`) cannot
  meaningfully drive a *many* repair, so Java only emits the single variant.
- **Edge operators agree** at the selection level except for the degenerate
  `k=l=0` case (D9) and the NAC/PAC presentation difference.

### Recommendation

Before "fixing" either side, decide the intended **precedence/support matrix**:

1. If the base `CREATE`/`DELETE` operators are intended to always be available
   whenever multiplicities permit, the spec should add the missing rows
   (resolving D1–D3, D5–D7 in Java's favour).
2. D4 should be reconciled by removing the stray `CREATE` from the spec's
   `k=l, n>1` row (align spec → Java).
3. D8 is a judgement call: keep Java's `n=1 ⇒ single only` guard, or relax it to
   also emit `LB many` to match the spec. Java's guard is the more sensible
   default.
4. D9 (`k=l=0`) is degenerate; no action needed unless `0..0` opposites are a
   real modelling scenario.

This report concerns the **Java reference vs the spec only**. The Kotlin port's
alignment with the Java reference (gaps 1 and 2 of
`CPO_SPEC_COMPARISON_REPORT.md`) is handled separately and is now implemented.
