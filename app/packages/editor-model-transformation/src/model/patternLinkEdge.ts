import { GEdge } from "@mdeo/editor-shared";
import { PatternModifierKind } from "./elementTypes.js";

/**
 * Client-side model for pattern link edges.
 * Represents a link between two pattern instances with optional property specifications.
 */
export class GPatternLinkEdge extends GEdge {
    /**
     * The optional source property name
     */
    sourceProperty?: string;

    /**
     * The optional target property name
     */
    targetProperty?: string;

    /**
     * The modifier kind (none, create, delete, forbid)
     */
    modifier!: PatternModifierKind;
}
