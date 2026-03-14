import { GLabel, GLabelBuilder } from "@mdeo/language-shared";
import { ModelElementType } from "@mdeo/protocol-model";

/**
 * Label for the object instance, displaying both name and type.
 * Format: "name : type"
 */
export class GObjectNameLabel extends GLabel {
    /**
     * Creates a builder for GObjectNameLabel instances.
     *
     * @returns A new GObjectNameLabelBuilder
     */
    static builder(): GObjectNameLabelBuilder {
        return new GObjectNameLabelBuilder(GObjectNameLabel).type(ModelElementType.LABEL_OBJECT_NAME);
    }
}

/**
 * Builder for GObjectNameLabel instances.
 * Provides fluent API for constructing object labels.
 */
export class GObjectNameLabelBuilder<T extends GObjectNameLabel = GObjectNameLabel> extends GLabelBuilder<T> {}
