import type { GModelElement } from "@eclipse-glsp/server";
import type { EdgeAttachmentPosition } from "@mdeo/protocol-common";

/**
 * Context information provided to context item providers when requesting context items.
 *
 * This interface encapsulates all relevant contextual information about the element
 * being acted upon, including its position and attachment point (for edges).
 * This information allows providers to make intelligent decisions about which
 * context items to display based on the specific context of the action.
 *
 * Context is gathered server-side and passed to ContextItemProvider implementations,
 * enabling context-sensitive action discovery and filtering at the provider level.
 *
 * @see ContextItemProvider.getContextItems - Where this context is passed
 */
export interface ContextActionRequestContext {
    /**
     * The model element that is the target of the context action request.
     *
     * This is the element for which context items are being requested.
     * Providers use this to determine applicable context items
     * (e.g., only show "Delete" if the element is deletable).
     *
     * The element can be any GModelElement:
     * - A node (class, component, state, etc.)
     * - An edge (association, transition, dependency, etc.)
     * - A label or decoration
     * - Any other diagram element
     */
    element: GModelElement;

    /**
     * Position information for edge elements.
     *
     * Only applicable when the target element is an edge.
     * Specifies where on the edge the context action was triggered,
     * enabling position-specific context items.
     *
     * For node elements, this field should be undefined.
     *
     * Common use cases:
     * - Multiple context items at different edge positions (source, target, middle)
     * - Position-specific actions like "Change source type" (START)
     * - Position-aware filtering in providers
     *
     * Example:
     * ```typescript
     * if (context.edgePosition === EdgeAttachmentPosition.START) {
     *   return items for changing the source end;
     * }
     * ```
     *
     * @see EdgeAttachmentPosition - For available position options
     */
    edgePosition?: EdgeAttachmentPosition;
}
