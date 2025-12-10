import type { ComputedRef, InjectionKey } from "vue";

export interface TreeItem {
    id: any;
}

export const activeItemKey = Symbol("activeItemKey") as InjectionKey<ComputedRef<TreeItem | undefined>>;
