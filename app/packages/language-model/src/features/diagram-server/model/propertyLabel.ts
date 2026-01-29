import { GLabel, GLabelBuilder } from "@mdeo/language-shared";
import { ModelElementType } from "./elementTypes.js";

/**
 * Label for property assignment display.
 * Shows property name and its assigned value.
 */
export class GPropertyLabel extends GLabel {
    /**
     * Creates a builder for GPropertyLabel instances.
     *
     * @returns A new GPropertyLabelBuilder
     */
    static builder(): GPropertyLabelBuilder {
        return new GPropertyLabelBuilder(GPropertyLabel).type(ModelElementType.LABEL_PROPERTY);
    }
}

/**
 * Builder for GPropertyLabel instances.
 * Provides fluent API for constructing property assignment labels.
 */
export class GPropertyLabelBuilder<T extends GPropertyLabel = GPropertyLabel> extends GLabelBuilder<T> {}
