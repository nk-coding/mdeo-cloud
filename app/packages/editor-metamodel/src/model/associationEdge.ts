import { sharedImport } from "@mdeo/editor-shared";

const { SEdgeImpl } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Client-side model for association edges.
 */
export class GAssociationEdge extends SEdgeImpl {
    operator?: string;
}
