import type { BoundsAwareModelElement, GEdge as GEdgeType } from "@eclipse-glsp/client";
import type { TypeGuard } from "@eclipse-glsp/protocol";
import { sharedImport } from "../../sharedImport.js";
import { GNode } from "../../model/node.js";
import { GEdge } from "../../model/edge.js";

const { injectable } = sharedImport("inversify");
const {
    MarqueeUtil: GLSPMarqueeUtil,
    toTypeGuard,
    typeGuard,
    isSelectableAndBoundsAware
} = sharedImport("@eclipse-glsp/client");
const { isSelectable } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Custom MarqueeUtil that works with custom node/edge types
 */
@injectable()
export class MarqueeUtil extends GLSPMarqueeUtil {
    protected override isMarkableNode(): TypeGuard<BoundsAwareModelElement> {
        return (element): element is BoundsAwareModelElement =>
            element instanceof GNode && isSelectableAndBoundsAware(element) && !(element.parent instanceof GEdge);
    }

    protected override isMarkableEdge(): TypeGuard<GEdgeType> {
        return typeGuard(toTypeGuard(GEdge), isSelectable) as unknown as TypeGuard<GEdgeType>;
    }

    override isMarked(element: BoundsAwareModelElement | GEdgeType): boolean {
        return element instanceof GEdge ? this.isMarkedEdge(element as GEdgeType) : this.isMarkedNode(element);
    }
}
