import type { Point } from "@eclipse-glsp/protocol";

/**
 * Helper function to validate if an object is a valid Point.
 *
 * @param obj - The object to validate
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
 * Base metadata interface for node position.
 */
export interface NodePositionMetadata {
    /**
     * The position of the node in the diagram.
     */
    position?: Point;
}

/**
 * Namespace providing helper methods for node position metadata validation and manipulation.
 */
export namespace NodePositionMetadata {
    /**
     * Checks if the given object is a valid NodePositionMetadata.
     *
     * @param obj - The object to validate
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
     * @param x - The x coordinate
     * @param y - The y coordinate
     * @returns A new NodePositionMetadata object
     */
    export function create(x: number, y: number): NodePositionMetadata {
        return { position: { x, y } };
    }

    /**
     * Verifies and corrects invalid metadata.
     *
     * @param obj - The object to verify
     * @param defaultX - Default x coordinate if position is invalid
     * @param defaultY - Default y coordinate if position is invalid
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
 * Extended metadata interface for node layout with sizing.
 */
export interface NodeLayoutMetadata extends NodePositionMetadata {
    /**
     * The preferred width of the node.
     * If not specified, the node will be sized automatically.
     */
    prefWidth?: number;
    /**
     * The preferred height of the node.
     * If not specified, the node will be sized autonatically
     */
    prefHeight?: number;
}

/**
 * Namespace providing helper methods for node layout metadata validation and manipulation.
 */
export namespace NodeLayoutMetadata {
    /**
     * Checks if the given object is a valid NodeLayoutMetadata.
     *
     * @param obj - The object to validate
     * @returns True if the object is a valid NodeLayoutMetadata
     */
    export function isValid(obj: unknown): obj is NodeLayoutMetadata {
        if (!NodePositionMetadata.isValid(obj)) {
            return false;
        }

        const meta = obj as Partial<NodeLayoutMetadata>;
        return (
            (meta.prefWidth === undefined || typeof meta.prefWidth === "number") &&
            (meta.prefHeight === undefined || typeof meta.prefHeight === "number")
        );
    }

    /**
     * Creates a default NodeLayoutMetadata with the given position.
     *
     * @param x - The x coordinate
     * @param y - The y coordinate
     * @param preferredWidth - Optional preferred width
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
     * @param obj - The object to verify
     * @param defaultX - Default x coordinate if position is invalid
     * @param defaultY - Default y coordinate if position is invalid
     * @returns Corrected metadata if invalid, undefined if valid
     */
    export function verify(obj: unknown, defaultX: number = 0, defaultY: number = 0): NodeLayoutMetadata | undefined {
        if (isValid(obj)) {
            return undefined;
        }

        const meta = (typeof obj === "object" && obj !== null ? obj : {}) as Partial<NodeLayoutMetadata>;
        const position = meta.position && isValidPoint(meta.position) ? meta.position : { x: defaultX, y: defaultY };
        const preferredWidth = typeof meta.prefWidth === "number" ? meta.prefWidth : undefined;

        const result: NodeLayoutMetadata = { position };
        if (preferredWidth !== undefined) {
            result.prefWidth = preferredWidth;
        }
        return result;
    }
}

/**
 * Metadata interface for edge visual properties.
 * Contains routing information for graph edges.
 */
export interface EdgeVisualMetadata {
    /**
     * The routing points for the edge.
     * These define the path the edge takes from source to target.
     */
    routingPoints: Point[];
}

/**
 * Namespace providing helper methods for edge metadata validation and manipulation.
 */
export namespace EdgeVisualMetadata {
    /**
     * Checks if the given object is a valid EdgeVisualMetadata.
     *
     * @param obj - The object to validate
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
     * @param routingPoints - Array of routing points
     * @returns A new EdgeVisualMetadata object
     */
    export function create(routingPoints: Point[] = []): EdgeVisualMetadata {
        return { routingPoints };
    }

    /**
     * Verifies and corrects invalid metadata.
     *
     * @param obj - The object to verify
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
 * Metadata interface for edge placement properties.
 * Defines positioning along an edge for elements like labels or decorations.
 */
export interface EdgePlacementMetadata {
    /**
     * The position along the edge (0.0 = start, 1.0 = end).
     */
    position: number;

    /**
     * The side of the edge the element appears on.
     */
    side?: "left" | "right" | "top" | "bottom";

    /**
     * The offset from the edge in pixels.
     */
    offset?: number;
}

/**
 * Namespace providing helper methods for edge placement metadata.
 */
export namespace EdgePlacementMetadata {
    /**
     * Checks if the given object is a valid EdgePlacementMetadata.
     *
     * @param obj - The object to validate
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
     * @param position - Position along the edge (0.0 to 1.0)
     * @param side - Optional side of the edge
     * @param offset - Optional offset in pixels
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
     * @param obj - The object to verify
     * @param defaultPosition - Default position if invalid
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
