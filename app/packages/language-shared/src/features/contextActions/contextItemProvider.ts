import type { GModelElement } from "@eclipse-glsp/server";
import type { MaybePromise } from "@eclipse-glsp/protocol";
import type { ContextItem } from "@mdeo/protocol-common";
import type { ContextActionRequestContext } from "./contextActionRequestContext.js";

/**
 * Interface for components that can provide context items for elements in the diagram.
 *
 * This is a duck-typing interface used for dynamic provider discovery. Classes that
 * implement this interface are automatically detected and managed by the
 * BaseContextItemProvider framework.
 *
 * Context items are context-sensitive actions that appear at specific positions on
 * diagram elements (particularly edges) or in context menus. Unlike static palette items,
 * context items are dynamically determined based on:
 * - The specific model element selected
 * - The position on the element (for edges)
 * - The current state or properties of the element
 *
 * ## Implementation Pattern
 *
 * Providers typically extend BaseContextItemProvider and override getContextItems():
 *
 * ```typescript
 * class DeleteNodeContextProvider extends BaseContextItemProvider {
 *   override getContextItems(element: GModelElement, context: ContextActionRequestContext): ContextItem[] {
 *     if (isDeleteable(element)) {
 *       return [{
 *         id: `delete-${element.id}`,
 *         label: "Delete",
 *         icon: "trash-2",
 *         action: { kind: "deleteElement", elementId: element.id }
 *       }];
 *     }
 *     return [];
 *   }
 * }
 * ```
 *
 * ## Discovery Process
 *
 * The default context-actions provider discovers implementations by scanning
 * operation handlers registered in the operation handler registry. Any handler
 * that exposes a getContextItems() function is treated as a provider.
 *
 * This enables polymorphic context item discovery where multiple independent
 * providers can contribute context items without knowing about each other.
 *
 * @see BaseContextItemProvider - Base class providing default implementation
 * @see ContextActionRequestContext - Context passed to providers
 * @see ContextItem - Structure of items returned by providers
 */
export interface ContextItemProvider {
    /**
     * Returns context items applicable to the given model element and context.
     *
     * This method is called when context items are requested for a specific element.
     * Implementations should analyze the element and context, then return an array
     * of applicable context items.
     *
     * ## Async Behavior
     *
     * This method can return either:
     * - A synchronous array: `ContextItem[]`
     * - A promise resolving to an array: `Promise<ContextItem[]>`
     *
     * This flexibility allows providers to be either synchronous or asynchronous
     * depending on whether they need to query additional data or perform I/O.
     *
     * ## Context-Based Filtering
     *
     * Providers should use the context parameter to make intelligent decisions:
     *
     * ```typescript
     * getContextItems(
     *   element: GModelElement,
     *   context: ContextActionRequestContext
     * ): ContextItem[] {
     *   const items: ContextItem[] = [];
     *
     *   // Check element type
     *   if (isEdge(element)) {
     *     // Position-specific items for edges
     *     if (context.edgePosition === EdgeAttachmentPosition.START) {
     *       items.push({
     *         id: `change-source-${element.id}`,
     *         label: "Change Source",
     *         icon: "edit-2",
     *         position: EdgeAttachmentPosition.START,
     *         action: { kind: "changeSource", edgeId: element.id }
     *       });
     *     }
     *   } else if (isNode(element)) {
     *     // Node-level context items
     *     items.push({
     *       id: `add-property-${element.id}`,
     *       label: "Add Property",
     *       icon: "plus",
     *       action: { kind: "addProperty", nodeId: element.id }
     *     });
     *   }
     *
     *   return items;
     * }
     * ```
     *
     * ## Return Value
     *
     * Return an empty array `[]` if:
     * - The element is not applicable to this provider
     * - No context items are applicable in the current context
     * - The element is not in a state where actions can be performed
     *
     * Multiple providers can each return items for the same element.
     * All returned items are collected and combined in the response.
     *
     * @param element The model element for which context items are requested
     * @param context Additional context about the action (position, element state, etc.)
     * @returns An array of context items, or a promise resolving to such an array
     *
     * @see ContextItem - Structure of items to return
     * @see ContextActionRequestContext - Context structure
     * @see EdgeAttachmentPosition - For position-aware filtering
     */
    getContextItems(element: GModelElement, context: ContextActionRequestContext): MaybePromise<ContextItem[]>;
}
