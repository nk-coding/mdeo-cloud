import { computed, ref, type Ref, type ComputedRef } from "vue";
import type { Diagnostic as VDiagnostic, Uri } from "vscode";
import { DiagnosticSeverity } from "vscode";

/**
 * Summary of diagnostics for a single file.
 */
export interface FileDiagnosticSummary {
    readonly errors: number;
    readonly warnings: number;
}

/**
 * Reactive diagnostic store that tracks diagnostics published by the language server.
 * Provides per-file and per-folder aggregation.
 */
export interface DiagnosticStore {
    /**
     * Set diagnostics for a file URI. Called from the language client middleware.
     */
    set(uri: Uri, diagnostics: VDiagnostic[]): void;

    /**
     * Clear all stored diagnostics (e.g., on project/server change).
     */
    clear(): void;

    /**
     * Remove diagnostics for a specific URI.
     */
    delete(uri: Uri): void;

    /**
     * Reactive map of URI string to diagnostic summary.
     */
    readonly fileDiagnostics: Ref<ReadonlyMap<string, FileDiagnosticSummary>>;

    /**
     * Get the error count for a specific file URI.
     */
    getFileErrorCount(uriString: string): ComputedRef<number>;

    /**
     * Get the error count for a folder URI (aggregated from all files within it).
     */
    getFolderErrorCount(uriString: string): ComputedRef<number>;
}

/**
 * Creates a reactive diagnostic store.
 */
export function useDiagnostics(): DiagnosticStore {
    const fileDiagnostics = ref<Map<string, FileDiagnosticSummary>>(new Map());

    function set(uri: Uri, diagnostics: VDiagnostic[]): void {
        let errors = 0;
        let warnings = 0;
        for (const d of diagnostics) {
            if (d.severity === DiagnosticSeverity.Error) {
                errors++;
            } else if (d.severity === DiagnosticSeverity.Warning) {
                warnings++;
            }
        }

        const key = uri.toString();
        const current = fileDiagnostics.value.get(key);

        if (errors === 0 && warnings === 0) {
            if (current != null) {
                fileDiagnostics.value.delete(key);
                triggerUpdate();
            }
            return;
        }

        if (current != null && current.errors === errors && current.warnings === warnings) {
            return;
        }

        fileDiagnostics.value.set(key, { errors, warnings });
        triggerUpdate();
    }

    function clear(): void {
        if (fileDiagnostics.value.size > 0) {
            fileDiagnostics.value = new Map();
        }
    }

    function deleteUri(uri: Uri): void {
        const key = uri.toString();
        if (fileDiagnostics.value.has(key)) {
            fileDiagnostics.value.delete(key);
            triggerUpdate();
        }
    }

    /**
     * Triggers Vue reactivity update by replacing the map reference.
     */
    function triggerUpdate(): void {
        fileDiagnostics.value = new Map(fileDiagnostics.value);
    }

    function getFileErrorCount(uriString: string): ComputedRef<number> {
        return computed(() => fileDiagnostics.value.get(uriString)?.errors ?? 0);
    }

    function getFolderErrorCount(folderUriString: string): ComputedRef<number> {
        return computed(() => {
            let total = 0;
            // Folder URI ends without trailing slash, file URIs start with the folder path
            const prefix = folderUriString.endsWith("/") ? folderUriString : folderUriString + "/";
            for (const [uri, summary] of fileDiagnostics.value) {
                if (uri.startsWith(prefix)) {
                    total += summary.errors;
                }
            }
            return total;
        });
    }

    return {
        set,
        clear,
        delete: deleteUri,
        fileDiagnostics,
        getFileErrorCount,
        getFolderErrorCount
    };
}
