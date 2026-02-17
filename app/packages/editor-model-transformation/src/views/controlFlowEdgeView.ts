import type { RenderingContext } from "@eclipse-glsp/sprotty";
import { sharedImport, GEdgeView, type EdgeMarkerData, type GEdge } from "@mdeo/editor-shared";

const { injectable } = sharedImport("inversify");
const { svg } = sharedImport("@eclipse-glsp/sprotty");

/**
 * View for rendering control flow edges.
 * Renders edges with an arrow marker at the target end.
 */
@injectable()
export class GControlFlowEdgeView extends GEdgeView {
    /**
     * Renders the target marker as an arrow indicating the flow direction.
     *
     * @param _model The control flow edge model
     * @param _context The rendering context
     * @returns Edge marker data with arrow shape
     */
    protected override renderTargetMarker(_model: Readonly<GEdge>, _context: RenderingContext): EdgeMarkerData {
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
