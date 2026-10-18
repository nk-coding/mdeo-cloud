import { GNode, GNodeBuilder } from "@mdeo/language-shared";
import { ModelTransformationElementType } from "@mdeo/protocol-model-transformation";

/**
 * Server-side node for a call of another transformation (`call name(...) -> (...)`). It shows the
 * call's text; the callee's statements are not part of this diagram.
 */
export class GCallNode extends GNode {
    /**
     * The call as written, e.g. `call moveComponent(maxCandidates = 2) -> (moved: m)`.
     */
    text!: string;

    static builder(): GCallNodeBuilder {
        return new GCallNodeBuilder(GCallNode).type(ModelTransformationElementType.NODE_CALL);
    }
}

/**
 * Builder for {@link GCallNode}.
 */
export class GCallNodeBuilder<T extends GCallNode = GCallNode> extends GNodeBuilder<T> {
    text(text: string): this {
        this.proxy.text = text;
        return this;
    }
}
