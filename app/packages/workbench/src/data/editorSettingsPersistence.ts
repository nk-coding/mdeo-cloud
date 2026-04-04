import { toRaw, watch } from "vue";
import { useLocalStorage } from "@vueuse/core";
import { setGlobalEditorSettingsProvider, DEFAULT_EDITOR_SETTINGS } from "@mdeo/editor-common";
import type { EditorSettings } from "@mdeo/editor-common";

/**
 * Initialises localStorage-backed editor settings and registers a global
 * {@link EditorSettingsProvider} so that graphical editors and language servers
 * can read and persist UI state.
 *
 * Call this **once** during application startup (e.g., in `App.vue`'s
 * `<script setup>`) **before** any graphical editor containers are created.
 *
 * Settings are stored under their own `localStorage` key and synchronised
 * across tabs automatically via VueUse's `useLocalStorage`.
 * Vue reactive proxies are unwrapped with {@link toRaw} before any value is
 * handed to the server to ensure plain serialisable objects are transmitted.
 */
export function useEditorSettingsPersistence(): void {
    const stored = useLocalStorage<EditorSettings>("mdeo-editor-settings", DEFAULT_EDITOR_SETTINGS, {
        mergeDefaults: true
    });

    const externalChangeCallbacks = new Set<(settings: EditorSettings) => void>();

    let saving = false;

    watch(
        stored,
        (newVal) => {
            if (!saving) {
                const raw = toRaw(newVal);
                for (const cb of externalChangeCallbacks) {
                    cb(raw);
                }
            }
        },
        { deep: true, flush: "sync" }
    );

    setGlobalEditorSettingsProvider({
        getSettings: () => toRaw(stored.value),
        saveSettings: (settings) => {
            const raw = toRaw(settings) as EditorSettings;
            for (const cb of externalChangeCallbacks) {
                cb(raw);
            }
            saving = true;
            stored.value = settings;
            saving = false;
        },
        onExternalSettingsChange: (callback) => {
            externalChangeCallbacks.add(callback);
            return () => externalChangeCallbacks.delete(callback);
        }
    });
}
