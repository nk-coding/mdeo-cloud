import { sharedImport } from "../sharedImport.js";

const { injectable } = sharedImport("inversify");
const { GModelIndex: BaseGModelIndex } = sharedImport("@eclipse-glsp/server");

/**
 * Index for efficient lookup of graph model elements.
 * Extends the base GLSP GModelIndex implementation.
 */
@injectable()
export class GModelIndex extends BaseGModelIndex {}
