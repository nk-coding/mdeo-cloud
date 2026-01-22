import type { ComputedRef, InjectionKey } from "vue";

export interface TreeItem {
    id: string;
}

export interface DragAndDropCallbacks {
    onDragStart?: (item: TreeItem, event: DragEvent) => void;
    onDragEnd?: (item: TreeItem, event: DragEvent) => void;
    onDrop?: (droppedItemId: string, targetItem: TreeItem, event: DragEvent) => void | Promise<void>;
    onTreeDrop?: (droppedItemId: string, event: DragEvent) => void | Promise<void>;
    canDrop?: (droppedItemId: string, targetItem: TreeItem) => boolean;
}

export interface DragAndDropConfig {
    enabled: boolean;
    callbacks?: DragAndDropCallbacks;
}

export interface TreeContext {
    activeItem: ComputedRef<TreeItem | undefined>;
    dragAndDrop: ComputedRef<DragAndDropConfig>;
    expandedItems: ComputedRef<Set<TreeItem>>;
}

export const treeContextKey = Symbol("treeContext") as InjectionKey<TreeContext>;
