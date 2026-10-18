using "./microservices.mm"
import { moveComponent } from "./generic/move-component.mt"
import { storesOf } from "./sync/stores-of.mt"
import { followHost } from "./sync/follow-host.mt"

// The operator a layer-3 configuration lists. It counts as one operator: the generic move and
// the repair always run together. If moveComponent fails, the whole operator fails.
call moveComponent(maxCandidates = 4) -> (moved: m, source: a, target: b)
call storesOf(reader = m) -> (stores: s)
for store in s do {
    call followHost(component = store, host = b)
}

// the outputs are bound like the objects of a match
match {
    m { }
    where a.name != b.name
}
