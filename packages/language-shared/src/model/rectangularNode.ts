import { GNode, GNodeBuilder } from "./node.js";

/**
 * A rectangular node with layout container capabilities.
 */
export class GRectangularNode extends GNode {}

export class GRectangularNodeBuilder<T extends GRectangularNode = GRectangularNode> extends GNodeBuilder<T> {}
