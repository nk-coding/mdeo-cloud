import type { RenderingContext } from "@eclipse-glsp/sprotty";
import { sharedImport, GEdgeView, type EdgeMarkerData } from "@mdeo/editor-shared";
import type { GInheritanceEdge } from "../model/inheritanceEdge.js";

const { injectable } = sharedImport("inversify");
const { svg } = sharedImport("@eclipse-glsp/sprotty");

/**
 * View for rendering inheritance edges.
 * Renders edges as polylines with an unfilled triangle marker at the target (superclass).
 */
@injectable()
export class GInheritanceEdgeView extends GEdgeView {
    protected override renderTargetMarker(
        _model: Readonly<GInheritanceEdge>,
        _context: RenderingContext
    ): EdgeMarkerData | undefined {
        const triangle = svg("polygon", {
            class: {
                "fill-background": true,
                "stroke-foreground": true,
                "stroke-[1.5px]": true
            },
            attrs: {
                points: "0,0 -15,-7.5 -15,7.5"
            }
        });

        return {
            marker: triangle,
            strokeOffset: 15,
            elementOffset: 15
        };
    }
}
