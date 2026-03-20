import { GLabelView, sharedImport } from "@mdeo/editor-shared";

const { injectable } = sharedImport("inversify");

/**
 * Label view for association multiplicity labels.
 * Uses the standard label edit flow (ApplyLabelEditOperation) since
 * multiplicity labels are always present for navigable association ends.
 */
@injectable()
export class GAssociationMultiplicityLabelView extends GLabelView {}
