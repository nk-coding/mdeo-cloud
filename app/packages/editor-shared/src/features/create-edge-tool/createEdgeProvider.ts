import type { IActionDispatcher } from "@eclipse-glsp/sprotty";
import type { GNode } from "../../model/node.js";
import { sharedImport } from "../../sharedImport.js";
import {
    RequestCreateEdgeInitialSchemaAction,
    RequestCreateEdgeTargetSchemaAction,
    type CreateEdgeSchema
} from "@mdeo/editor-protocol";

const { injectable, inject, optional } = sharedImport("inversify");
const { TYPES } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Optional client-side context provider used to enrich create-edge schema requests.
 * The returned context is forwarded to the diagram server unchanged.
 */
export interface CreateEdgeContextProvider {
    /**
     * Builds context for initial schema lookup.
     *
     * @param source The source node selected by the user
     * @returns Opaque context payload forwarded to the server
     */
    getInitialContext(source: GNode): unknown;

    /**
     * Builds context for target schema lookup.
     *
     * @param source The selected source node
     * @param target The currently hovered target node
     * @param currentSchema The schema currently active on the client
     * @returns Opaque context payload forwarded to the server
     */
    getTargetContext(source: GNode, target: GNode, currentSchema: CreateEdgeSchema): unknown;
}

/**
 * DI token for the optional create-edge context provider.
 */
export const CreateEdgeContextProvider = Symbol("CreateEdgeContextProvider");

/**
 * Default create-edge provider implementation.
 * Delegates schema resolution to the diagram server via request actions.
 */
@injectable()
export class CreateEdgeProvider {
    @inject(TYPES.IActionDispatcher)
    protected readonly actionDispatcher!: IActionDispatcher;

    @inject(CreateEdgeContextProvider)
    @optional()
    protected readonly contextProvider?: CreateEdgeContextProvider;

    /**
     * Returns the initial edge schema after the source node is selected.
     * This determines the type, visual appearance, and parameters of the edge
     * during the second phase of creation (before a target is selected).
     *
     * @param source The selected source node
     * @returns The initial edge schema, or undefined if creation should not proceed
     */
    async getInitialSchema(source: GNode): Promise<CreateEdgeSchema | undefined> {
        const response = await this.actionDispatcher.request(
            RequestCreateEdgeInitialSchemaAction.create({
                sourceElementId: source.id,
                context: this.contextProvider?.getInitialContext(source)
            })
        );
        return response.schema;
    }

    /**
     * Optionally returns an updated edge schema when hovering over a potential target.
     * Called after the base validity check passes (canConnect).
     * If this returns undefined, the existing schema is kept.
     *
     * @param source The source node
     * @param target The potential target node
     * @param currentSchema The currently active schema
     * @returns An updated schema, or undefined to keep the current one
     */
    async getTargetSchema(
        source: GNode,
        target: GNode,
        currentSchema: CreateEdgeSchema
    ): Promise<CreateEdgeSchema | undefined> {
        const response = await this.actionDispatcher.request(
            RequestCreateEdgeTargetSchemaAction.create({
                sourceElementId: source.id,
                targetElementId: target.id,
                currentSchema,
                context: this.contextProvider?.getTargetContext(source, target, currentSchema)
            })
        );
        return response.schema;
    }
}
