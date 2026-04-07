import { sharedImport } from "../../sharedImport.js";
import { BrowserClipboardService } from "./clipboardService.js";

const { FeatureModule, TYPES, standaloneCopyPasteModule } = sharedImport("@eclipse-glsp/client");

/**
 * Determines whether the browser Clipboard API is available.
 *
 * @returns {@code true} when {@code navigator.clipboard} is defined.
 */
function hasBrowserClipboard(): boolean {
    return typeof navigator !== "undefined" && navigator.clipboard !== undefined;
}

/**
 * Feature module that provides clipboard (copy/cut/paste) support, extending
 * the base GLSP implementation with optional browser-clipboard persistence.
 */
export const copyPasteModule = new FeatureModule(
    (bind, unbind, isBound, rebind) => {
        if (hasBrowserClipboard()) {
            bind(BrowserClipboardService).toSelf().inSingletonScope();
            rebind(TYPES.IAsyncClipboardService).toService(BrowserClipboardService);
        }
    },
    { featureId: Symbol("copyPaste"), requires: [standaloneCopyPasteModule] }
);
