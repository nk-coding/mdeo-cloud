import { GEdge } from "@mdeo/editor-shared";
import type { PatternModifierKind } from "./elementTypes.js";

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
     * The modifier kind (none, create, delete, forbid, require)
     */
    modifier!: PatternModifierKind;

    /**
     * The name of the metamodel class required at the source of this link type.
     * Used by canConnect to validate source node eligibility.
     */
    sourceClass?: string;

    /**
     * The name of the metamodel class required at the target of this link type.
     * Used by canConnect to validate target node eligibility.
     */
    targetClass?: string;
}
