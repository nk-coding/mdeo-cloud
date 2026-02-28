# Mutation Rule Generation — Port Notes

This document compares the Kotlin port (`platform/optimizer/src/main/kotlin/com/mdeo/optimizer/rulegen/`)
with the original Java implementation (`copilot/mde_optimiser/libraries/rulegen/`) and records every
material limitation and architectural change.

---

## Limitations

### 1. Cartesian-product rule combinations not generated

| | |
|---|---|
| **Present in original?** | ✅ Yes — deliberately designed feature |
| **Classification** | New limitation introduced by the port |

#### Original behaviour

`SpecsGenerator.generateNodeRepairCombinations()` calls Guava's `Sets.cartesianProduct` across all
references of a node:

```java
// SpecsGenerator.java
return Sets.cartesianProduct(repairs);   // repairs = one Set<RepairSpec> per reference
```

For a node with three references where each reference has two possible repair types, this produces
$2^3 = 8$ combined rules.  Each combined rule applies one operation on _every_ reference of the
node simultaneously in a single atomic transformation step — for example, "create a link on
`refA`, remove a link on `refB`, and change a link on `refC`" all as one Henshin pattern.

#### Why it was not implemented

The custom DSL executes one match-block per script and produces one atomic graph edit.  Expressing
"match N independent sub-patterns and apply N edits" is syntactically possible, but the number of
unique combined scripts grows exponentially with references and cannot be known without the
metamodel at code-generation time.  Additionally, the MOEA framework's mutation step already
supports applying multiple operators sequentially via the step-size strategy
(`MutationStepConfig.Fixed(n)` or `Interval`).  Multi-step sequential application achieves the
same reachable search space, though the path taken differs.

#### Practical effect

For a node with many references the original generates a dramatically larger operator library than
the port does.  In practice this changes the fine-grained neighbourhood structure of the search
space: the original can move between certain non-adjacent states in a single operator application
while the port requires multiple steps.  This does **not** reduce reachability but does affect
convergence speed and plateau escaping behaviour.

---

### 3. Lower-bound repair (`LB_REPAIR`) is a simplified stub

| | |
|---|---|
| **Present in original?** | ✅ Yes — explicit multi-node repair patterns |
| **Classification** | New limitation introduced by the port |

#### Original behaviour

`CREATE_LB_REPAIR` and `DELETE_LB_REPAIR` are genuine structural repair operations.  For CREATE,
the generated Henshin rule includes a _third_ node alongside source and target:

1. Match an additional existing node `A*` of the same type as the node being created.
2. Add a `DELETE` edge from `A*` to the target `B` (the node satisfying the lower bound).
3. The new node `A_new` already gets a `CREATE` edge to `B` (from `createMandatoryNeighbours`).

The net effect: `A_new` is created and given `B`; `A*` simultaneously loses `B` ("steals it").
The graph remains consistent with the lower-bound constraint. For `CREATE_LB_REPAIR_MANY` a more
complex pattern handles the case where each existing node holds multiple lower-bound targets.

#### Why it was not implemented

The "steal from existing" pattern requires a single match block to simultaneously address _three_
distinct nodes with a mixed create/delete/preserve pattern.  This is expressible in the custom DSL
(`match { …; delete A*.ref -- B; create A_new.ref -- B }`), but the pattern must also include the
correct cardinality guards specific to the reference's current multiplicity at the metamodel
analysis time.  Generating these guards safely would require the full `SpecsGenerator` logic to
run again in a sub-context, which was deferred.

#### Practical effect

Rules for references with lower bounds $> 0$ are generated as plain CREATE/DELETE rules without
the steal step.  Applying such an operator may produce a graph that temporarily violates the
lower-bound multiplicity constraint.  Whether this matters depends on whether the problem's
guidance functions penalise multiplicity violations (as the original `mde_optimiser` does via
`OCL` constraints on the evaluation model).

---

### 4. Multiplicity guard conditions (LB/UB checks) not added to rules

| | |
|---|---|
| **Present in original?** | ✅ Yes — every rule type except SWAP |
| **Classification** | New limitation introduced by the port |

#### Original behaviour

After building the core LHS/RHS pattern, every command class (Create, Delete, Add, Remove, Change)
calls `applyRuleNacConditions()`, which invokes:

- `LowerBoundManyRepairCheckGenerator.generate()` — adds a PAC (Positive Application Condition)
  to `rule.getLhs()` that verifies sufficient outgoing edges exist before the rule fires when edges
  would be lost.
