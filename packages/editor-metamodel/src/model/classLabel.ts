import { sharedImport } from "@mdeo/editor-shared";

const { GLabel } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Client-side model for the Class name label.
 */
export class GClassLabel extends GLabel {}
