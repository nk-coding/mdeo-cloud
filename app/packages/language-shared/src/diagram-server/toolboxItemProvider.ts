import type { Args, PaletteItem } from "@eclipse-glsp/protocol";
import type { MaybePromise } from "@eclipse-glsp/protocol";
import type { OperationHandler } from "@eclipse-glsp/server";

/**
 * Represents a toolbox item along with its group information.
 * This structure allows providers to specify which palette group an item belongs to.
 */
export interface GroupedToolboxItem {
    /**
     * The palette item to be displayed in the toolbox.
     */
    item: PaletteItem;

    /**
     * The identifier of the group this item belongs to.
     * Items with the same groupId will be grouped together in the palette.
     */
    groupId: string;
}

/**
 * Interface for components that can provide toolbox items for the tool palette.
 */
export interface ToolboxItemProvider extends OperationHandler {
    /**
     * Returns the toolbox items provided by this provider.
     *
     * @param args Optional arguments that may influence the provided items (e.g., based on context)
     * @returns An array of grouped toolbox items, or a promise resolving to such an array
     */
    getToolboxItems(args: Args | undefined): MaybePromise<GroupedToolboxItem[]>;
}
