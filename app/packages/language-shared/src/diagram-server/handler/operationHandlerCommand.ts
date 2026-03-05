import type { Command, MaybePromise } from "@eclipse-glsp/server";
import type { EdgeMetadata, GraphMetadata, NodeMetadata } from "../metadata.js";
import type { ModelState } from "../modelState.js";
import type { WorkspaceEdit } from "vscode-languageserver-types";

/**
 * Metadata edits to be applied to the model state.
 */
export interface MetadataEdits {
    nodes?: Record<string, Partial<NodeMetadata> | null>;
    edges?: Record<string, Partial<EdgeMetadata> | null>;
}

/**
 * Command to handle operations that modify the model state,
 * specifically applying text edits and metadata edits.
 */
export class OperationHandlerCommand implements Command {
    /**
     * Creates a new OperationHandlerCommand
     *
     * @param workspaceEdit the text edits to apply
     * @param metadataEdits the metadata edits to apply
     */
    constructor(
        readonly state: ModelState,
        readonly workspaceEdit: WorkspaceEdit | undefined,
        readonly metadataEdits: MetadataEdits | undefined
    ) {}

    async execute(): Promise<void> {
        if (this.metadataEdits != undefined) {
            const currentMetadata = this.state.metadata;
            const newMetadata: GraphMetadata = {
                nodes: this.mergeMetadata(currentMetadata.nodes, this.metadataEdits.nodes),
                edges: this.mergeMetadata(currentMetadata.edges, this.metadataEdits.edges)
            };
            this.state.metadata = newMetadata;
        }
        if (this.workspaceEdit != undefined) {
            await this.state.languageServices.shared.lsp.Connection!.workspace.applyEdit(this.workspaceEdit);
        }
    }

    /**
     * Merges metadata edits into the current metadata.
     * Each edit is deeply merged with the existing metadata entry for that key.
     *
     * @param current The current metadata object
     * @param edits The partial metadata edits to apply
     * @template T The type of the metadata object
     * @returns A new metadata object with the edits merged in
     */
    private mergeMetadata<T extends EdgeMetadata | NodeMetadata>(
        current: Record<string, T>,
        edits: Record<string, Partial<T> | null> | undefined
    ): Record<string, T> {
        if (edits == undefined) {
            return current;
        }

        const result = { ...current };

        for (const [key, value] of Object.entries(edits)) {
            if (value === null) {
                delete result[key];
            } else {
                const currentEntry = current[key] ?? {};
                let meta: Record<string, any> | undefined;
                if (currentEntry.meta != undefined) {
                    meta = {
                        ...currentEntry.meta
                    };
                }
                if (value.meta != undefined) {
                    if (meta == undefined) {
                        meta = {
                            ...value.meta
                        };
                    } else {
                        for (const [metaKey, metaValue] of Object.entries(value.meta)) {
                            meta[metaKey] = metaValue;
                        }
                    }
                }
                result[key] = {
                    ...currentEntry,
                    ...value,
                    meta
                };
            }
        }
        return result;
    }

    undo(): MaybePromise<void> {
        throw new Error("Method not implemented.");
    }

    redo(): MaybePromise<void> {
        throw new Error("Method not implemented.");
    }

    canUndo(): boolean {
        return false;
    }
}
