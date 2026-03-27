import type { VNode } from "snabbdom";
import { sharedImport } from "../../sharedImport.js";

const { injectable } = sharedImport("inversify");
const { GlspDecorationPlacer } = sharedImport("@eclipse-glsp/client");

/**
 * Replaces the default {@link GlspDecorationPlacer} with a no-op so that the
 * framework does not apply an automatic `transform="translate(…)"` to decoration
 * VNodes (such as `GIssueMarker`).
 *
 * Issue marker badges are positioned entirely by their parent node/edge view, so the
 * automatic offset added by {@link GlspDecorationPlacer} is unwanted.
 */
@injectable()
export class NoopDecorationPlacer extends GlspDecorationPlacer {
    /**
     * Returns `vnode` unchanged — suppresses the automatic translation transform
     * that {@link GlspDecorationPlacer} would otherwise apply.
     *
     * @param vnode The decoration VNode to pass through.
     * @param _element The model element (unused).
     * @returns The unmodified `vnode`.
     */
    override decorate(vnode: VNode, _element: unknown): VNode {
        return vnode;
    }
}
