/**
 * A layered metamodel and transformations for the tests of callable transformations, after the
 * Palladio architecture projects: layer 2 (`base.mm`) places components on containers, layer 3
 * (`shop.mm`) adds services and copy feeds whose store must stay with its reader.
 */

export const BASE_MM = `class Component {
    name: string
}

class Container {
    name: string
}

class Placement {
}

Component.host[1] --> Container
Placement.component[1] --> Component
Placement.candidates[1..*] --> Container

enum Kind {
    SMALL
    LARGE
}
`;

export const SHOP_MM = `import "./base.mm"

class Service extends Component {
}

class CopyFeed {
}

CopyFeed.reader[1] --> Component
CopyFeed.store[1] --> Component
`;

export const OTHER_MM = `class Thing {
    name: string
}
`;

/**
 * Layer 2: moves a component to another of its candidate containers and returns what moved.
 */
export const MOVE_COMPONENT = `using "../../metamodel/base.mm"

transformation moveComponent(
    in maxCandidates: int = 0,
    out moved: Component,
    out source: Container,
    out target: Container,
    out choices: int
)

match {
    placement: Placement { }
    component: Component { }
    current: Container { }
    destination: Container { }
    placement.component -- component
    placement.candidates -- destination
    where maxCandidates == 0 || placement.candidates.size() <= maxCandidates

    delete component.host -- current
    create component.host -- destination

    moved = component
    source = current
    target = destination
    choices = placement.candidates.size()
}
`;

/**
 * Layer 3: every copy store of the moved reader follows it to its new container.
 */
export const FOLLOW_READER = `using "../../metamodel/shop.mm"

transformation followReader(in moved: Component, in target: Container)

for match {
    moved { }
    target { }
    feed: CopyFeed { }
    store: Component { }
    old: Container { }
    feed.reader -- moved
    feed.store -- store
    delete store.host -- old
    create store.host -- target
    forbid {
        store.host -- target
    }
} do {
}
`;

/**
 * Layer 3: the operator the configuration lists.
 */
export const MOVE_WITH_STORES = `using "../metamodel/shop.mm"
import { moveComponent } from "./generic/move-component.mt"
import { followReader } from "./sync/follow-reader.mt"

call moveComponent(maxCandidates = 4) -> (moved: m, source: a, target: b, choices: n)
call followReader(moved = m, target = b)

match {
    m { }
    b { }
    where n >= 1 && a.name != b.name
}
`;

/**
 * The project of the worked example.
 */
export function layeredProject(extra: Record<string, string> = {}): Record<string, string> {
    return {
        "/metamodel/base.mm": BASE_MM,
        "/metamodel/shop.mm": SHOP_MM,
        "/metamodel/other.mm": OTHER_MM,
        "/optimize/generic/move-component.mt": MOVE_COMPONENT,
        "/optimize/sync/follow-reader.mt": FOLLOW_READER,
        "/optimize/move-with-stores.mt": MOVE_WITH_STORES,
        ...extra
    };
}

/**
 * Scenarios of the engine tests (platform/model-transformation, CallStatementExecutionTest):
 * failure propagation, `stop` and `kill` in a callee, success without a match, scalar inputs.
 */
