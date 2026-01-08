import type { isViewport as sprottyIsViewport, GModelElement } from "@eclipse-glsp/sprotty";

/**
 * Finds the zoom of the current viewport by searching for a viewport in the parents of element
 *
 * @param element the element which is a child of a viewport
 * @param isViewport a function to determine if an element is a viewport
 * @returns the found zoom or 1 if no viewport was found
 */
export function findViewportZoom(element: Readonly<GModelElement>, isViewport: typeof sprottyIsViewport): number {
    const viewport = element.root;
    if (isViewport(viewport)) {
        return viewport.zoom;
    }
    return 1;
}