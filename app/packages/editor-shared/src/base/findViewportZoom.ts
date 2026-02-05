import type { GModelElement } from "@eclipse-glsp/sprotty";
import { sharedImport } from "../sharedImport.js";

const { isViewport } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Finds the zoom of the current viewport by searching for a viewport in the parents of element
 *
 * @param element the element which is a child of a viewport
 * @returns the found zoom or 1 if no viewport was found
 */
export function findViewportZoom(element: Readonly<GModelElement>): number {
    const viewport = element.root;
    if (isViewport(viewport)) {
        return viewport.zoom || 1;
    }
    return 1;
}
