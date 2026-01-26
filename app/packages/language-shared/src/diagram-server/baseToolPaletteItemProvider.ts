import type { PaletteItem } from "@eclipse-glsp/protocol";
import type { OperationHandler, OperationHandlerRegistry } from "@eclipse-glsp/server";
import { sharedImport } from "../sharedImport.js";
import type { ToolboxItemProvider } from "./toolboxItemProvider.js";

const { injectable, inject } = sharedImport("inversify");
const { OperationHandlerRegistry: OperationHandlerRegistryKey, ToolPaletteItemProvider } =
    sharedImport("@eclipse-glsp/server");

/**
 * Base implementation of ToolPaletteItemProvider that automatically collects and organizes
 * palette items from multiple sources:
 */
@injectable()
export abstract class BaseToolPaletteItemProvider extends ToolPaletteItemProvider {
    /**
     * Registry containing all operation handlers, used to discover CreateOperationHandlers.
     */
    @inject(OperationHandlerRegistryKey)
    protected operationHandlerRegistry!: OperationHandlerRegistry;

    /**
     * Constructs the list of palette items by gathering items from all registered ToolboxItemProviders.
     *
     * @returns An array of palette items organized by groups
     */
    protected async getGroupedItems(): Promise<Map<string, PaletteItem[]>> {
        const handlers = this.operationHandlerRegistry
            .getAll()
            .filter((handler) => this.isPaletteItemProvider(handler));
        const groupedItems: Map<string, PaletteItem[]> = new Map();

        for (const handler of handlers) {
            const toolboxProvider = handler as ToolboxItemProvider;
            const toolboxItems = await toolboxProvider.getToolboxItems();

            for (const groupedItem of toolboxItems) {
                const groupId = groupedItem.groupId;
                const item = groupedItem.item;

                if (!groupedItems.has(groupId)) {
                    groupedItems.set(groupId, []);
                }

                const group = groupedItems.get(groupId);
                group!.push(item);
            }
        }
        return groupedItems;
    }

    /**
     * Type guard to check if a handler is a ToolboxItemProvider.
     *
     * @param handler The operation handler to check
     * @returns True if the handler implements ToolboxItemProvider, false otherwise
     */
    private isPaletteItemProvider(handler: OperationHandler): handler is ToolboxItemProvider {
        return "getToolboxItems" in handler && typeof handler.getToolboxItems === "function";
    }
}
