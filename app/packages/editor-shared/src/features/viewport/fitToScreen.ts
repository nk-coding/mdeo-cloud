import type { Action, Bounds, Viewport } from "@eclipse-glsp/protocol";
import { sharedImport } from "../../sharedImport.js";
import type { GModelRoot } from "@eclipse-glsp/sprotty";

const { FitToScreenAction } = sharedImport("@eclipse-glsp/protocol");
const { FitToScreenCommand: SprottyFitToScreenCommand, limit } = sharedImport("@eclipse-glsp/sprotty");
const { Bounds: BoundsUtil } = sharedImport("@eclipse-glsp/protocol");
const { injectable } = sharedImport("inversify");

/**
 * Creates a new action to fit the viewport to the current diagram with common params.
 *
 * @param animate whether to animate the action
 */
export function createFitToScreenAction(animate: boolean | undefined = undefined, elementIds: string[]): Action {
    return FitToScreenAction.create(elementIds, { padding: 50, maxZoom: 2, animate });
}

/**
 * Custom FitToScreenCommand that sets a fallback viewport if necessary
 */
@injectable()
export class FitToScreenCommand extends SprottyFitToScreenCommand {
    protected override initialize(model: GModelRoot): void {
        super.initialize(model);
        if (this.newViewport == undefined) {
            this.newViewport = {
                scroll: { x: 0, y: 0 },
                zoom: 1
            };
        }
    }

    override getNewViewport(bounds: Bounds, model: GModelRoot): Viewport | undefined {
        const viewport = super.getNewViewport(bounds, model);
        if (viewport == undefined) {
            return undefined;
        }
        const center = BoundsUtil.center(bounds);
        const zoom = limit(viewport.zoom, this.viewerOptions.zoomLimits);
        return {
            scroll: {
                x: center.x - (0.5 * model.canvasBounds.width) / zoom,
                y: center.y - (0.5 * model.canvasBounds.height) / zoom
            },
            zoom
        };
    }
}
