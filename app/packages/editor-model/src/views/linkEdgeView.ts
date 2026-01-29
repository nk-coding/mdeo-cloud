import type { RenderingContext } from "@eclipse-glsp/sprotty";
import { sharedImport, GEdgeView, type EdgeMarkerData } from "@mdeo/editor-shared";
import type { GLinkEdge } from "../model/linkEdge.js";

const { injectable } = sharedImport("inversify");
const { svg } = sharedImport("@eclipse-glsp/sprotty");

/**
 * View for rendering link edges between objects.
 * Renders edges as polylines with an arrow marker at the target end.
 */
@injectable()
export class GLinkEdgeView extends GEdgeView {
    /**
     * Renders the target marker as an arrow indicating the link direction.
     *
     * @param _model - The link edge model
     * @param _context - The rendering context
     * @returns Edge marker data with arrow shape
     */
    protected override renderTargetMarker(
        _model: Readonly<GLinkEdge>,
        _context: RenderingContext
    ): EdgeMarkerData | undefined {
        const arrow = svg("polygon", {
            class: {
                "fill-foreground": true,
                "stroke-foreground": true,
                "stroke-[1px]": true
            },
            attrs: {
                points: "0,0 -12,-6 -12,6"
            }
        });

        return {
            marker: arrow,
            strokeOffset: 12,
            elementOffset: 6
        };
    }
}
