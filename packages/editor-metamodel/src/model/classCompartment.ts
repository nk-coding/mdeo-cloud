import { GCompartment } from "@mdeo/editor-shared";
import { sharedImport } from "@mdeo/editor-shared";

const { boundsFeature, layoutContainerFeature, layoutableChildFeature } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Client-side model for compartments within a Class node.
 */
export class GClassCompartment extends GCompartment {
    static override readonly DEFAULT_FEATURES = [boundsFeature, layoutContainerFeature, layoutableChildFeature];
}
