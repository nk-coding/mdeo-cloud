/**
 * Persisted editor UI settings.
 *
 * @remarks Intentionally mirrors {@link EditorSettings} from `@mdeo/protocol-common`
 * without importing it, to keep `editor-common` free of cross-package dependencies.
 * Both interfaces are structurally identical and are therefore compatible under
 * TypeScript's structural type system.
 */
export interface EditorSettings {
    /**
     * Whether the toolbox panel is currently open (expanded).
     */
    isOpen: boolean;

    /**
     * Whether the bottom panel inside the toolbox is collapsed.
     */
    isBottomSidebarCollapsed: boolean;
}

/**
 * Service that provides and persists editor UI settings.
 *
 * The workbench registers a concrete implementation (typically backed by
 * `localStorage`) before any graphical editor containers are created.
 *
 * @example Registering a localStorage-backed provider in the workbench:
 * ```ts
 * import { setGlobalEditorSettingsProvider } from '@mdeo/editor-common';
 * import { useLocalStorage } from '@vueuse/core';
 *
 * const stored = useLocalStorage('mdeo-editor-settings', DEFAULT_EDITOR_SETTINGS);
 * setGlobalEditorSettingsProvider({
 *   getSettings: () => stored.value,
 *   saveSettings: (s) => { stored.value = s; },
 * });
 * ```
 */
export interface EditorSettingsProvider {
    /**
     * Returns the current editor settings.
     *
     * @returns The current {@link EditorSettings}.
     */
    getSettings(): EditorSettings;

    /**
     * Persists updated editor settings.
     *
     * @param settings The new settings to store.
     */
    saveSettings(settings: EditorSettings): void;

    /**
     * Registers a callback that is invoked whenever the editor settings are changed
     * by an external source (e.g. another open editor tab).  The callback is **not**
     * fired for changes that were saved by the caller itself.
     *
     * @param callback Function to call with the new settings.
     * @returns An unsubscribe function that removes this callback.
     */
    onExternalSettingsChange(callback: (settings: EditorSettings) => void): () => void;
}

/**
 * Default editor settings used when none are stored.
 */
export const DEFAULT_EDITOR_SETTINGS: EditorSettings = {
    isOpen: true,
    isBottomSidebarCollapsed: false
};

declare global {
    /**
     * Global bridge for the editor settings provider.
     * Set by the workbench; read by graphical editor plugins that may be
     * loaded in a separate JS bundle.
     */

    var mdeoEditorSettingsProvider: EditorSettingsProvider | undefined;
}

/**
 * Registers a global {@link EditorSettingsProvider} that all graphical editor
 * containers will use to read and persist editor settings.
 *
 * Call this once at workbench startup, before any diagrams are loaded.
 *
 * @param provider The provider implementation to register.
 */
export function setGlobalEditorSettingsProvider(provider: EditorSettingsProvider): void {
    globalThis.mdeoEditorSettingsProvider = provider;
}

/**
 * Returns the globally registered {@link EditorSettingsProvider}, or `undefined`
 * if none has been registered yet.
 *
 * @returns The registered provider, or `undefined`.
 */
export function getGlobalEditorSettingsProvider(): EditorSettingsProvider | undefined {
    return globalThis.mdeoEditorSettingsProvider;
}
