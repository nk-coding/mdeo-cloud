import type { Bounds, Point } from "@eclipse-glsp/sprotty";
import { sharedImport } from "../sharedImport.js";
import type { NodeLayoutMetadata } from "@mdeo/editor-protocol";

const { GNode: SNodeImpl } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Base client-side model for node elements.
 * Extends the GLSP node implementation to provide a foundation for custom nodes.
 */
export class GNode extends SNodeImpl {
    meta!: NodeLayoutMetadata;

    constructor() {
        super();
        // @ts-expect-error not optional, but will soon be redefined
        delete this.position;
        Object.defineProperty(this, "position", {
            get: () => {
                return this.meta.position ?? { x: 0, y: 0 };
            },
            set: (value: Point) => {
                if (this.meta) {
                    this.meta.position = value;
                }
            },
            enumerable: true,
            configurable: true
        });
    }
}
