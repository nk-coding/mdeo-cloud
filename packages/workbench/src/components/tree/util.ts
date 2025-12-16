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

export interface TreeContext {
    activeItem: ComputedRef<TreeItem | undefined>;
    dragAndDrop: ComputedRef<DragAndDropConfig>;
    expandedItems: ComputedRef<Set<TreeItem>>;
}

export const treeContextKey = Symbol("treeContext") as InjectionKey<TreeContext>;
