import { sharedImport } from "../sharedImport.js";

const { GGraph: GLSPGraph } = sharedImport("@eclipse-glsp/client");

/**
 * Custom graph element for the editor, extending the default GGraph.
 * Sets zoom to 0 to disable rendering initially
 */
export class GGraph extends GLSPGraph {
    constructor() {
        super();
        this.zoom = 0;
    }
}
