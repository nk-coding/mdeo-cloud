using "./microservices.mm"
import { moveComponent } from "./generic/move-component.mt"
import { followReader } from "./sync/follow-reader.mt"

// The operator a layer-3 configuration lists. It counts as one operator: the generic move and
// the repair always run together. If moveComponent fails, the whole operator fails.
call moveComponent(maxCandidates = 4) -> (moved: m, source: a, target: b)
call followReader(moved = m, target = b)

// the outputs are bound like the objects of a match
match {
    m { }
    where a.name != b.name
}