- `UpperBoundManyRepairCheckGenerator.generate()` — adds a NAC (Negative Application Condition)
  that verifies the upper bound would not be exceeded when edges are added.

These are `NestedCondition` objects (wrapped in `Not` for NAC) chained with `AND` onto the rule's
LHS `Formula`.  Only `SwapEdgeRuleCommand` skips this step entirely.

#### Why it was not implemented

The custom DSL has a `forbid` modifier for NACs but has no PAC modifier (positive application
condition) and no way to express "at least N of these edges must exist" as a quantified pattern
condition.  A `forbid` can express "this edge must not exist" (upper bound = 0 case) but not
"fewer than N of these edges exist" as a general guard.  The LB/UB generators in the original
compute the exact offset value $k = (b - \text{preserved} + \text{balance})$ and then build a
nested pattern asserting the existence of exactly $k$ additional edges via `NestedCondition` chains
— a construct the DSL does not support.

#### Practical effect

Generated operators can fire in states that would violate multiplicity bounds.  The ADD rule has a
`forbid` NAC that prevents adding a duplicate edge (the simplest upper-bound case), but it does
not prevent exceeding an upper bound $> 1$.  Operators that delete edges may fire even when the
lower bound would be breached.  As with limitation 3, this is handled indirectly through the
objective/constraint scoring rather than at the operator level.

---

### 5. Semantic deduplication replaced with name-based deduplication

| | |
|---|---|
| **Present in original?** | ✅ Yes — graph-isomorphism comparison via `RuleSemanticsChecker` |
| **Classification** | New limitation introduced by the port |

#### Original behaviour

`RulesGenerator.removeDupplicateRules()` uses `RuleSemanticsChecker.isEqual()` (from the
SIDiff/SERGe library) which performs a graph-isomorphism check on the Henshin rule graphs.  This
correctly identifies two rules as duplicates even if they were generated from different specs and
use different node-variable names, as long as the applied graph transformation is structurally
identical.

#### Why it was not implemented

`RuleSemanticsChecker` is a SIDiff-specific utility that operates on EMF-based Henshin rule
objects.  There is no equivalent graph-isomorphism checker in the platform for `TypedAst` objects.
Building one would require a significant new module.

#### Practical effect

Name-based deduplication (the current approach) eliminates all cases where the same
`(node, edge, RepairSpecType)` triple is produced twice — which covers all structural duplicates
within a single generation pass.  It does _not_ detect two semantically identical rules that arise
from different triples, which the original would merge.  In practice, any S-type rule whose name
already exists in the result map (because tightened bounds produce an identical rule shape to a
base rule) is silently skipped — the `seen` map in `MutationRuleGenerator` handles this
naturally since S-type rules carry an `S_` prefix and therefore never share a name with base rules.

---

### 6. Inherited references not enumerated

| | |
|---|---|
| **Present in original?** | ✅ Yes — original also uses own-references only for enumeration |
| **Classification** | Shared limitation (same behaviour as original) |

#### Original behaviour

`SpecsGenerator` uses `getEReferences()` (own only), not `getEAllReferences()` (including
inherited).  `MetamodelWrapper.getEdge()` uses `getEAllStructuralFeatures()` for _lookup_ by name,
which does include inherited features — but this is only called when a user explicitly names an
edge in a `RuleSpec`, not during general enumeration.

#### Port behaviour

`MetamodelInfo.referencesForNode()` filters `MetamodelData.associations` by `source.className`,
which likewise returns only directly declared associations.  If the metamodel service flattens
inherited associations into the serialised `associations` list, both original and port behave
identically.

#### Practical effect

Rules are not generated for references inherited from abstract superclasses unless those are
explicitly listed in the metamodel service's association output.  This is consistent with the
original's behaviour.

---

## Major Architectural Changes

### A. Rule representation: Henshin `Module` → `TypedAst`

| | Original | Port |
|---|---|---|
| **Output type** | `org.eclipse.emf.henshin.model.Module` | `com.mdeo.modeltransformation.ast.TypedAst` |
| **Execution engine** | Henshin interpreter (EMF graph rewriting) | `TransformationEngine` (Gremlin/TinkerGraph) |
| **Language** | Henshin DSL (XML-serialised EMF model) | Custom model-transformation DSL (compiled to `TypedAst`) |

