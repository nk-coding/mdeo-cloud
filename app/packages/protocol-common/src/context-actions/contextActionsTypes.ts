import type { Action } from "@eclipse-glsp/protocol";

/**
 * Position for context action attachment on an edge.
 *
 * Used to place context action buttons at specific locations on edges,
 * enabling context-sensitive operations at different parts of the diagram.
 *
 * Each position corresponds to a corner or side of the edge area,
 * allowing multiple context items to be displayed without overlap.
 */
export enum EdgeAttachmentPosition {
    /**
     * Near the source node.
     */
    START = "start",
    /**
     * Near the target node.
     */
    END = "end",
    /**
     * At the midpoint of the edge.
     */
    MIDDLE = "middle"
}

/**
 * Shared context ID used for element-level context actions.
 */
export const ELEMENT_CONTEXT_ACTIONS_ID = "element-context";

/**
 * Icon representation in GLSP format for network transmission.
 *
 * Converted from Lucide icons for serialization over the network protocol.
 * Contains all icon metadata as serializable strings and objects.
 *
 * @see ContextItem.icon - Where this type is used
 */
export interface ActionIconNode {
    /**
     * Icon name from the Lucide icon library.
     *
     * Lucide provides a comprehensive set of consistent SVG icons.
     * The icon name is resolved client-side via the IconRegistry.
     *
     * Examples:
     * - "plus" - Addition/creation icon
     * - "trash-2" - Deletion icon
     * - "arrow-right" - Navigation or flow direction
     * - "zap" - Action or modification
     * - "copy" - Duplication
     * - "edit-2" - Editing
     */
    name: string;

    /**
     * Relative bounds for icon sizing within its container.
     *
     * Used to scale and position the icon within the container.
     * Values can be specified as:
     * - Numbers: Absolute pixel values or relative units
     * - Strings: CSS values like "1em", "16px", "25%"
     *
     * Both width and height should typically be the same for uniform scaling.
     *
     * If omitted, the icon uses default sizing from IconRegistry.
     */
    relativeBounds?: {
        /**
         * Icon width.
         */
        width: number | string;

        /**
         * Icon height.
         */
        height: number | string;
    };

    /**
     * Additional CSS classes to apply to the icon element.
     *
     * Useful for styling, sizing, animation, and color modifications.
     *
     * Common patterns:
     * - "w-4 h-4" - Tailwind sizing (small icon)
     * - "w-6 h-6" - Tailwind sizing (medium icon)
     * - "text-blue-500" - Tailwind color
     * - "text-red-600" - Tailwind color
     * - "animate-spin" - Animation
     * - "opacity-50" - Transparency
     *
     * Multiple classes can be specified space-separated.
     */
    cssClasses?: string;
}

/**
 * Data structure representing a single context action item.
 *
 * Context items model hierarchical, context-sensitive actions that can be
 * displayed in the UI at specific positions (for edges) or as menus (for nodes).
 *
 * Similar to ToolboxItem but designed for:
 * - Positional attachment to edges via EdgeAttachmentPosition
 * - Icon-only or icon+label rendering modes
 * - Nested children for submenu/hierarchical display
 * - Direct action dispatch on selection
 *
 * @see EdgeAttachmentPosition - For position-aware rendering on edges
 * @see ActionIconNode - For icon representation
 */
export interface ContextItem {
    /**
     * Unique identifier for this context item within the context action system.
     *
     * Should be globally unique to avoid conflicts with other context items.
     *
     * Naming convention:
     * - For node items: `{action-type}-{element-id}`
     * - For edge items: `{action-type}-{element-id}-{position}`
     *
     * Examples:
     * - "add-property-Node_1"
     * - "add-property-Node_1-class-properties"
     * - "change-end-Edge_1-source-left"
     * - "delete-Element_1"
     * - "insert-match-Edge_5-middle-left"
     *
     * This ID is used for:
     * - Tracking selected items in UI
     * - Associating items with corresponding actions
     * - Analytics and debugging
     */
    id: string;

    /**
     * Display label for this context item.
     *
     * Optional - items can be rendered as icon-only if no label is provided.
     *
     * If provided:
     * - Appears next to the icon in the UI
     * - For parent items (with children), this is the menu/submenu label
     * - For leaf items, this is the action description shown to the user
     *
     * Examples:
     * - "Add Property"
     * - "Change Type"
     * - "Source End"
     * - "Delete Element"
     * - "Make Abstract"
     *
     * Should be concise and action-oriented for better UX.
     */
    label?: string;

    /**
     * Icon representation for this context item.
     *
     * Can be specified as:
     * - A string: Simple icon name from Lucide (e.g., "plus", "trash-2")
     * - An ActionIconNode object: For icon with custom sizing and styling
     *
     * The icon is resolved on the client side using the IconRegistry,
     * which converts Lucide icon names to SVG/VNode representations.
     *
     * If not provided, the item renders as text-only with no icon.
     *
     * Common patterns:
     * - Creation operations: "plus"
     * - Deletion operations: "trash-2"
     * - Modification operations: "edit-2"
     * - Navigation: "arrow-right"
     *
     * For custom styling, use ActionIconNode format:
     * ```typescript
     * icon: {
     *   name: "plus",
     *   relativeBounds: { width: "1.2em", height: "1.2em" },
     *   cssClasses: "text-blue-500"
     * }
     * ```
     *
     * @see ActionIconNode - For detailed icon customization options
     */
    icon?: string | ActionIconNode;

