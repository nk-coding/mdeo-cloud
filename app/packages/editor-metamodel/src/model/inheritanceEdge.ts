import { sharedImport } from "@mdeo/editor-shared";

const { SEdgeImpl } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Client-side model for inheritance edges (extends relationships).
 */
export class GInheritanceEdge extends SEdgeImpl {}
