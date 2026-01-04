import { sharedImport } from "../sharedImport.js";
import type { GraphMetadata } from "./metadata.js";

const { injectable } = sharedImport("inversify");
const { DefaultModelState } = sharedImport("@eclipse-glsp/server");

@injectable()
export class ModelState extends DefaultModelState {
    protected _metadata: GraphMetadata = {
        nodes: {},
        edges: {}
    };

    get metadata(): GraphMetadata {
        return this._metadata;
    }

    set metadata(value: GraphMetadata) {
        this._metadata = value;
    }
}
