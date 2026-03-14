import { GNode, sharedImport } from "@mdeo/editor-shared";
import { PatternModifierKind } from "@mdeo/protocol-model-transformation";

const { boundsFeature } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Client-side model for a pattern link modifier node.
 * Wraps the modifier label to provide proper bounds handling for the
 * «create»/«delete»/«forbid»/«require» badge in the middle of a pattern link edge.
 */
export class GPatternLinkModifierNode extends GNode {
    static readonly DEFAULT_FEATURES = [boundsFeature];

    /**
     * The modifier kind driving which colour to apply
     */
    modifier: PatternModifierKind = PatternModifierKind.NONE;
}
