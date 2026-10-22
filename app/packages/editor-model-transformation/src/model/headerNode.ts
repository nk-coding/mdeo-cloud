import { GLabel, GRectangularNode, nodeLayoutMetadataFeature, sharedImport } from "@mdeo/editor-shared";

const { boundsFeature, selectFeature, moveFeature, fadeFeature, deletableFeature, editLabelFeature } =
    sharedImport("@eclipse-glsp/sprotty");

/**
 * Client-side model for the header box of a transformation, drawn above its start node: the
 * transformation's file name, its path imports and its `in` / `out` declarations. The box
 * itself cannot be deleted; its rows can.
 */
export class GHeaderNode extends GRectangularNode {
    static readonly DEFAULT_FEATURES = [
        boundsFeature,
        selectFeature,
        moveFeature,
        fadeFeature,
        nodeLayoutMetadataFeature
    ];
}

/**
 * Client-side model for the title of the header box.
 */
export class GHeaderTitleLabel extends GLabel {}

/**
 * Client-side model for a row of the header box: an import or a declaration as written. A row
 * can be selected and deleted (which deletes its line) and edited (which replaces its line).
 */
export class GHeaderRowLabel extends GLabel {
    static override readonly DEFAULT_FEATURES = [editLabelFeature, selectFeature, deletableFeature];

    /**
     * Indicates whether the row is selected.
     */
    selected: boolean = false;
}
