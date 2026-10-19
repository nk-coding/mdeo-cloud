# Model Transformation plugin

Rewrites models. A transformation matches a fragment of a model and changes it; during an
optimisation, transformations are the mutation operators — the only moves the search may make.

## At a glance

| | |
| --- | --- |
| **Plugin id** | `model-transformation-service` |
| **Display name** | Model Transformation |
| **Description** | Language support for model transformation definitions (`.mt` files) |
| **Default URL** | `/plugin/model-transformation` |
| **Source** | `app/packages/service-model-transformation`, `app/packages/language-model-transformation`, `app/packages/editor-model-transformation` |
| **Depends on** | The [Metamodel plugin](/plugins/metamodel); executions go to `model-transformation-execution` |

## Languages contributed

| Language id | Name | Extension | Textual editor | Graphical editor | Generated |
| --- | --- | --- | --- | --- | --- |
| `model-transformation` | Model Transformation | `.mt` | ✅ | ✅ | ❌ |
| `model-transformation_gen` | Generated Model Transformation | `.mt_gen` | ❌ | ✅ | ✅ |

### The model transformation language

A `.mt` file names its metamodel and then contains a sequence of statements.

<<< @/../samples/language-tour/match.mt{mt}

#### Patterns

A `match { ... }` block is a pattern. Every element of the pattern has to be found in the model before
any of the marked changes are applied. Elements can be:

| Element | Syntax | Meaning |
| --- | --- | --- |
| Object | `name: Class { ... }` | An object of that class must exist |
| Link | `source[.property] -- target[.property]` | A link must exist between two matched objects |
| Reference | `name { ... }` | Constrain or update an object matched in an *enclosing* scope |
| Delete | `delete name` | Remove an object bound by an earlier or enclosing statement |
| Variable | `var name[: type] = expression` | Bind a value for later use in the pattern |
| Condition | `where expression` | An arbitrary boolean condition on the match, or — inside a block — on that condition |
| Application condition | `forbid [name] { ... }` / `require [name] { ... }` | A sub-pattern that must not / must be findable |

Objects and links can carry a modifier:

| Modifier | Effect |
| --- | --- |
| *(none)* | The element must exist and is left untouched |
| `create` | The element is added |
| `delete` | The element is removed |

**Injective matching.** Two objects of a pattern with the same class never match the same model
element, and neither do an object and a *bound* object the pattern references — an element input
(`seed { }`), an object of an enclosing or earlier statement, a call output or a loop variable — when
the bound element is of exactly the object's class. So with `seed { }` for an `AllocationContext`
input, a new object `other: AllocationContext { }` never matches `seed` itself, and no `where other !=
seed` is needed. Objects of different classes may match one element when one class is a subclass of
the other (`unit: AbstractAction { }` may be the element `call: ExternalCallAction { }` matched);
write `where a != b` to exclude that. Bound objects are not compared with each other: two inputs may be
the same element. A bound object that the pattern does not reference — only a `where` clause reads
it — is a value and takes no part. The objects of a `forbid` or `require` block are distinct from the
pattern's objects of the same class, not from bound objects.

To remove an object found in the same pattern, put the modifier on the object itself —
`delete book: Book { }`. `delete name` only names objects of an earlier or enclosing statement; inside the pattern
that declares `name`, it is rejected.

#### Application conditions

A `forbid` block rejects the match as soon as **its whole sub-pattern** can be found; a `require`
block demands that its whole sub-pattern is found. Each block is a graph of its own, matched
independently of the pattern around it and of every other block:

<<< @/../samples/language-tour/application-conditions.mt{mt}

The grouping is what carries the meaning. Two blocks reject the match when *either* of them matches;
the same elements inside one block reject it only when they *all* match together:

