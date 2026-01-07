import { sharedImport } from "../sharedImport.js";

const { GNode, GNodeBuilder } = sharedImport("@eclipse-glsp/server");

/**
 * A rectangular node with layout container capabilities.
 */
export class GRectangularNode extends GNode {
    override layout = "vbox";

    override layoutOptions = {
        paddingTop: 2,
        paddingLeft: 2,
        paddingBottom: 2,
        paddingRight: 2
    };
}

export class GRectangularNodeBuilder<T extends GRectangularNode = GRectangularNode> extends GNodeBuilder<T> {}