export const SCENARIOS: Record<string, string> = {
    // a callee whose match fails: the call fails, and so the caller
    "/optimize/scenarios/no-placement.mt": `using "../../metamodel/base.mm"

transformation noPlacement(in wanted: string)

match {
    placement: Placement { }
    component: Component {
        name == wanted
    }
    placement.component -- component
}
`,
    "/optimize/scenarios/call-failing.mt": `using "../../metamodel/shop.mm"
import { noPlacement } from "./no-placement.mt"

match {
    first: Container {
        name = "before"
    }
}
call noPlacement(wanted = "nobody")
match {
    second: Container {
        name = "after"
    }
}
`,
    // stop returns from the callee only
    "/optimize/scenarios/rename-unless-skipped.mt": `using "../../metamodel/base.mm"

transformation renameUnlessSkipped(in target: Container, in newName: string, out renamed: boolean)

match {
    var no = false
    renamed = no
}
if match {
    target {
        name == "skip"
    }
} then {
    stop
}
match {
    target {
        name = newName
    }
    var yes = true
    renamed = yes
}
`,
    "/optimize/scenarios/call-stopping.mt": `using "../../metamodel/shop.mm"
import { renameUnlessSkipped } from "./rename-unless-skipped.mt"

match {
    c: Container {
        name == "skip"
    }
}
call renameUnlessSkipped(target = c, newName = "renamed") -> (renamed: r)
match {
    c {
        name = "after"
    }
    where !r
}
`,
    // kill in a callee ends the whole transformation
    "/optimize/scenarios/killer.mt": `using "../../metamodel/base.mm"

transformation killer()

match {
    c: Container {
        name = "killed"
    }
}
kill
`,
    "/optimize/scenarios/call-killing.mt": `using "../../metamodel/shop.mm"
import { killer } from "./killer.mt"

call killer()
match {
    c: Container {
        name = "unreachable"
    }
}
`,
    // a callee that matches nothing succeeds; its nullable output stays null
    "/optimize/scenarios/find-named.mt": `using "../../metamodel/base.mm"

transformation findNamed(in wanted: string = "", out found: Container?, out count: int)

match {
    var zero = 0
    count = zero
}
for match {
    c: Container {
        name == wanted
    }
} do {
}
if match {
    named: Container {
        name == wanted
    }
} then {
    match {
        named { }
        found = named
        var one = 1
        count = one
    }
}
`,
    "/optimize/scenarios/call-finding.mt": `using "../../metamodel/shop.mm"
import { findNamed } from "./find-named.mt"

call findNamed(wanted = "nobody") -> (found: f, count: n)
match {
    c: Container {
        name = "none found"
    }
    where f == null && n == 0
}
`,
    // a callee that creates a node and returns it
    "/optimize/scenarios/make-container.mt": `using "../../metamodel/base.mm"

transformation makeContainer(in label: string, out made: Container)

match {
    create c: Container {
        name = label
    }
    made = c
}
`,
    "/optimize/scenarios/call-making.mt": `using "../../metamodel/shop.mm"
import { makeContainer } from "./make-container.mt"

call makeContainer(label = "fresh") -> (made: f)
match {
    s: Service { }
    old: Container { }
    delete s.host -- old
    create s.host -- f
    f {
        name = "fresh and used"
    }
}
`,
    // a call chain: a callee that calls two other transformations and passes an output on
    "/optimize/scenarios/move-and-sync.mt": `using "../../metamodel/shop.mm"
import { moveComponent } from "../generic/move-component.mt"
import { followReader } from "../sync/follow-reader.mt"

transformation moveAndSync(out movedTo: Container)

call moveComponent() -> (moved: m, target: b)
call followReader(moved = m, target = b)
match {
    b { }
    movedTo = b
}
`,
    "/optimize/scenarios/call-nested.mt": `using "../../metamodel/shop.mm"
import { moveAndSync } from "./move-and-sync.mt"

call moveAndSync() -> (movedTo: t)
match {
    t {
        name = "chosen"
    }
}
`
};

/**
 * Scenarios of stage 2 (collections, `for … in`, assignments outside patterns, `if call`) and
 * stage 3 (optional element inputs that pin an object of the first pattern), for the engine
 * tests (platform/model-transformation, CollectionCallExecutionTest).
 */
