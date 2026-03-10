import { GLabel } from "@mdeo/editor-shared";

/**
 * Client-side model for a pattern link modifier label.
 * Displayed as a GLabel child of a GPatternLinkModifierNode, showing
 * the modifier keyword with guillemets (e.g. «create», «delete»).
 * Inherits text, editMode, readonly etc. from GLabel.
 */
export class GPatternLinkModifierLabel extends GLabel {}