    /**
     * Position for attachment on edges.
     *
     * Only applicable for context items displayed on edges.
     * Specifies where around the edge this item should appear visually.
     *
     * For node context items, this field should be undefined/omitted.
     *
     * This field enables multiple context items to be rendered at different
     * positions on the same edge without visual overlap, providing a rich
     * context-aware UI experience.
     *
     * Examples:
     * - START: "Change source end" or "Add source multiplicity" on associations
     * - END: "Add target multiplicity" on associations
     * - MIDDLE: "Insert for-match" on transformation edges
     *
     * If multiple items have the same position, they are typically rendered
     * in a submenu or list to avoid crowding.
     *
     * @see EdgeAttachmentPosition - For available position options
     */
    position?: EdgeAttachmentPosition;

    /**
     * Nested context items for hierarchical/submenu display.
     *
     * If provided (non-empty array):
     * - This item acts as a parent/menu trigger
     * - Usually displayed differently (e.g., with expand indicator)
     * - Children items appear in a submenu/dropdown when parent is activated
     * - Each child still has its own independent action
     *
     * If not provided or empty:
     * - Item is a leaf node in the hierarchy
     * - Clicking/selecting directly executes the item's action
     * - No submenu is displayed
     *
     * Children can themselves have children, enabling unlimited nesting
     * (though typically limited to 2-3 levels for UX reasons).
     *
     * Examples:
     *
     * Parent item with children:
     * ```typescript
     * {
     *   id: "add-link-root",
     *   label: "Add Link",
     *   icon: "link",
     *   children: [
     *     { id: "add-link-aggregation", label: "Aggregation", action: ... },
     *     { id: "add-link-composition", label: "Composition", action: ... }
     *   ]
     * }
     * ```
     *
     * Leaf item without children:
     * ```typescript
     * {
     *   id: "delete-element",
     *   label: "Delete",
     *   icon: "trash-2",
     *   action: { ... }
     * }
     * ```
     *
     * @see action - The action executed when a leaf item is selected
     */
    children?: ContextItem[];

    /**
     * The GLSP action to dispatch when this item is selected.
     *
     * For parent items (with children), this field is typically undefined
     * unless the parent is also directly actionable (rare but supported).
     *
     * For leaf items (without children), this is required and will be
     * dispatched to the GLSP action handler when the user clicks/selects
     * this item.
     *
     * The action can be any Action from GLSP protocol, including:
     * - Custom operations (e.g., AddPropertyOperation)
     * - Standard GLSP actions (e.g., DeleteOperation)
     * - Server-side triggered commands
     *
     * The server includes the action as part of SetContextActionsAction
     * in the items response. The client dispatches this action to
     * the GLSP action handler registered for the action kind.
     *
     * Example:
     * ```typescript
     * action: {
     *   kind: "addProperty",
     *   classId: "Node_1"
     * }
     * ```
     *
     * @see Action - GLSP protocol action interface
     */
    action?: Action;

    /**
     * Optional sort key for ordering items in the UI.
     *
     * If provided, context items are sorted by this value before display.
     * Useful for ensuring consistent display order across different
     * executions or conditions.
     *
     * If not provided on any item, items maintain the server-provided order.
     * If provided on some items, those items are sorted while unprovided
     * items appear at the end in original order.
     *
     * Can be:
     * - A numeric string: "1", "2", "10" (sorted numerically)
     * - An alphabetic string: "a", "b", "z"
     * - A mixed string that is sorted lexicographically
     *
     * Examples:
     * ```typescript
     * [
     *   { id: "add", sortString: "1", ... },
     *   { id: "edit", sortString: "2", ... },
     *   { id: "delete", sortString: "3", ... }
     * ]
     * ```
     *
     * Sorted result: add → edit → delete
     */
    sortString?: string;
}

/**
 * Primary export type for context action items.
 *
 * This is the standard interface used throughout the context actions system
 * for representing individual context action items. It is fully compatible
 * with nested hierarchies via the children property.
 *
 * Used in:
 * - SetContextActionsAction.items array
 * - Server-side ContextItemProvider implementations
 * - Client-side context action UI rendering
 *
 * @see SetContextActionsAction
 * @see ContextItem
 */
export type ContextActionItem = ContextItem;

/**
 * Array type for context action items.
 *
 * Represents a list of context action items returned by server handlers.
 * This is the result type for:
 * - ContextItemProvider.getContextItems()
 * - Aggregated results from multiple providers
 * - Items included in SetContextActionsAction
 *
 * Each item in the array can be:
 * - A leaf item (with action, no children)
 * - A parent item (with children, typically no action)
 * - A collection of mixed items
 *
 * The array order is preserved unless sortString is used on items.
 *
 * @see ContextActionItem
 */
export type ContextActionItemArray = ContextActionItem[];

/**
 * Response type from server for context actions query.
 *
 * Encapsulates the result of a context actions request, containing
 * the array of items to display for the selected element(s).
 *
 * This is the structure returned by ContextItemProvider implementations
 * and aggregated by BaseContextItemProvider before sending to client
 * via SetContextActionsAction.
 *
 * @see ContextActionItemArray
 */
export type ContextActionResult = ContextActionItemArray;
