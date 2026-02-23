import type { Fadeable, FluentIterable, Hoverable, Point, Selectable } from "@eclipse-glsp/sprotty";
import { sharedImport } from "../sharedImport.js";
import type { NodeLayoutMetadata } from "@mdeo/editor-protocol";
import { GEdge } from "./edge.js";
import type { Connectable } from "../features/edge-routing/connectable.js";

const { GShapeElement } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Base client-side model for node elements.
 * Extends the GLSP node implementation to provide a foundation for custom nodes.
 * Can be used for both SVG and HTML-based nodes.
 */
export class GNode extends GShapeElement implements Selectable, Fadeable, Hoverable, Connectable {
    /**
     * Metadata associated with the node for layout and other information.
     */
    meta!: NodeLayoutMetadata;

    /**
     * Indicates whether the node is currently selected.
     */
    selected: boolean = false;
    /**
     * Indicates the opacity level of the node for fade effects.
     */
    opacity: number = 1;
    /**
     * Indicates whether the node is currently being hovered over.
     */
    hoverFeedback: boolean = false;

    /**
     * Indicates whether the node is currently a reconnect target.
     */
    isReconnectTarget: boolean = false;

    /**
     * Vertical alignment of the node. Defaults to "top".
     */
    vAlign: "top" | "center" | "bottom" = "top";

    /**
     * Horizontal alignment of the node. Defaults to "left".
     */
    hAlign: "left" | "center" | "right" = "left";

    /**
     * Creates a new GNode instance.
     */
    constructor() {
        super();
        // @ts-expect-error not optional, but will soon be redefined
        delete this.position;
        Object.defineProperty(this, "position", {
            get: () => {
                return this.meta.position ?? { x: 0, y: 0 };
            },
            set: (value: Point) => {
                if (this.meta) {
                    this.meta.position = value;
                }
            },
            enumerable: true,
            configurable: true
        });
    }

    /**
     * Determines if a connection can be made to this node with the given edge and role.
     *
     * @param _edge the edge to connect
     * @param _role the role of the connection ('source' or 'target')
     * @returns true if the connection is allowed, false otherwise
     */
    canConnect(_edge: GEdge, _role: "source" | "target"): boolean {
        return true;
    }

    /**
     * Retrieves all incoming edges connected to this node.
     *
     * @returns An iterable of incoming GEdge instances.
     */
    incomingEdges(): FluentIterable<GEdge> {
        const allEdges = this.index.all().filter((e) => e instanceof GEdge) as FluentIterable<GEdge>;
        return allEdges.filter((e) => e.targetId === this.id);
    }

    /**
     * Retrieves all outgoing edges connected to this node.
     *
     * @returns An iterable of outgoing GEdge instances.
     */
    outgoingEdges(): FluentIterable<GEdge> {
        const allEdges = this.index.all().filter((e) => e instanceof GEdge) as FluentIterable<GEdge>;
        return allEdges.filter((e) => e.sourceId === this.id);
    }
}