The original generates Henshin modules, serialises them to `.henshin` XML files, and executes them
via the Henshin interpreter against an EMF in-memory model.  The port directly constructs
`TypedAst` objects (the compiled intermediate representation of the platform's DSL) and feeds them
to `TransformationEngine`, which applies them to a TinkerGraph via Gremlin traversals.

The rule-building API changes from Henshin's factory
(`HenshinFactory.createRule()`, `createNode()`, `createEdge()`, `createNestedCondition()`) to
assembling `TypedPatternObjectInstanceElement`, `TypedPatternLinkElement`, and
`TypedMatchStatement` data structures in `MutationAstBuilder`.

Rules are never written to disk; they live only in memory as `TypedAst` objects keyed by name in
the `transformations: Map<String, TypedAst>` map that `OptimizationOrchestrator` receives.

---

### B. Metamodel representation: `EPackage` → `MetamodelData`

| | Original | Port |
|---|---|---|
| **Metamodel type** | `org.eclipse.emf.ecore.EPackage` (live EMF model) | `MetamodelData` (serialised snapshot) |
| **Mutability** | Mutable singleton — bounds can be changed in-place and reverted | Immutable data class — no in-place mutation |
| **Inheritance** | `getEAllStructuralFeatures()` traverses full hierarchy | Flattened `associations` list from metamodel service |
| **Opposite access** | `EReference.getEOpposite()` gives live reference | `AssociationEndData` with optional `targetEndName` |

The original exploits EMF's mutable object graph for the refinement second-pass trick (modify
bounds, generate rules, revert bounds).  `MetamodelData` is an immutable serialised transfer
object, making this approach impossible without creating a mutated copy of the data structure.

---

### C. Rule generation trigger: standalone Java library → embedded optimizer phase

| | Original | Port |
|---|---|---|
| **Invocation** | `RulesGenerator` is a standalone library; callers (`.mopt` problem specs) instantiate it explicitly | `MutationRuleGenerator.generate()` is called inside `OptimizerExecutionService` as Phase 3b |
| **When rules are created** | Before the optimisation run, as an offline preprocessing step | At the start of each optimisation run, in-memory |
| **Config source** | `.mopt` file specifying `generate` blocks with node/edge/action | `MutationsConfig.generate: List<MutationRuleSpec>` in the JSON optimisation config |
| **Output destination** | `.henshin` files on disk (or `Module` objects passed to engine) | Merged into `Map<String, TypedAst>` passed directly to `OptimizationOrchestrator` |

In the original, rule generation is a distinct offline step: the problem spec declares what to
generate, the generator writes `.henshin` files, and those files are then loaded by the execution
engine.  In the port, generation is an inline phase — rules are generated in memory, given
synthetic names, and treated identically to hand-authored transformation scripts.

---

### D. Deduplication model

| | Original | Port |
|---|---|---|
| **Algorithm** | `RuleSemanticsChecker.isEqual()` — graph isomorphism on Henshin rule graphs | Name-based: `LinkedHashMap<String, GeneratedMutation>` keyed by rule name |
| **What it catches** | Any two structurally/semantically equivalent rules regardless of how they were generated | Only exact `(node, edge, RepairSpecType)` triples that produce the same name |
| **Cross-pass dedup** | Merges P-type and S-type rules, removing S rules that are identical to P rules | Not applicable (S-type pass not implemented) |

---

### E. Multiplicity refinement integration

| | Original | Port |
|---|---|---|
| **Refinements consumed by** | `RulesGenerator` (generation) + OCL constraints (evaluation) | `MutationRuleGenerator.generate()` (via `refinements` parameter) + constraint guidance functions |
| **Effect on operators** | Generates S-type rules valid within tighter bounds | Generates `S_`-prefixed rules via `MetamodelInfo.withOverrides()` using tightened bounds |
| **Effect on evaluation** | OCL constraint evaluator rejects solutions outside bounds | Constraint guidance functions score bound violations |

---

### F. Injective matching vs. explicit NAC guards

| | Original | Port |
|---|---|---|
| **CHANGE degenerate case** (`oldTarget == newTarget`) | Prevented by Henshin's **injective matching** — rule variables must map to distinct nodes | Prevented by a `forbid source.ref -- newTarget` NAC in the match pattern (same observable effect) |
| **SWAP degenerate case** (`A == A*` or `B == B*`) | Prevented by injective matching (no explicit guard in SwapEdgeRuleCommand) | Port generates a SWAP rule also without an explicit guard; relies on the TinkerGraph runtime's implicit distinctness of matched step-labels |

The original relies on Henshin's built-in graph matching semantics.  The port uses an explicit
`forbid` NAC for CHANGE (see `MutationAstBuilder.buildChangeEdge`) and implicitly relies on
variable name distinctness for SWAP.  The observable behaviour is identical.
