import type { Point } from "@eclipse-glsp/protocol";
import type { NodePositionMetadata, NodeLayoutMetadata, EdgeLayoutMetadata } from "@mdeo/editor-protocol";

/**
 * Helper function to validate if an object is a valid Point.
 *
 * @param obj The object to validate
 * @returns True if the object is a valid Point
 */
function isValidPoint(obj: unknown): obj is Point {
    return (
        typeof obj === "object" &&
        obj !== null &&
        typeof (obj as Point).x === "number" &&
        typeof (obj as Point).y === "number"
    );
}

/**
 * Namespace providing helper methods for node position metadata validation and manipulation.
 */
export namespace NodePositionMetadataUtil {
    /**
     * Checks if the given object is a valid NodePositionMetadata.
     *
     * @param obj The object to validate
     * @returns True if the object is a valid NodePositionMetadata
     */
    export function isValid(obj: unknown): obj is NodePositionMetadata {
        if (typeof obj !== "object" || obj === null) {
            return false;
        }

        const meta = obj as Partial<NodePositionMetadata>;
        return meta.position === undefined || isValidPoint(meta.position);
    }

    /**
     * Creates a default NodePositionMetadata with the given position.
     *
     * @param x The x coordinate
     * @param y The y coordinate
     * @returns A new NodePositionMetadata object
     */
    export function create(x: number, y: number): NodePositionMetadata {
        return { position: { x, y } };
    }

    /**
     * Verifies and corrects invalid metadata.
     *
     * @param obj The object to verify
     * @param defaultX Default x coordinate if position is invalid
     * @param defaultY Default y coordinate if position is invalid
     * @returns Corrected metadata if invalid, undefined if valid
     */
    export function verify(obj: unknown, defaultX: number = 0, defaultY: number = 0): NodePositionMetadata | undefined {
        if (isValid(obj)) {
            return undefined;
        }

        const meta = (typeof obj === "object" && obj !== null ? obj : {}) as Partial<NodePositionMetadata>;
        const position = meta.position && isValidPoint(meta.position) ? meta.position : { x: defaultX, y: defaultY };

        return { position };
    }
}

/**
 * Namespace providing helper methods for node layout metadata validation and manipulation.
 */
export namespace NodeLayoutMetadataUtil {
    /**
     * Checks if the given object is a valid NodeLayoutMetadata.
     *
     * @param obj The object to validate
     * @returns True if the object is a valid NodeLayoutMetadata
     */
    export function isValid(obj: unknown): obj is NodeLayoutMetadata {
        if (!NodePositionMetadataUtil.isValid(obj)) {
            return false;
        }

        const meta = obj as Partial<NodeLayoutMetadata>;
        return typeof meta.prefWidth === "number" || meta.prefWidth === undefined;
    }

    /**
     * Creates a default NodeLayoutMetadata with the given position.
     *
     * @param x The x coordinate
     * @param y The y coordinate
     * @param preferredWidth Optional preferred width
     * @returns A new NodeLayoutMetadata object
     */
    export function create(x: number = 0, y: number = 0, preferredWidth?: number): NodeLayoutMetadata {
        const metadata: NodeLayoutMetadata = {
            position: { x, y }
        };
        if (preferredWidth !== undefined) {
            metadata.prefWidth = preferredWidth;
        }
        return metadata;
    }

    /**
     * Verifies and corrects invalid metadata.
     *
     * @param obj The object to verify
     * @param defaultPrefWidth Default preferred width if invalid
     * @returns Corrected metadata if invalid, undefined if valid
     */
    export function verify(obj: unknown, defaultPrefWidth?: number): NodeLayoutMetadata | undefined {
        if (isValid(obj)) {
            return undefined;
        }

        const meta = (typeof obj === "object" && obj !== null ? obj : {}) as Partial<NodeLayoutMetadata>;
        const position = meta.position && isValidPoint(meta.position) ? meta.position : { x: 0, y: 0 };
        const prefWidth = typeof meta.prefWidth === "number" ? meta.prefWidth : defaultPrefWidth;
        const prefHeight = typeof meta.prefHeight === "number" ? meta.prefHeight : undefined;

        return { position, prefWidth, prefHeight };
    }
}

/**
 * Helper function to validate if an object is a valid EdgeAnchor.
 *
 * @param obj The object to validate
 * @returns True if the object is a valid EdgeAnchor
 */
function isValidAnchor(obj: unknown): boolean {
    if (typeof obj !== "object" || obj === null) {
        return false;
    }
    const anchor = obj as Record<string, unknown>;
    const validSides = ["top", "bottom", "left", "right"];
    return (
        typeof anchor.side === "string" &&
        validSides.includes(anchor.side) &&
        typeof anchor.value === "number" &&
        anchor.value >= 0 &&
        anchor.value <= 1
    );
}

/**
 * Namespace providing helper methods for edge metadata validation and manipulation.
 */
export namespace EdgeLayoutMetadataUtil {
    /**
     * Checks if the given object is a valid EdgeLayoutMetadata.
     *
     * @param obj The object to validate
     * @returns True if the object is a valid EdgeLayoutMetadata
     */
    export function isValid(obj: unknown): obj is EdgeLayoutMetadata {
        if (typeof obj !== "object" || obj === null) {
            return false;
        }

        const meta = obj as Partial<EdgeLayoutMetadata>;

        if (!Array.isArray(meta.routingPoints)) {
            return false;
        }

        if (!meta.routingPoints.every((point) => isValidPoint(point))) {
            return false;
        }

        if (meta.sourceAnchor !== undefined && !isValidAnchor(meta.sourceAnchor)) {
            return false;
        }

        if (meta.targetAnchor !== undefined && !isValidAnchor(meta.targetAnchor)) {
            return false;
        }

        return true;
    }

    /**
     * Creates a default EdgeLayoutMetadata with the given routing points.
     *
     * @param routingPoints Array of routing points
     * @returns A new EdgeLayoutMetadata object
     */
    export function create(routingPoints: Point[] = []): EdgeLayoutMetadata {
        return { routingPoints };
    }

    /**
     * Verifies and corrects invalid metadata.
     *
     * @param obj The object to verify
     * @returns Corrected metadata if invalid, undefined if valid
     */
    export function verify(obj: unknown): EdgeLayoutMetadata | undefined {
        if (isValid(obj)) {
            return undefined;
        }

        const meta = (typeof obj === "object" && obj !== null ? obj : {}) as Partial<EdgeLayoutMetadata>;
        const routingPoints = Array.isArray(meta.routingPoints)
            ? meta.routingPoints.filter((point) => isValidPoint(point))
            : [];

        const sourceAnchor = isValidAnchor(meta.sourceAnchor) ? meta.sourceAnchor : undefined;
        const targetAnchor = isValidAnchor(meta.targetAnchor) ? meta.targetAnchor : undefined;

        return { routingPoints, sourceAnchor, targetAnchor };
    }
}