export const COLLECTION_SCENARIOS: Record<string, string> = {
    // collection outputs: += inside a for-match pattern and in its do block
    "/optimize/scenarios/components-on.mt": `using "../../metamodel/base.mm"

transformation componentsOn(in host: Container, out components: List<Component>, out names: Set<string>)

for match {
    host { }
    c: Component { }
    c.host -- host
    components += c
} do {
    names += c.name
}
`,
    // a local repair per element
    "/optimize/scenarios/mark-component.mt": `using "../../metamodel/base.mm"

transformation markComponent(in component: Component, in label: string)

match {
    component {
        name = label
    }
}
`,
    // a collection input, a loop over it and assignments outside patterns
    "/optimize/scenarios/rename-all.mt": `using "../../metamodel/base.mm"

transformation renameAll(in components: List<Component>, in prefix: string, out renamed: int)

renamed = 0
for c in components do {
    match {
        c {
            name = prefix
        }
    }
    renamed = renamed + 1
}
`,
    // a collection output assigned from a many-valued reference
    "/optimize/scenarios/candidates-of.mt": `using "../../metamodel/base.mm"

transformation candidatesOf(out options: Set<Container>)

match {
    p: Placement { }
    options = p.candidates
}
`,
    "/optimize/scenarios/call-collecting.mt": `using "../../metamodel/shop.mm"
import { moveComponent } from "../generic/move-component.mt"
import { componentsOn } from "./components-on.mt"
import { markComponent } from "./mark-component.mt"
import { renameAll } from "./rename-all.mt"
import { candidatesOf } from "./candidates-of.mt"

call moveComponent() -> (moved: m, source: a, target: b)
call componentsOn(host = b) -> (components: here, names: hereNames)
for x in here do {
    call markComponent(component = x, label = "on b")
}
for n in hereNames do {
    match {
        create log: Container {
            name = n
        }
    }
}
call componentsOn(host = a) -> (components: left, names: leftNames)
call renameAll(components = left, prefix = "left") -> (renamed: k)
call candidatesOf() -> (options: choices)
for choice in choices do {
    match {
        choice {
            name = "candidate"
        }
    }
}
match {
    b { }
    where k == left.size() && hereNames.size() == here.size() && leftNames.size() == 1
}
`,
    // if call: the else block after a callee that fails before changing anything, outputs in then
    "/optimize/scenarios/call-if.mt": `using "../../metamodel/shop.mm"
import { moveComponent } from "../generic/move-component.mt"
import { noPlacement } from "./no-placement.mt"

if call noPlacement(wanted = "nobody") then {
    match {
        create c1: Container {
            name = "then ran"
        }
    }
} else {
    match {
        create c2: Container {
            name = "else ran"
        }
    }
}
if call moveComponent() -> (moved: m, target: t) then {
    match {
        t {
            name = "moved here"
        }
    }
} else {
    kill
}
`,
    // kill in a callee of if call still ends everything
    "/optimize/scenarios/call-if-killing.mt": `using "../../metamodel/shop.mm"
import { killer } from "./killer.mt"

if call killer() then {
    match {
        create c1: Container {
            name = "then ran"
        }
    }
} else {
    match {
        create c2: Container {
            name = "else ran"
        }
    }
}
`,
    // proposal C: an optional element input pins the object of the same name of the first
    // pattern; the same file is an operator (placement matched freely) and a callee
    "/optimize/scenarios/move-placement.mt": `using "../../metamodel/base.mm"

transformation movePlacement(in placement: Placement?, out moved: Component, out target: Container)

match {
    placement: Placement { }
    component: Component { }
    current: Container { }
    destination: Container { }
    placement.component -- component
    placement.candidates -- destination
    delete component.host -- current
    create component.host -- destination
    moved = component
    target = destination
}
`,
    "/optimize/scenarios/call-pinned.mt": `using "../../metamodel/shop.mm"
import { movePlacement } from "./move-placement.mt"

match {
    p: Placement { }
    s: Service { }
    p.component -- s
}
call movePlacement(placement = p) -> (moved: m, target: t)
match {
    t {
        name = "pinned target"
    }
}
`,
    // loops over a many-valued reference and over a filtered collection; an element output
    // assigned outside a pattern
    "/optimize/scenarios/pick-candidate.mt": `using "../../metamodel/base.mm"

transformation pickCandidate(in placement: Placement, out picked: Container?, out seen: int)

seen = 0
for candidate in placement.candidates do {
    picked = candidate
    seen = seen + 1
}
`,
    "/optimize/scenarios/call-looping.mt": `using "../../metamodel/shop.mm"
import { pickCandidate } from "./pick-candidate.mt"

match {
    p: Placement { }
}
for named in p.candidates.filter((c) => c.name == "c") do {
    match {
        named {
            name = "filtered"
        }
    }
}
for candidate in p.candidates.filter((c) => c.name != "filtered") do {
    match {
        candidate {
            name = "candidate"
        }
    }
}
call pickCandidate(placement = p) -> (picked: chosen, seen: n)
match {
    create log: Container {
        name = "seen " + n
    }
    where chosen != null
}
`
};

