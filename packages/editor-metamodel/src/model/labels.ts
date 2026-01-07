import { sharedImport } from "@mdeo/editor-shared";

const { GLabel } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Client-side model for the Class name label.
 */
export class ClassLabel extends GLabel {}

/**
 * Client-side model for property labels within a Class node.
 */
export class PropertyLabel extends GLabel {}

/**
 * Client-side model for association endpoint labels.
 */
export class AssociationEndLabel extends GLabel {}