```mt
// rejected when the patient is admitted OR when a better candidate exists
forbid { a: Admission {}   a.patient -- patient }
forbid { better: Patient { surgeryDuration < duration } }

// rejected only when the patient is admitted AND a better candidate exists
forbid {
    a: Admission {}   a.patient -- patient
    better: Patient { surgeryDuration < duration }
}
```

A block may name itself — `forbid alreadyAdmitted { ... }` — following Henshin's nested names. The
name identifies the block in diagnostics, and in the graphical editor its elements are tagged with
`«forbid alreadyAdmitted»`. Inside a block you may:

- declare objects and links that belong to the condition graph alone,
- refer to objects of the enclosing pattern, which anchors the condition to the match, and
- constrain an object of the enclosing pattern through a reference — `patient { age > 60 }`.

Objects declared inside a block are not bound by the match: they are invisible outside their block,
and outside it they cannot be used in expressions, created or deleted. A condition decides whether a
match is admissible and never writes to the model, so its objects only compare properties — `=` is
rejected inside a block.

A block may also carry `where` clauses. A clause inside a block constrains that block — the block
only holds when its graph is found *and* its clauses are satisfied — and it may compare the block's
own objects with each other and with everything the match binds:

```mt
forbid betterCandidate {
    better: Patient { isMandatory == false }
    where better.surgeryDuration < patientDuration
}
```

Since the clause belongs to the block, it is subject to the same grouping rule as everything else in
it: the clause above only ever prevents *this* block from holding. Several clauses in one block are
a conjunction, and a block that holds nothing but a clause is a plain guard on the match.

::: tip Moving elements between blocks
In the graphical editor, an element created in *forbid* or *require* mode starts in a block of its
own. Use **Move to Block** in the context rail to move it — together with everything connected to it
— into another block, into a new one, or back into the match pattern. Where clauses move the same
way, unless they read an object of their block: such a clause means nothing outside it, so the move
is not offered. **Add Where Clause** on a member of a block adds a clause to that block.
:::

Inside an object's braces, `=` assigns a property while `==`, `!=`, `<`, `>`, `<=` and `>=` constrain
the match:

```mt
rectangle: Rectangle {
    visible == true
}
```

A property may be assigned at most once per object, since a second `=` on the same property would
silently overwrite the first. Comparisons are not restricted: the same property may be constrained
several times — `width > 10` together with `width < 100` — and a property that is compared may still
be assigned in the same object.

A value assigned to an `int`, `long`, `float` or `double` property takes the property's type
(truncated toward zero for `int` and `long`), as do the elements of a list assigned to a numeric
list property — on created objects, on matched ones and on references (`seed { count = n / 2 }`),
whose property is that of the bound element's class. Assigning `null` leaves an optional property
unset: `maybe = other.maybe` when `other.maybe` is unset, `maybe = null`, or a ternary whose branch
is `null` removes the value of a matched object and sets nothing on a created one. A list assigned
from an unset list property is empty, and `null` elements of a list add no value.

::: tip Names are file-global
Object names must be unique across the whole `.mt` file, not just within one pattern. An object
matched in an outer scope is referred to by name — `rectangle { visible = true }` — rather than
matched again.
:::

#### Statements

<<< @/../samples/language-tour/control-flow.mt{mt}

