import type { Point } from "@eclipse-glsp/sprotty";
import { sharedImport } from "../sharedImport.js";
import type { GNode } from "../model/node.js";
import { findViewportZoom } from "../base/findViewportZoom.js";
import type { VNode } from "snabbdom";

const { injectable } = sharedImport("inversify");
const { svg, ShapeView } = sharedImport("@eclipse-glsp/sprotty");

@injectable()
export abstract class GNodeView extends ShapeView {
    /**
     * The offset of the selection rectangle
     */
    static readonly SELECTION_OFFSET = 6;

    /**
     * The size of the inner point
     */
    static readonly INNER_POINT_SIZE = 8;

    /**
     * Generates a rectangle that is displayed if the element is selected.
     * Does NOT check if the element is selected.
     *
     * @param model The model of the element
     * @returns The rectangle and its children
     */
    protected renderSelectedRect(model: Readonly<GNode>): VNode[] {
        if (!model.selected) {
            return [];
        }
        const zoom = findViewportZoom(model);
        const result: VNode[] = [
            svg("rect", {
                class: {
                    "stroke-primary": true,
                    "stroke-1": true,
                    "fill-none": true,
                    "non-scaling-stroke": true
                },
                attrs: {
                    ...this.generatePoint(model, zoom, 0),
                    width: model.bounds.width + (GNodeView.SELECTION_OFFSET * 2) / zoom,
                    height: model.bounds.height + (GNodeView.SELECTION_OFFSET * 2) / zoom
                }
            }),
            ...[0, 1, 2, 3].map((pos) => this.renderSelectedOutlineCorner(model, zoom, pos))
        ];
        return result;
    }

    /**
     * Generates a corner of the selection rectangle.
     * Does not check if the element is selected.
     *
     * @param model The model of the element
     * @param zoom The zoom level of the diagram
     * @param pos The position of the corner
     * @returns The corner
     */
    private renderSelectedOutlineCorner(model: Readonly<GNode>, zoom: number, pos: number): VNode {
        const point = this.generatePoint(model, zoom, pos);
        return svg("rect", {
            class: {
                "stroke-primary": true,
                "stroke-1": true,
                "fill-background": true,
                "non-scaling-stroke": true
            },
            attrs: {
                x: point.x - GNodeView.INNER_POINT_SIZE / 2 / zoom,
                y: point.y - GNodeView.INNER_POINT_SIZE / 2 / zoom,
                width: GNodeView.INNER_POINT_SIZE / zoom,
                height: GNodeView.INNER_POINT_SIZE / zoom,
                rx: GNodeView.INNER_POINT_SIZE / 4 / zoom
            }
        });
    }

    /**
     * Computes the position of a point on the selection rectangle.
     * 0 is the top left corner, 1 is the top right corner, 2 is the bottom right corner and 3 is the bottom left corner.
     *
     * @param model the model for which the point should be computed
     * @param zoom the zoom level of the diagram
     * @param pos the position of the point, taken modulo 4
     * @returns the point
     */
    private generatePoint(model: Readonly<GNode>, zoom: number, pos: number): Point {
        pos = pos % 4;
        const scaledOffset = GNodeView.SELECTION_OFFSET / zoom;
        const x = pos === 1 || pos === 2 ? model.bounds.width + scaledOffset : -scaledOffset;
        const y = pos < 2 ? -scaledOffset : model.bounds.height + scaledOffset;
        return { x, y };
    }
}
