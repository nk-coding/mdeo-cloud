import { GCompartmentView, sharedImport } from "@mdeo/editor-shared";

const { injectable } = sharedImport("inversify");

/**
 * View for the pattern modifier title compartment.
 * Renders the compartment children (modifier label + instance name label).
 * The modifier colour is handled by GPatternModifierLabelView.
 */
@injectable()
export class GPatternModifierTitleCompartmentView extends GCompartmentView {}
