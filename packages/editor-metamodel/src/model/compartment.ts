import { sharedImport } from "@mdeo/editor-shared";

const {
    GCompartment: SCompartmentImpl,
    boundsFeature,
    layoutContainerFeature,
    layoutableChildFeature
} = sharedImport("@eclipse-glsp/sprotty");

/**
 * Client-side model for compartments within a Class node.
 */
export class ClassCompartment extends SCompartmentImpl {
    static override readonly DEFAULT_FEATURES = [boundsFeature, layoutContainerFeature, layoutableChildFeature];
}
