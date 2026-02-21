import { sharedImport } from "@mdeo/editor-shared";
import type { PatternModifierKind } from "./elementTypes.js";
import type { Bounds, BoundsAware, Dimension } from "@eclipse-glsp/sprotty";

const { GChildElement, boundsFeature, Bounds: BoundsUtil } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Client-side model for a pattern link modifier label.
 * Displayed in the middle of a pattern link edge when a modifier
 * (create/delete/forbid) is present on the link.
 */
export class GPatternLinkModifierLabel extends GChildElement implements BoundsAware {
    static readonly DEFAULT_FEATURES = [boundsFeature];

    /**
     * The modifier kind driving which text/colour to show
     */
    modifier!: PatternModifierKind;

    bounds: Bounds = BoundsUtil.EMPTY;

    get size(): Dimension {
        return { width: this.bounds.width, height: this.bounds.height };
    }

    set size(value: Dimension) {
        this.bounds = { ...this.bounds, width: value.width, height: value.height };
    }
}
