import type { Command, MaybePromise } from "@eclipse-glsp/server";
import type { EdgeMetadata, GraphMetadata, NodeMetadata } from "../metadata.js";
import type { ModelState } from "../modelState.js";
import { sharedImport } from "../../sharedImport.js";

const { URI } = sharedImport("langium");

/**
 * Command to handle operations that modify the model state,
 * specifically applying text edits and metadata edits.
 */
export class OperationHandlerCommand implements Command {
    /**
     * Creates a new OperationHandlerCommand
     *
     * @param textEdits the text edits to apply
     * @param metadataEdits the metadata edits to apply
     */
    constructor(
        readonly state: ModelState,
        readonly textEdits: undefined,
        readonly metadataEdits:
            | {
                  nodes?: Record<string, Partial<NodeMetadata>>;
                  edges?: Record<string, Partial<EdgeMetadata>>;
              }
            | undefined
    ) {}

    async execute(): Promise<void> {
        if (this.metadataEdits != undefined) {
            const currentMetadata = this.state.metadata;
            const newMetadata: GraphMetadata = {
                nodes: this.mergeMetadata(currentMetadata.nodes, this.metadataEdits.nodes),
                edges: this.mergeMetadata(currentMetadata.edges, this.metadataEdits.edges)
            };
            this.state.languageServices.shared.workspace.FileSystemProvider.writeMetadata(
                URI.parse(this.state.sourceUri!),
                newMetadata
            );
            this.state.metadata = newMetadata;
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
        edits: Record<string, Partial<T>> | undefined
    ): Record<string, T> {
        if (edits == undefined) {
            return current;
        }

        return {
            ...current,
            ...Object.fromEntries(
                Object.entries(edits).map(([key, value]) => {
                    const currentEntry = current[key] ?? {};
                    const meta =
                        currentEntry.meta != undefined
                            ? {
                                  ...currentEntry.meta,
                                  ...value?.meta
                              }
                            : value?.meta;
                    return [
                        key,
                        {
                            ...currentEntry,
                            ...value,
                            meta
                        }
                    ];
                })
            )
        };
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
