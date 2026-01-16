import type { Point } from "@eclipse-glsp/protocol";
import type {
    NodePositionMetadata,
    NodeLayoutMetadata,
    EdgePlacementMetadata,
    EdgeVisualMetadata
} from "@mdeo/editor-protocol";

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
        return (
            (typeof meta.prefWidth === "number") &&
            (meta.prefHeight === undefined || typeof meta.prefHeight === "number")
        );
    }

    /**
     * Creates a default NodeLayoutMetadata with the given position.
     *
     * @param x The x coordinate
     * @param y The y coordinate
     * @param preferredWidth Optional preferred width
     * @returns A new NodeLayoutMetadata object
     */
    export function create(x: number, y: number, preferredWidth?: number): NodeLayoutMetadata {
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
    export function verify(obj: unknown, defaultPrefWidth: number): NodeLayoutMetadata | undefined {
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
 * Namespace providing helper methods for edge metadata validation and manipulation.
 */
export namespace EdgeVisualMetadataUtil {
    /**
     * Checks if the given object is a valid EdgeVisualMetadata.
     *
     * @param obj The object to validate
     * @returns True if the object is a valid EdgeVisualMetadata
     */
    export function isValid(obj: unknown): obj is EdgeVisualMetadata {
        if (typeof obj !== "object" || obj === null) {
            return false;
        }

        const meta = obj as Partial<EdgeVisualMetadata>;

        if (!Array.isArray(meta.routingPoints)) {
            return false;
        }

        return meta.routingPoints.every((point) => isValidPoint(point));
    }

    /**
     * Creates a default EdgeVisualMetadata with the given routing points.
     *
     * @param routingPoints Array of routing points
     * @returns A new EdgeVisualMetadata object
     */
    export function create(routingPoints: Point[] = []): EdgeVisualMetadata {
        return { routingPoints };
    }

    /**
     * Verifies and corrects invalid metadata.
     *
     * @param obj The object to verify
     * @returns Corrected metadata if invalid, undefined if valid
     */
    export function verify(obj: unknown): EdgeVisualMetadata | undefined {
        if (isValid(obj)) {
            return undefined;
        }

        const meta = (typeof obj === "object" && obj !== null ? obj : {}) as Partial<EdgeVisualMetadata>;
        const routingPoints = Array.isArray(meta.routingPoints)
            ? meta.routingPoints.filter((point) => isValidPoint(point))
            : [];

        return { routingPoints };
    }
}

/**
 * Namespace providing helper methods for edge placement metadata.
 */
export namespace EdgePlacementMetadataUtil {
    /**
     * Checks if the given object is a valid EdgePlacementMetadata.
     *
     * @param obj The object to validate
     * @returns True if the object is a valid EdgePlacementMetadata
     */
    export function isValid(obj: unknown): obj is EdgePlacementMetadata {
        if (typeof obj !== "object" || obj === null) {
            return false;
        }

        const meta = obj as Partial<EdgePlacementMetadata>;

        return (
            typeof meta.position === "number" &&
            meta.position >= 0 &&
            meta.position <= 1 &&
            (meta.side === undefined || ["left", "right", "top", "bottom"].includes(meta.side)) &&
            (meta.offset === undefined || typeof meta.offset === "number")
        );
    }

    /**
     * Creates a default EdgePlacementMetadata.
     *
     * @param position Position along the edge (0.0 to 1.0)
     * @param side Optional side of the edge
     * @param offset Optional offset in pixels
     * @returns A new EdgePlacementMetadata object
     */
    export function create(
        position: number = 0.5,
        side?: "left" | "right" | "top" | "bottom",
        offset?: number
    ): EdgePlacementMetadata {
        const metadata: EdgePlacementMetadata = { position };
        if (side !== undefined) {
            metadata.side = side;
        }
        if (offset !== undefined) {
            metadata.offset = offset;
        }
        return metadata;
    }

    /**
     * Verifies and corrects invalid metadata.
     *
     * @param obj The object to verify
     * @param defaultPosition Default position if invalid
     * @returns Corrected metadata if invalid, undefined if valid
     */
    export function verify(obj: unknown, defaultPosition: number = 0.5): EdgePlacementMetadata | undefined {
        if (isValid(obj)) {
            return undefined;
        }

        const meta = (typeof obj === "object" && obj !== null ? obj : {}) as Partial<EdgePlacementMetadata>;
        const position =
            typeof meta.position === "number" && meta.position >= 0 && meta.position <= 1
                ? meta.position
                : defaultPosition;
        const side = meta.side && ["left", "right", "top", "bottom"].includes(meta.side) ? meta.side : undefined;
        const offset = typeof meta.offset === "number" ? meta.offset : undefined;

        const result: EdgePlacementMetadata = { position };
        if (side !== undefined) {
            result.side = side;
        }
        if (offset !== undefined) {
            result.offset = offset;
        }
        return result;
    }
}
