import { sharedImport } from "../sharedImport.js";
import type { GraphMetadata } from "./metadata.js";
import type { AstNode } from "langium";
import { MetadataManager } from "./metadataManager.js";

const { injectable, inject, optional } = sharedImport("inversify");
const { DefaultModelState } = sharedImport("@eclipse-glsp/server");

/**
 * Extended model state that manages both the graphical model and the source model.
 * The source model represents the domain-specific AST, while the graphical model
 * is the GLSP representation for rendering.
 *
 * @template T The type of the source model, must extend AstNode
 */
@injectable()
export class ModelState<T extends AstNode = AstNode> extends DefaultModelState {
    @inject(MetadataManager)
    @optional()
    protected metadataManager?: MetadataManager<T>;

    /**
     * Metadata for graph elements including visual properties like positions and routing points.
     */
    protected _metadata: GraphMetadata = {
        nodes: {},
        edges: {}
    };

    /**
     * The source model representing the domain-specific AST.
     */
    protected _sourceModel: T | undefined;

    /**
     * Gets the current graph metadata.
     */
    get metadata(): GraphMetadata {
        return this._metadata;
    }

    /**
     * Sets the graph metadata.
     */
    set metadata(value: GraphMetadata) {
        this._metadata = value;
    }

    /**
     * Gets the current source model.
     */
    get sourceModel(): T | undefined {
        return this._sourceModel;
    }

    /**
     * Asynchronously updates the source model.
     * This method can be overridden to perform additional operations
     * when the source model changes, such as validation or notifications.
     *
     * @param sourceModel The new source model to set
     */
    async updateSourceModel(sourceModel: T | undefined): Promise<void> {
        this._sourceModel = sourceModel;
    }

    /**
     * Gets validated metadata for the current source model.
     * If a metadata manager is available and a source model exists,
     * validates the current metadata against the source model.
     * Returns the validated metadata, or the current metadata if validation
     * indicates no changes are needed.
     *
     * @returns The validated graph metadata
     */
    getValidatedMetadata(): GraphMetadata {
        if (this.metadataManager && this._sourceModel) {
            const validatedMetadata = this.metadataManager.validateMetadata(this._sourceModel, this._metadata);
            if (validatedMetadata) {
                this._metadata = validatedMetadata;
            }
        }
        return this._metadata;
    }
}
