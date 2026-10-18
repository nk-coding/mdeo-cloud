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
`
};
