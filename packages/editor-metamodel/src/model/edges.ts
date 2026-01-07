import { sharedImport } from "@mdeo/editor-shared";

const { SEdgeImpl } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Client-side model for inheritance edges (extends relationships).
 */
export class InheritanceEdge extends SEdgeImpl {}

/**
 * Client-side model for association edges.
 */
export class AssociationEdge extends SEdgeImpl {
    operator?: string;
}
