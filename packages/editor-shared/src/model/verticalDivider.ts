import type { ModelLayoutOptions } from "@eclipse-glsp/sprotty";
import { sharedImport } from "../sharedImport.js";

const { GShapeElement, layoutableChildFeature, boundsFeature } = sharedImport("@eclipse-glsp/sprotty");

export class GVerticalDivider extends GShapeElement {
    static readonly DEFAULT_FEATURES = [layoutableChildFeature, boundsFeature];

    override layoutOptions: ModelLayoutOptions = {
        vGrab: true,
        hGrab: false
    };
}
