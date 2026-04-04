import type { Action, ActionHandler, MaybePromise } from "@eclipse-glsp/server";
import { sharedImport } from "../../sharedImport.js";
import { UpdateEditorSettingsAction } from "@mdeo/protocol-common";
import { EditorSettingsService } from "../editorSettingsService.js";

const { injectable, inject } = sharedImport("inversify");

/**
 * Server-side handler for {@link UpdateEditorSettingsAction}.
 *
 * Receives editor settings updates sent by the diagram client (e.g., on startup
 * or when the user changes a UI setting) and persists them in the
 * session-scoped {@link EditorSettingsService}.
 */
@injectable()
export class UpdateEditorSettingsActionHandler implements ActionHandler {
    @inject(EditorSettingsService)
    protected readonly settingsService!: EditorSettingsService;

    readonly actionKinds: string[] = [UpdateEditorSettingsAction.KIND];

    execute(action: Action): MaybePromise<Action[]> {
        if (UpdateEditorSettingsAction.is(action)) {
            this.settingsService.setSettings(action.settings);
        }
        return [];
    }
}
