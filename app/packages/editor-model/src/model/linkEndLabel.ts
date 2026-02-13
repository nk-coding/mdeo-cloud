import { GLabel, sharedImport } from "@mdeo/editor-shared";

const { editLabelFeature } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Client-side model for link end labels (source or target).
 * Displays the optional property specification at the link end.
 */
export class GLinkEndLabel extends GLabel {
    static override readonly DEFAULT_FEATURES = [editLabelFeature];
}
