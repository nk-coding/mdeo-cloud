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

### 2. Multiplicity-refined ("S-type") rules not generated

| | |
|---|---|
| **Present in original?** | ✅ Yes — the second generation pass |
| **Classification** | New limitation introduced by the port |

#### Original behaviour

`RulesGenerator.generateRules()` runs the full generation pipeline **twice**:

```java
// RulesGenerator.java
if (this.refinedMultiplicities.size() > 0) {
    var solutionMetamodelWrapper =
        new MetamodelWrapper(this.metamodel, this.refinedMultiplicities);  // ruleType = "S"
    ruleSpecs.forEach(ruleSpec -> {
        var repairSpecs = specsGenerator.getRepairsForRuleSpec(ruleSpec, solutionMetamodelWrapper);
        generatedRulesListSolution.addAll(
            generateSpecRules(ruleSpec, repairSpecs, solutionMetamodelWrapper));
    });
    this.refinedMultiplicities.forEach(m -> solutionMetamodelWrapper.revertEdgeMultiplicities(m));
}
```

The first pass uses base metamodel multiplicities and names rules with a `P_` prefix (Problem
rules).  The second pass temporarily mutates the `EPackage` in-place with user-specified tightened
lower/upper bounds and names rules with an `S_` prefix (Solution rules).  Both sets are merged
after semantic deduplication.  The purpose is to generate operators that are valid specifically
inside the tighter solution space, avoiding moves that would immediately violate refinement
constraints.

The `GoalConfig.refinements` list (carrying `className`, `fieldName`, `lower`, `upper`) maps
directly to the `List<Multiplicity>` the original feeds to this second pass.

#### Why it was not implemented

The platform's `MetamodelData` does not provide mutable access to multiplicity bounds.  The
original exploits EMF's mutable `EPackage` singleton — it literally calls setters on `EReference`
objects and then reverts them.  `MetamodelData` is an immutable serialised snapshot; replicating
the mutation-and-revert trick would require building a modified copy of the `MetamodelData` struct
for the second pass.  This is implementable as a follow-up.

#### Practical effect

Refinements declared under `goal.refinements` in the optimisation config are currently parsed and
stored on `RefinementConfig` but never consumed by the rule generator.  The search space is not
restricted to the tighter solution bounds at the operator level.  Generated operators will
occasionally produce moves that violate refinement constraints; the guidance functions and
constraint scores must then steer the search away from those regions instead.

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
from different triples, which the original would merge.  This occurs primarily when the S-type
second pass (limitation 2) generates rules already covered by the P-type first pass.  Since the
second pass is not implemented, this case does not arise in practice.

---

### 6. Bidirectional reverse-end references not enumerated for the target class

| | |
|---|---|
| **Present in original?** | ⚠️ Partially — the behaviour in the original also only generates from the owning class |
| **Classification** | Shared limitation (both original and port); documented for clarity |

#### Original behaviour

`SpecsGenerator` calls `node.getEReferences()` on the `EClass` to enumerate references for
generation.  In EMF, `EClass.getEReferences()` returns only references **structurally owned** by
that class — the ones declared on it directly.

For the bidirectional association `House.rooms ←→ Room.house`, where `rooms` is declared on
`House` and `house` is declared on `Room`:
- When processing `House`, `getEReferences()` returns `rooms` (and `house` is available via
  `rooms.getEOpposite()`).
- When processing `Room`, `getEReferences()` returns `house` (and `rooms` is available via
  `house.getEOpposite()`).

Both directions **are** generated, but each from the perspective of the class that owns that
reference.

#### Port behaviour

`MetamodelInfo.referencesForNode(className)` filters `MetamodelData.associations` for entries
where `assoc.source.className == className && assoc.source.name != null`.  If `MetamodelData`
stores a bidirectional association as a single record with `source = House` and `target = Room`,
then `referencesForNode("Room")` returns nothing for the `house` link.

#### Practical effect

If the backend populates `MetamodelData` with one `AssociationData` record per logical association
(regardless of bidirectionality), the port produces **half the operators** the original would for
the reverse end.  This is a meaningful gap for bidirectional containment associations in
particular.  Whether this applies depends on how `MetamodelData.associations` is populated by the
metamodel service — if it emits both `House→Room` and `Room→House` as separate records, there is
no gap.  The KDoc in `MetamodelInfo.referencesForNode` documents this dependency explicitly.

---

### 7. Inherited references not enumerated

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
| **Refinements consumed by** | `RulesGenerator` (generation) + OCL constraints (evaluation) | `RefinementConfig` parsed and stored; not yet consumed by generator |
| **Effect on operators** | Generates S-type rules valid within tighter bounds | No effect on generated operators |
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
