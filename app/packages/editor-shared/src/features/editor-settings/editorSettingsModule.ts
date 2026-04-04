import { sharedImport } from "../../sharedImport.js";
import { EditorSettingsManager } from "./editorSettingsManager.js";
import { EditorSettingsActionHandler } from "./editorSettingsActionHandler.js";
import { UpdateEditorSettingsAction } from "@mdeo/protocol-common";

const { FeatureModule, configureActionHandler } = sharedImport("@eclipse-glsp/client");

/**
 * Feature module that provides editor settings management as a standalone concern,
 * decoupled from any specific UI extension such as the Toolbox.
 *
 * Registers:
 * - {@link EditorSettingsManager} — singleton service for reading, persisting, and
 *   broadcasting editor settings changes.
 * - {@link EditorSettingsActionHandler} — handles {@link UpdateEditorSettingsAction}
 *   pushed from the language server, delegating to the manager.
 *
 * Included in {@link DEFAULT_MODULES} so it is available to all editor configurations.
 */
export const editorSettingsModule = new FeatureModule(
    (bind, _unbind, isBound) => {
        bind(EditorSettingsManager).toSelf().inSingletonScope();

        const context = { bind, isBound };
        bind(EditorSettingsActionHandler).toSelf().inSingletonScope();
        configureActionHandler(context, UpdateEditorSettingsAction.KIND, EditorSettingsActionHandler);
    },
    { featureId: Symbol("editorSettings") }
);
