import { sharedImport } from "@mdeo/editor-shared";

const { GLabel } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Client-side model for association endpoint labels.
 */
export class GAssociationEndLabel extends GLabel {}
