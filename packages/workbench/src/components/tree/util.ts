import type { ComputedRef, InjectionKey } from "vue";

export interface TreeItem {
    id: any;
}

export interface DragAndDropCallbacks {
    onDragStart?: (item: TreeItem, event: DragEvent) => void;
    onDragEnd?: (item: TreeItem, event: DragEvent) => void;
    onDrop?: (droppedItem: TreeItem, targetItem: TreeItem, event: DragEvent) => void | Promise<void>;
    onTreeDrop?: (droppedItem: TreeItem, event: DragEvent) => void | Promise<void>;
    canDrop?: (droppedItem: TreeItem, targetItem: TreeItem) => boolean;
}

export interface DragAndDropConfig {
    enabled: boolean;
    callbacks?: DragAndDropCallbacks;
}

export const activeItemKey = Symbol("activeItemKey") as InjectionKey<ComputedRef<TreeItem | undefined>>;
export const dragAndDropKey = Symbol("dragAndDropKey") as InjectionKey<ComputedRef<DragAndDropConfig>>;
export const expandedItemsKey = Symbol("expandedItemsKey") as InjectionKey<ComputedRef<Set<TreeItem>>>;
