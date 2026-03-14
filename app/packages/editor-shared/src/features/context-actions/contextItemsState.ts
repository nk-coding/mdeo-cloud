import type { ContextActionItem } from "@mdeo/protocol-common";

/**
 * Stores context action items state in the diagram model.
 *
 * This class maintains:
 * - The currently displayed context items
 * - The element ID these items apply to
 * - Metadata like timestamp for cache management
 *
 * The state is part of the diagram model and is updated when:
 * - A new element is selected
 * - Server responds with SetContextActionsAction
 * - Model is reset or updated
 */
export class ContextItemsState {
    /**
     * The array of current context action items.
     */
    private items: ContextActionItem[] = [];

    /**
     * The ID of the element these context items apply to.
     */
    private selectedElementId: string | undefined;

    /**
     * Timestamp when items were last updated.
     * Used for cache invalidation and debugging.
     */
    private lastUpdatedTimestamp: number = 0;

    /**
     * Creates a new ContextItemsState instance.
     */
    constructor() {
        this.reset();
    }

    /**
     * Sets the context action items.
     *
     * @param items Array of context action items to store
     */
    public setItems(items: ContextActionItem[]): void {
        this.items = items;
        this.lastUpdatedTimestamp = Date.now();
    }

    /**
     * Gets the currently stored context action items.
     *
     * @returns Array of context action items
     */
    public getItems(): ContextActionItem[] {
        return this.items;
    }

    /**
     * Sets the ID of the element these context items apply to.
     *
     * @param elementId The element ID
     */
    public setSelectedElementId(elementId: string | undefined): void {
        this.selectedElementId = elementId;
        this.lastUpdatedTimestamp = Date.now();
    }

    /**
     * Gets the ID of the element these context items apply to.
     *
     * @returns The element ID, or undefined if no element is selected
     */
    public getSelectedElementId(): string | undefined {
        return this.selectedElementId;
    }

    /**
     * Gets the timestamp when items were last updated.
     *
     * @returns Timestamp in milliseconds
     */
    public getLastUpdatedTimestamp(): number {
        return this.lastUpdatedTimestamp;
    }

    /**
     * Checks if the context items state is empty (no items or element selected).
     *
     * @returns true if empty, false otherwise
     */
    public isEmpty(): boolean {
        return this.items.length === 0 || !this.selectedElementId;
    }

    /**
     * Resets the state to initial empty values.
     * Called on model update or when clearing context items.
     */
    public reset(): void {
        this.items = [];
        this.selectedElementId = undefined;
        this.lastUpdatedTimestamp = Date.now();
    }

    /**
     * Serializes the state for debugging or storage.
     *
     * @returns Object representation of the state
     */
    public toJSON(): Record<string, any> {
        return {
            itemCount: this.items.length,
            selectedElementId: this.selectedElementId,
            lastUpdatedTimestamp: this.lastUpdatedTimestamp,
            items: this.items
        };
    }
}
