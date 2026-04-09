import type { ClipboardData } from "@eclipse-glsp/protocol";
import type { IAsyncClipboardService } from "@eclipse-glsp/client";

/**
 * Clipboard service that writes clipboard data to the browser's system clipboard
 * via the async {@code navigator.clipboard} API for cross-window support,
 * while also maintaining an in-memory copy for synchronous retrieval.
 *
 * <p>Falls back gracefully to in-memory-only storage when the Clipboard API is
 * not available or the write fails.
 *
 * <p>The UUID written to the system clipboard (via {@code DataTransfer.setData})
 * during the copy event acts as a staleness check on paste.
 */
export class BrowserClipboardService implements IAsyncClipboardService {
    /**
     * The current clipboard data identifier.
     */
    private currentId?: string;
    /**
     * The in-memory copy of the stored clipboard data.
     */
    private data?: ClipboardData;

    clear(): void {
        this.currentId = undefined;
        this.data = undefined;
    }

    put(data: ClipboardData, id?: string): void {
        this.currentId = id;
        this.data = data;
        if (typeof navigator !== "undefined" && navigator.clipboard?.writeText) {
            navigator.clipboard.writeText(JSON.stringify({ id, data })).catch(() => {
                // Silently fall back to in-memory if write fails
            });
        }
    }

    get(id?: string): ClipboardData | undefined {
        if (id !== this.currentId) {
            return undefined;
        }
        return this.data;
    }
}
