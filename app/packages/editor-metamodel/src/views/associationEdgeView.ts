import { sharedImport, GEdgeView } from "@mdeo/editor-shared";

const { injectable } = sharedImport("inversify");

/**
 * View for rendering association edges.
 * Renders edges as polylines with association styling.
 */
@injectable()
export class GAssociationEdgeView extends GEdgeView {}
