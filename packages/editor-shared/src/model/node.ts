import type { Point } from "@eclipse-glsp/sprotty";
import { sharedImport } from "../sharedImport.js";
import type { NodeLayoutMetadata } from "@mdeo/language-shared";

const { GNode: SNodeImpl } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Base client-side model for node elements.
 * Extends the GLSP node implementation to provide a foundation for custom nodes.
 */
export class GNode extends SNodeImpl {
    meta?: NodeLayoutMetadata;
}