| Statement | Meaning |
| --- | --- |
| `match { … }` | Apply the rewrite once |
| `if match { … } then { … } else { … }` | Apply the second block only if the pattern matches |
| `for match { … } do { … }` | Run the body once per match |
| `while match { … } do { … }` | Repeat while the pattern still matches |
| `until match { … } do { … }` | Repeat until the pattern matches |
| `if (expr) { … } else if (expr) { … } else { … }` | Ordinary conditional on an expression |
| `while (expr) { … }` | Ordinary loop on an expression |
| `stop` | End the transformation successfully |
| `kill` | Abort the transformation and discard the result |
| `call name(arg = expr, …) -> (out: name, …)` | Run another transformation — see [Callable transformations](#callable-transformations) |
| `if call name(…) -> (…) then { … } else { … }` | Run the first block, with the outputs, if the callee succeeds, the second if it fails |
| `for x in collection do { … }` | Run the body once per element of a collection |
| `name = expr`, `name += expr` | Assign a variable or an output, or add to a collection output |

Expressions use the same syntax as the [Script language](/plugins/script) — the two share an
expression and type system, including the number literals (`1.0e300`, `2.5E-3f`, `42L`). The
engine compiles every expression to a Gremlin traversal without lambda steps, so the transformation
language offers the part of the script language's library that compiles that way:

| On | Methods |
| --- | --- |
| collections | `size()`, `isEmpty()`, `notEmpty()`, `sum()`, `first()`, `includes(x)` (an element `==` x), `filter`, `map`, `exists`, `all`, `none`, `one`, `find`, `reject` (with a lambda) |
| lists and ordered sets | also `last()` and `indexOf(x)` (the first position of an element `==` x, or `-1`) |
| numbers | `abs`, `floor`, `ceil`, `round`, `log`, `log10`, `pow`, `mod`, `max`, `min`; on `int` and `long` also `a.to(b)`: the `int`s from `a` to `b`, both included, counting down when `b < a` |

They behave as in scripts, in patterns, `where` clauses and statements alike, and several lambdas
may appear in one expression (`a.exists((x) => x == o) && !b.exists((y) => y == o)`). There is no
`random()`: Gremlin has no step for it.

#### Callable transformations

A transformation can call another one, in the style of the method calls of story diagrams: the
callee declares a **signature** with typed `in` and `out` parameters, and the caller **imports** it
and runs it with `call`.

<<< @/../samples/callable-transformations/generic/move-component.mt{mt}

The signature follows the `using` line and the imports. A parameter is a **model element** — its
type is a class of the metamodel —, a **scalar** — `int`, `long`, `float`, `double`, `string`,
`boolean` or an enum of the metamodel — or a **collection** of either, `List<T>` or `Set<T>`.

| Parameter | When the callee starts | Use in the callee |
| --- | --- | --- |
| `in name: Class` | bound to the element the caller passed | a bound node of every pattern: `name { … }`, `name.end -- other`, `delete name`, and in expressions |
| `in name: Class?` | bound to the passed element; when none (or null) is passed, the object `name` of the first pattern is matched freely | the object of the same name in the first pattern — see [Optional element inputs](#optional-element-inputs) |
| `in name: int = 0` | bound to the argument, or to the default when the caller passes none | a value in expressions |
| `in name: List<Class>` / `Set<int>` | the collection the caller passed | in expressions (`name.size()`, `name.exists(…)`) and in `for x in name do { … }` |
| `out name: Class` / `out name: int` | `null` | assigned with `name = expression`, in a pattern or as a statement |
| `out name: List<Class>` / `Set<string>` | empty | grown with `name += expression`, in a pattern or as a statement |

A callee whose element inputs are already bound repairs locally around them:

<<< @/../samples/callable-transformations/sync/follow-reader.mt{mt}

The caller imports the callee and names the arguments; after `->` it binds the outputs it needs to
names of its own. They are bound exactly as if a `match` had bound them: an element output is an
object that later patterns reference as `m { }`, a scalar or collection output is a value.

<<< @/../samples/callable-transformations/move-with-stores.mt{mt}

**Semantics.**

- The callee runs on the same model, with its own variables: only its inputs and outputs cross the
  call.
- A callee that **fails** — a `match` that finds nothing — fails the call, and so the caller, just as
  a failing `match` does. During an optimisation the attempt is then discarded like any other failed
  operator.
- `stop` in the callee **returns from the callee**; the caller goes on with the next statement.
  `kill` in the callee aborts the whole transformation.
- A callee that succeeds without matching anything (a `for match` with no match, an `if match` that
  takes no branch) is a success. Outputs it did not assign are `null`, and collection outputs it did
  not add to are empty. The validator therefore requires every non-nullable single output to be
  assigned on every path that succeeds; declare an output `T?` when that cannot be guaranteed. A
  nullable output can be compared with `null`, but it cannot be a node of a pattern.
- The engine does not undo the changes of a failed callee. Inside an optimisation that does not
  matter, since the whole attempt is discarded.

**Collections.** A `List` keeps its elements in the order they were added, a `Set` keeps one of
equal elements (the same model element, or equal values). `name += value` adds a value to a
collection output — or all elements of a collection value. Inside a pattern it is computed with the
match, after its changes, so it can add an object the pattern creates; in a `for match` every match
adds its value. Collections are values: they cannot be nodes of a pattern, and a callee never
changes the collections it was passed.

**Loops over collections.** `for x in collection do { … }` runs its block once per element. The
collection — a collection parameter or output, a many-valued reference such as
`placement.candidates`, or any collection expression — is evaluated once, before the first run. In
the block, `x` is a bound object when the elements are model elements (`x { … }` in the block's
patterns, `call repair(component = x)`) and a value otherwise, and it is visible in the block only,
like the objects of a `for match`. An empty collection runs the block not at all, which is a success.

**Assignments outside patterns.** `name = expression` assigns a variable of an enclosing scope or an
output, and `name += expression` adds to a collection output, as statements of their own — in a
loop's block, for instance. They count for the definite assignment of outputs like assignments in
patterns.

**`if call`.** `if call name(…) -> (…) then { … } else { … }` branches on the callee's result, as
`if match` branches on a match: the outputs are bound in the `then` block only, and the `else` block
runs when the callee fails. `stop` in the callee is a success; `kill` in the callee still aborts
everything.

<<< @/../samples/callable-transformations/move-service.mt{mt}

Since the engine cannot undo changes, the `else` block must not run on a model the callee changed
half-way. `if call` therefore only calls a transformation that **fails before it changes the
model**, which the validator checks conservatively: following the callee's statements in order, on
every path, every statement that may fail must come before every statement that may change the
model.

- A `match` may fail unless its pattern only creates objects and links, sets properties or deletes
  bound objects, and declares or assigns variables — that is, has nothing to find.
- A plain `call` may fail when its callee may fail; the callee must then satisfy the same rule.
  An `if call` inside the callee does not fail.
- A loop counts as running its block after the block's own changes, so a block that changes the
  model and may fail is rejected.
- `if match`, `for match`, `while match` and `stop` do not fail; `kill` is not a failure either.

The error names the statement that may fail and the change before it, e.g. *`'unsafe' can fail at
line 10 of unsafe.mt (a match that may find nothing) after the change at line 5 of unsafe.mt`*.
Should a failing callee have changed the model anyway, the engine reports the failure instead of
running the `else` block.

<a id="optional-element-inputs"></a>

**Optional element inputs.** A nullable element input `in placement: Placement?` whose name is that
of an object of the **first pattern** — the pattern of the first statement, a `match`, `if match`,
`for match` or `while match` — *pins* that object (story diagrams' "maybe-bound" objects): a caller
that passes a placement binds the object to it, and when none is passed, or `null`, the pattern
matches the object freely. The same file thus works as a mutation operator, where nothing is passed,
and as a callee. The object's class must be the input's class or a superclass of it, the object
cannot be one the pattern creates, and the input is optional: a call or a configuration may leave it
out. A required (non-nullable) element input is bound when the transformation starts, so no pattern
may declare an object of its name — reference it as `name { }`.

**Layered metamodels.** A callee's metamodel must be the caller's metamodel or one that the caller's
metamodel imports (directly or not). A transformation written for a base metamodel — here
`architecture.mm` — therefore runs inside a transformation of an extending metamodel —
`microservices.mm` — and its patterns also match the subclasses that metamodel adds. The reverse is
rejected.

**Checks in the editor.** The validator reports, on the line concerned:

- arguments that name no parameter of the callee, an output passed as an argument or an input bound
  as an output, and arguments or outputs given twice;
- missing required inputs (an `in` parameter without default whose type is not nullable);
- argument types that are not assignable to the parameter's type (a subclass of an element
  parameter's class is fine, and so is a collection of a subclass);
- imports of files that do not exist, that have no signature or whose signature has another name;
- callees of an incompatible metamodel;
- import cycles: recursive calls are not supported;
- outputs that are not assigned on every successful path (at the end and at every `stop`);
- assignments to `in` parameters, `+=` on anything but a collection output, values of the wrong
  type for `=` and `+=`, and parameters, outputs or loop variables used as pattern nodes although
  they are scalars, collections, unbound outputs or nullable;
- loops over expressions that are not collections;
- `if call` of a transformation that may fail after it changed the model;
- nullable element inputs that pin no object of the first pattern, or one of another class.

Parameter, output and loop-variable names take part in the file-wide uniqueness of names (an
optional element input shares its name with the object it pins). Hover over a call, an import, an
argument or an output shows the callee's signature or the parameter; *Go to definition* jumps into
the callee. Completion offers the callee's inputs inside the parentheses and its outputs after `->`.

**As mutation operators.** A transformation listed in a [`search` block](/plugins/config-mdeo#the-search-section)
must not have required inputs: the optimizer passes no arguments, so inputs take their defaults,
optional inputs are null — optional element inputs leave their objects free — and outputs are
ignored. List the wrapper (`move-with-stores.mt`), not its callees: the wrapper is one operator in
the random choice of operators, so the move and its repair always happen together.

A file with required inputs cannot be run on its own with the **Run** action either; it only runs
when another transformation calls it.

::: info Reserved words
`transformation`, `in`, `out`, `call`, `import` and `from` are keywords of the language. A name that
collides with one of them is written in backticks, e.g. `` `from` ``.
:::

::: info Not yet supported
Recursion, and passing arguments to a transformation from a configuration
(`using "x.mt"(k = v)`), are not supported. There is no `try call` that undoes a failed callee's
changes; `if call` is limited to callees that fail before changing the model instead.
:::

### The generated model transformation language

`.mt_gen` files are transformations the platform produced itself. An optimisation run can generate
mutation rules from the `create` / `delete` / `mutate` entries of a `search` block, and writes them
into the result tree so you can see exactly which rewrites the search was allowed to perform.

## Contribution plugins contributed

None.

## Server-side capabilities

| File data key | Contents |
| --- | --- |
| `ast` | The serialised AST |
| `typed-ast` | The type-annotated AST the execution service interprets; the typed ASTs of the transformations it calls are bundled into it |
| `model-transformation-text` | The textual rendering of a generated `.mt_gen` file |

**Execution.** A transformation can be run against a model. The plugin forwards the request to the
`model-transformation-execution` service, configured through
`MODEL_TRANSFORMATION_EXECUTION_SERVICE_URL`.

## Graphical editor

Transformations have a diagram editor too. Pattern objects and links appear as nodes and edges, with
their modifier reflected in the styling, so the effect of a rule can be read at a glance. Element
parameters, call outputs and loop variables appear as bound objects in the patterns that use them.

A call is a node on the control flow, drawn with a double left edge, that shows the call's text.
Double-click the text to edit the call's arguments and outputs. **Insert Statement** on a control
flow edge offers **Call** and **If Call** for every imported transformation (inserted without
arguments; the validator lists the inputs to pass), and deleting a call node deletes the call — for
an `if call`, the whole statement with its blocks. An `if call` branches into *success* and
*failure* edges like the *then* and *else* edges of `if match`. A `for … in` loop is a node with its
body on an *each* edge, and an assignment outside a pattern is a node with its text. Loop heads and
assignments are edited in the textual editor.
