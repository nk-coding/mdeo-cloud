import type { GModelElement } from "@eclipse-glsp/server";
import type { ContextItem } from "@mdeo/protocol-common";
import { sharedImport } from "../sharedImport.js";
import type { ContextItemProvider } from "./contextItemProvider.js";
import type { ContextActionRequestContext } from "./contextActionRequestContext.js";

const { injectable } = sharedImport("inversify");

/**
 * Base implementation of ContextItemProvider that automatically collects and organizes
 * context items from multiple sources.
 *
 * This abstract base class provides the default no-op implementation of
 * ContextItemProvider and serves as a convenient extension point.
 *
 * ## Usage
 *
 * Extend this class and override getContextItems() to provide context-specific items:
 *
 * ```typescript
 * \@injectable()
 * class DeleteNodeContextProvider extends BaseContextItemProvider {
 *   override getContextItems(
 *     element: GModelElement,
 *     context: ContextActionRequestContext
 *   ): ContextItem[] {
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
 * Operation handlers that implement this class are automatically discovered by
 * the default context-actions provider via operation-handler registry scanning.
 *
 * ## Method Extension Points
 *
 * Subclasses can override:
 * - `getContextItems()`: Return context items for a specific element and context
 *   This is the main extension point for providing custom context items.
 *
 * @see ContextItemProvider - The interface this class manages
 * @see ContextActionRequestContext - Context passed to providers
 * @see ContextItem - Structure of items returned
 */
@injectable()
export abstract class BaseContextItemProvider implements ContextItemProvider {
    /**
     * Returns context items applicable to the given model element and context.
     *
     * This is the main extension point for subclasses. Default implementation
     * returns an empty array; subclasses should override to provide context-specific items.
     *
     * @param _element The model element for which context items are requested
     * @param _context Additional context about the action (position, element state, etc.)
     * @returns An array of context items applicable to this element and context
     *
     * @see ContextItem - For detailed structure of context items
     * @see ContextActionRequestContext - For context structure
     */
    getContextItems(_element: GModelElement, _context: ContextActionRequestContext): ContextItem[] {
        return [];
    }
}
