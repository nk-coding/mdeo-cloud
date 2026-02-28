import { GCompartment } from "@mdeo/editor-shared";

/**
 * Client-side model for the pattern modifier title compartment.
 * Displays a stereotype label («create», «delete», «forbid», «require») above the instance name.
 * The modifier kind is resolved at render time by traversing up to the parent
 * GPatternInstanceNode, so it does not need to be stored here.
 */
export class GPatternModifierTitleCompartment extends GCompartment {}
