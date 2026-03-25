import type { EditorContext, LabeledAction } from "@eclipse-glsp/protocol";
import type { GModelElement, OperationHandler, OperationHandlerRegistry } from "@eclipse-glsp/server";
import { ELEMENT_CONTEXT_ACTIONS_ID, type ContextItem, type EdgeAttachmentPosition } from "@mdeo/protocol-common";
import { sharedImport } from "../sharedImport.js";
import type { ContextActionRequestContext } from "./contextActionRequestContext.js";
import type { ContextItemProvider } from "./contextItemProvider.js";
import { GEdge } from "../diagram-server/model/edge.js";
import { GNode } from "../diagram-server/model/node.js";
import type { ContextActionsProvider } from "@eclipse-glsp/server";

const { inject, injectable } = sharedImport("inversify");
const { GModelIndex: GModelIndexKey, OperationHandlerRegistry: OperationHandlerRegistryKey } =
    sharedImport("@eclipse-glsp/server");

/**
 * Context actions provider for element-level context actions.
 *
 * Providers are discovered by scanning registered operation handlers, mirroring
 * how toolbox items are discovered.
 */
@injectable()
export class DefaultContextActionsProvider implements ContextActionsProvider {
    @inject(GModelIndexKey)
    protected index!: { find(id: string): GModelElement | undefined };

    @inject(OperationHandlerRegistryKey)
    protected operationHandlerRegistry!: OperationHandlerRegistry;

    get contextId(): string {
        return ELEMENT_CONTEXT_ACTIONS_ID;
    }

    async getActions(editorContext: EditorContext): Promise<LabeledAction[]> {
        const selectedElementIds = editorContext.selectedElementIds;
        if (selectedElementIds.length !== 1) {
            return [];
        }

        const element = this.index.find(selectedElementIds[0]);
        if (!this.isNodeOrEdge(element)) {
            return [];
        }

        const requestContext: ContextActionRequestContext = {
            element,
            edgePosition: this.parseEdgePosition(editorContext.args)
        };

        const contextItems: ContextItem[] = [];
        const providers = this.operationHandlerRegistry
            .getAll()
            .filter((handler) => this.isContextItemProvider(handler));

        for (const provider of providers) {
            const items = await provider.getContextItems(element, requestContext);
            contextItems.push(...items);
        }

        return this.sortBySortString(contextItems) as unknown as LabeledAction[];
    }

    protected parseEdgePosition(args: unknown): EdgeAttachmentPosition | undefined {
        if (typeof args !== "object" || args == null || !("edgePosition" in args)) {
            return undefined;
        }

        const maybeEdgePosition = (args as { edgePosition?: unknown }).edgePosition;
        return typeof maybeEdgePosition === "string" ? (maybeEdgePosition as EdgeAttachmentPosition) : undefined;
    }

    protected isNodeOrEdge(element: GModelElement | undefined): element is GModelElement {
        return element instanceof GNode || element instanceof GEdge;
    }

    protected isContextItemProvider(handler: OperationHandler): handler is OperationHandler & ContextItemProvider {
        return "getContextItems" in handler && typeof handler.getContextItems === "function";
    }

    protected sortBySortString(items: ContextItem[]): ContextItem[] {
        return items
            .map((item, index) => ({ item, index }))
            .sort((left, right) => {
                const leftSortString = left.item.sortString;
                const rightSortString = right.item.sortString;

                if (leftSortString === undefined && rightSortString === undefined) {
                    return left.index - right.index;
                }
                if (leftSortString === undefined) {
                    return 1;
                }
                if (rightSortString === undefined) {
                    return -1;
                }

                const result = leftSortString.localeCompare(rightSortString, undefined, { numeric: true });
                return result !== 0 ? result : left.index - right.index;
            })
            .map((entry) => entry.item);
    }
}
