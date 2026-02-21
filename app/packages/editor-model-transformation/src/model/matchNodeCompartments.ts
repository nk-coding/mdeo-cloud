import type { Bounds, BoundsAware, Dimension } from "@eclipse-glsp/sprotty";
import { GCompartment, sharedImport } from "@mdeo/editor-shared";

const { boundsFeature, Bounds: BoundsUtil } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Client-side model for the match node compartments container.
 * Wraps the where-clause and variable compartments of a match node,
 * along with the horizontal dividers between them.
 */
export class GMatchNodeCompartments extends GCompartment implements BoundsAware {
    static readonly DEFAULT_FEATURES = [boundsFeature];

    bounds: Bounds = BoundsUtil.EMPTY;

    get size(): Dimension {
        return { width: this.bounds.width, height: this.bounds.height };
    }

    set size(value: Dimension) {
        this.bounds = { ...this.bounds, width: value.width, height: value.height };
    }
}
