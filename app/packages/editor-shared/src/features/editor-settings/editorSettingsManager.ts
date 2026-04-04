import type { IActionDispatcher } from "@eclipse-glsp/sprotty";
import { sharedImport } from "../../sharedImport.js";
import { getGlobalEditorSettingsProvider, type EditorSettings, DEFAULT_EDITOR_SETTINGS } from "@mdeo/editor-common";
import { UpdateEditorSettingsAction } from "@mdeo/protocol-common";

const { injectable, inject } = sharedImport("inversify");
const { TYPES } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Singleton service that manages editor settings lifecycle for a diagram session.
 *
 * Responsibilities:
 * - Reading and writing settings through the globally registered {@link EditorSettingsProvider}.
 * - Dispatching {@link UpdateEditorSettingsAction} to the language server when settings change.
 * - Notifying in-process subscribers (e.g. the Toolbox) when settings change from any source.
 * - Subscribing to external (cross-tab) changes via the provider and propagating them locally.
 */
@injectable()
export class EditorSettingsManager {
    @inject(TYPES.IActionDispatcher)
    declare protected readonly actionDispatcher: IActionDispatcher;

    private readonly listeners = new Set<(settings: EditorSettings) => void>();
    private disposeExternal?: () => void;

    /**
     * Returns the current editor settings from the global provider.
     * Falls back to {@link DEFAULT_EDITOR_SETTINGS} if no provider is registered.
     */
    getCurrentSettings(): EditorSettings {
        return getGlobalEditorSettingsProvider()?.getSettings() ?? DEFAULT_EDITOR_SETTINGS;
    }

    /**
     * Dispatches the current settings from the provider to the language server.
     * Call this during `preRequestModel` so the server is initialised with the
     * client-side persisted state from the very beginning of the session.
     */
    async syncToServer(): Promise<void> {
        const settings = this.getCurrentSettings();
        await this.actionDispatcher.dispatch(UpdateEditorSettingsAction.create(settings));
    }

    /**
     * Subscribes to external (cross-tab) settings changes via the global provider.
     * When an external change is received, all registered listeners are notified.
     *
     * Call this during `postRequestModel`, after the editor is fully set up.
     * Any previous subscription created by a prior `activate()` call is cleaned up first.
     */
    activate(): void {
        const provider = getGlobalEditorSettingsProvider();
        this.disposeExternal?.();
        this.disposeExternal = provider?.onExternalSettingsChange((settings) => {
            this.notifyListeners(settings);
        });
    }

    /**
     * Removes the external-change subscription set up by {@link activate}.
     * Call this when the editor/session is being torn down.
     */
    deactivate(): void {
        this.disposeExternal?.();
        this.disposeExternal = undefined;
    }

    /**
     * Persists the given settings via the global provider and dispatches
     * {@link UpdateEditorSettingsAction} to the language server.
     *
     * Use this when the user explicitly changes a setting (e.g. toggling the toolbox).
     *
     * @param settings The new settings to persist and sync.
     */
    saveAndSync(settings: EditorSettings): void {
        getGlobalEditorSettingsProvider()?.saveSettings(settings);
        this.actionDispatcher.dispatch(UpdateEditorSettingsAction.create(settings));
    }

    /**
     * Applies settings that were pushed from the language server:
     * persists them via the global provider, which synchronously notifies all
     * same-page editor listeners and persists to localStorage for cross-tab sync.
     *
     * @param settings The settings received from the server.
     */
    applyFromServer(settings: EditorSettings): void {
        getGlobalEditorSettingsProvider()?.saveSettings(settings);
    }

    /**
     * Registers a callback that is called whenever the editor settings change
     * from an external source (cross-tab) or when the server pushes new settings.
     *
     * @param callback Function to call with the updated settings.
     * @returns An unsubscribe function that removes this callback.
     */
    addListener(callback: (settings: EditorSettings) => void): () => void {
        this.listeners.add(callback);
        return () => this.listeners.delete(callback);
    }

    private notifyListeners(settings: EditorSettings): void {
        for (const cb of this.listeners) {
            cb(settings);
        }
    }
}
