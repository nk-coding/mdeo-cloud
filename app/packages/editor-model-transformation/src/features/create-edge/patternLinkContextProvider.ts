import type { GNode } from "@mdeo/editor-shared";
import { sharedImport, Toolbox, type CreateEdgeContextProvider } from "@mdeo/editor-shared";
import type { ModelTransformationToolbox } from "../toolbox/modelTransformationToolbox.js";

const { injectable, inject } = sharedImport("inversify");

/**
 * Context payload forwarded to the diagram server with each schema request.
 * The server uses the `mode` field to pre-select the edge modifier when
 * both source and target endpoints have no modifier (persist-persist pair).
 */
export interface PatternLinkEdgeContext {
    /** The active node creation mode (e.g. `"persist"`, `"create"`, `"delete"`). */
    mode: string;
}

/**
 * Client-side context provider for pattern link edge creation in model transformation diagrams.
 *
 * Forwards the currently selected {@link ModelTransformationToolbox.selectedMode} as an opaque
 * context payload to the diagram server. Both the initial and target schema requests include this
 * context so the server can determine the correct edge modifier for persist-persist node pairs.
 *
 * The toolbox is injected via the {@link Toolbox} service identifier, which is rebound to
 * {@link ModelTransformationToolbox} in the toolbox feature module.
 */
@injectable()
export class PatternLinkContextProvider implements CreateEdgeContextProvider {
    @inject(Toolbox)
    protected readonly toolbox!: ModelTransformationToolbox;

    /**
     * Returns the context for the initial schema request (source selected).
     *
     * @param _source The selected source node (unused; mode is toolbox-global)
     * @returns The context carrying the active creation mode
     */
    getInitialContext(_source: GNode): PatternLinkEdgeContext {
        return { mode: this.toolbox.selectedMode };
    }

    /**
     * Returns the context for the target schema request (target hovered/selected).
     *
     * @param _source The source node (unused)
     * @param _target The target node (unused)
     * @returns The context carrying the active creation mode
     */
    getTargetContext(_source: GNode, _target: GNode): PatternLinkEdgeContext {
        return { mode: this.toolbox.selectedMode };
    }
}
