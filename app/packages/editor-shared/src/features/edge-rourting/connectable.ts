import type { GModelElement } from "@eclipse-glsp/sprotty";
import type { GEdge } from "../../model/edge.js";
import { sharedImport } from "../../sharedImport.js";

const { connectableFeature } = sharedImport("@eclipse-glsp/client");

/**
 * Custom feature extension for the connectable feature, using GEdge instead of SRoutableElementImpl.
 */
export interface Connectable {
    canConnect(edge: GEdge, role: "source" | "target"): boolean;
}

/**
 * Type guard to check if an element is connectable.
 *
 * @param element The model element to check
 * @returns True if the element is connectable, false otherwise
 */
export function isConnectable<T extends GModelElement>(element: T): element is Connectable & T {
    return (
        element.hasFeature(connectableFeature) &&
        "canConnect" in element &&
        typeof (element as any).canConnect === "function"
    );
}
