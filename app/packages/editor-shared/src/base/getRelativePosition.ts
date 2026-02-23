import type { GModelElement, GModelRoot, Point, Viewport } from "@eclipse-glsp/sprotty";
import { sharedImport } from "../sharedImport.js";

const { translatePoint, isViewport, findParentByFeature, Point: PointUtil } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Returns the position corresponding to this mouse event in the target's coordinate system.
 *
 * @param target The model element to which the position should be relative.
 * @param mouseEvent The mouse event containing the page coordinates.
 * @returns The position of the mouse event relative to the target's coordinate system.
 */
export function getRelativePosition(target: GModelElement, mouseEvent: MouseEvent): Point {
    let position = PointUtil.subtract(
        {
            x: mouseEvent.pageX,
            y: mouseEvent.pageY
        },
        target.root.canvasBounds
    );

    const viewport: (Viewport & GModelRoot) | undefined = findParentByFeature(target, isViewport);
    if (viewport) {
        const zoom = viewport.zoom;
        const zoomedScroll = PointUtil.multiplyScalar(viewport.scroll, zoom);
        position = PointUtil.add(position, zoomedScroll);
        position = PointUtil.divideScalar(position, zoom);
        const newPos = translatePoint(position, viewport, target);
        position = newPos;
    }

    return position;
}
