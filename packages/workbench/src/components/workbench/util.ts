import type { WorkbenchState } from "@/data/workbenchState";
import type { MonacoApi } from "@/lib/monacoPlugin";
import type { InjectionKey } from "vue";

export const monacoApiKey: InjectionKey<MonacoApi> = Symbol("monacoApi");

/**
 * Injection key for the WorkbenchState
 */
export const workbenchStateKey = Symbol("workbenchState") as InjectionKey<WorkbenchState>;
