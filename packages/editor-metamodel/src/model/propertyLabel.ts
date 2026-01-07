import { sharedImport } from "@mdeo/editor-shared";

const { GLabel } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Client-side model for property labels within a Class node.
 */
export class GPropertyLabel extends GLabel {}
