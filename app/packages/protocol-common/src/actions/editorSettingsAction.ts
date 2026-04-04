import type { Action } from "@eclipse-glsp/protocol";

/**
 * Persisted editor UI settings communicated between the diagram client and the language server.
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
 * Default editor settings used when no stored settings are available.
 */
export const DEFAULT_EDITOR_SETTINGS: EditorSettings = {
    isOpen: true,
    isBottomSidebarCollapsed: false
};

/**
 * Bidirectional action for synchronising editor UI settings between the diagram client
 * and the language server.
 *
 * Sent client → server when the user changes a UI setting, as well as during
 * diagram-session initialisation so the server always holds the current settings.
 *
 * Sent server → client if the server needs to restore or override the settings
 * (e.g., after a session reset).
 */
export interface UpdateEditorSettingsAction extends Action {
    kind: typeof UpdateEditorSettingsAction.KIND;

    /**
     * The new editor settings to apply.
     */
    settings: EditorSettings;
}

export namespace UpdateEditorSettingsAction {
    export const KIND = "updateEditorSettings";

    /**
     * Creates a new {@link UpdateEditorSettingsAction}.
     *
     * @param settings The editor settings to communicate.
     * @returns The created action.
     */
    export function create(settings: EditorSettings): UpdateEditorSettingsAction {
        return { kind: KIND, settings };
    }

    /**
     * Type guard for {@link UpdateEditorSettingsAction}.
     *
     * @param action The action to check.
     * @returns True if the action is an {@link UpdateEditorSettingsAction}.
     */
    export function is(action: Action): action is UpdateEditorSettingsAction {
        return action.kind === KIND;
    }
}