/**
 * Scenarios of the language traps found by the domain project's group operators (DESIGN.md G9
 * there), for the engine tests (platform/model-transformation, LanguageTrapsExecutionTest): two
 * collection lambdas in one expression, `isEmpty()`/`notEmpty()` as booleans, injectivity of
 * bound objects, a `where` that reads an input the pattern does not reference, `to`, `indexOf`,
 * `includes`, and exponent float literals.
 */
export const TRAP_SCENARIOS: Record<string, string> = {
    // two lambdas in one where expression, and a ternary whose condition is a lambda
    "/optimize/scenarios/call-trap-lambdas.mt": `using "../../metamodel/shop.mm"
import { componentsOn } from "./components-on.mt"

match {
    hostA: Container {
        name == "a"
    }
    hostB: Container {
        name == "b"
    }
}
call componentsOn(host = hostA) -> (components: onA)
call componentsOn(host = hostB) -> (components: onB)
for match {
    x: Component { }
    where onA.exists((p) => p == x) && !onB.exists((q) => q == x)
} do {
    match {
        create log: Container {
            name = "only a " + x.name
        }
    }
}
for match {
    y: Component { }
    where onB.exists((p) => p == y) || onA.exists((q) => q == y) && y.name == "nobody"
} do {
    match {
        create log2: Container {
            name = (onB.exists((r) => r == y) ? "on b " : "not on b ") + y.name
        }
    }
}
`,
    // isEmpty() and notEmpty() are booleans, in conditions and as values
    "/optimize/scenarios/call-trap-empty.mt": `using "../../metamodel/shop.mm"
import { componentsOn } from "./components-on.mt"

match {
    hostA: Container {
        name == "a"
    }
    hostC: Container {
        name == "c"
    }
}
call componentsOn(host = hostA) -> (components: onA)
call componentsOn(host = hostC) -> (components: onC)
match {
    hostA { }
    var emptyA = onA.isEmpty()
    var fullC = onC.notEmpty()
    where onC.isEmpty() && onA.notEmpty()
    where !onA.isEmpty()
    create log: Container {
        name = "empty " + emptyA + " " + fullC
    }
}
if (onA.isEmpty() || !onC.isEmpty()) {
    kill
}
`,
    // a bound input is distinct from the pattern's other objects (injective matching)
    "/optimize/scenarios/mates-of.mt": `using "../../metamodel/base.mm"

transformation matesOf(in seed: Component, out mates: List<Component>)

for match {
    seed { }
    home: Container { }
    mate: Component { }
    seed.host -- home
    mate.host -- home
} do {
    mates += mate
}
`,
    "/optimize/scenarios/call-trap-injective.mt": `using "../../metamodel/shop.mm"
import { matesOf } from "./mates-of.mt"

match {
    db: Component {
        name == "ordersDb"
    }
    svc: Service {
        name == "orders"
    }
}
call matesOf(seed = db) -> (mates: dbMates)
call matesOf(seed = svc) -> (mates: svcMates)
for match {
    svc { }
    other: Component { }
    other.host -- svcHost
    svc.host -- svcHost
    svcHost: Container { }
} do {
    match {
        create seen: Container {
            name = "svc mate " + other.name
        }
    }
}
for match {
    s2: Service { }
    c2: Component { }
    s2.host -- h2
    c2.host -- h2
    h2: Container { }
} do {
    match {
        create seen2: Container {
            name = "pair " + s2.name + " " + c2.name
        }
    }
}
match {
    create log: Container {
        name = "mates " + dbMates.size() + " " + svcMates.size()
    }
}
`,
    // a where clause that reads an input the pattern does not reference
    "/optimize/scenarios/count-others.mt": `using "../../metamodel/base.mm"

transformation countOthers(in seed: Component, in home: Container, out others: int, out same: int, out near: int)

others = 0
same = 0
near = 0
for match {
    other: Component { }
    where other != seed
} do {
    others = others + 1
}
for match {
    again: Component { }
    where again == seed && seed.name == again.name
} do {
    same = same + 1
}
for match {
    home { }
    nearby: Component { }
    nearby.host -- home
    where nearby != seed
} do {
    near = near + 1
}
`,
    "/optimize/scenarios/call-trap-unreferenced.mt": `using "../../metamodel/shop.mm"
import { countOthers } from "./count-others.mt"

match {
    db: Component {
        name == "ordersDb"
    }
    hostA: Container {
        name == "a"
    }
}
call countOthers(seed = db, home = hostA) -> (others: n, same: k, near: m)
match {
    create log: Container {
        name = "others " + n + " " + k + " " + m
    }
}
`,
    // to, indexOf and includes as in the script language
    "/optimize/scenarios/call-trap-functions.mt": `using "../../metamodel/shop.mm"
import { componentsOn } from "./components-on.mt"

match {
    hostA: Container {
        name == "a"
    }
}
call componentsOn(host = hostA) -> (components: onA)
for match {
    x: Component { }
    where onA.includes(x)
} do {
    match {
        create log: Container {
            name = "includes " + x.name
        }
    }
}
match {
    hostA { }
    var up = 1.to(4)
    var down = 3.to(1)
    var one = 5.to(5)
    var big = 2L.to(3L)
    where up.size() == 4 && up.sum() == 10 && down.first() == 3 && one.size() == 1
    where [10, 20, 30].indexOf(20) == 1 && [10, 20].indexOf(5) == -1 && up.indexOf(3) == 2
    where [1, 2].includes(2) && !up.includes(9)
    create log2: Container {
        name = "range " + up.size() + " " + down.size() + " " + down.last() + " " + big.size() + " " + up.indexOf(4)
    }
}
for i in 2.to(4) do {
    match {
        create step: Container {
            name = "step " + i
        }
    }
}
if (!2.to(4).includes(3) || 2.to(4).indexOf(4) != 2 || [5, 6].indexOf(7) != -1) {
    kill
}
`,
    // properties of a referenced object resolve against its class, not by name across the
    // metamodel: Access.shape is the first field of Access, Call.shape the second of Call
    "/metamodel/shapes.mm": `class Access {
    shape: string
}

class Call {
    built: string
    shape: string
}
`,
    "/optimize/scenarios/reshape.mt": `using "../../metamodel/shapes.mm"

transformation reshape(in rc: Call)

match {
    rc {
        shape == "composed"
        built == "plain"
        shape = "redirect"
        built = "composed"
    }
}
`,
    "/optimize/scenarios/call-trap-reference-properties.mt": `using "../../metamodel/shapes.mm"
import { reshape } from "./reshape.mt"

match {
    c: Call {
        built == "plain"
    }
}
call reshape(rc = c)
match {
    c {
        shape == "redirect"
        built == "composed"
    }
}
`,
    // float literals with an exponent
    "/optimize/scenarios/call-trap-literals.mt": `using "../../metamodel/shop.mm"

match {
    hostA: Container {
        name == "a"
    }
    var huge = 1.0e300
    var tiny = 2.5E-3
    var plain = 1e3
    where huge > 1.0e299 && tiny < 0.01 && plain == 1000.0
    create log: Container {
        name = "literals ok"
    }
}
`,
    // numeric properties take the property's type (int/long/float/double), for computed values
    // and the elements of lists, on created and on matched objects
    "/metamodel/numbers.mm": `class Bag {
    name: string
    i: int
    l: long
    f: float
    d: double
    maybe: double[0..1]
    ints: int[0..*]
    longs: long[0..*]
    floats: float[0..*]
    doubles: double[0..*]
}
`,
    "/optimize/scenarios/call-trap-numeric-lists.mt": `using "../../metamodel/numbers.mm"

match {
    seed: Bag {
        name == "seed"
    }
    other: Bag {
        name == "other"
    }
    create made: Bag {
        name = "made"
        i = seed.i + 1
        l = seed.i
        f = seed.i
        d = seed.i
        ints = [seed.i, 2]
        longs = [seed.i, 5]
        floats = [seed.i, 2]
        doubles = [seed.i, 2]
    }
    create empty: Bag {
        name = "empty"
        i = 0
        l = 0
        f = 0
        d = 0
        ints = []
        doubles = []
    }
}
match {
    again: Bag {
        name == "seed"
        l = again.i * 2
        d = again.i
        ints = [7, again.i]
        longs = []
        floats = [again.i]
        doubles = [1, again.i, other.maybe]
    }
}
`
};
