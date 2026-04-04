import type { Action, IActionHandler, ICommand } from "@eclipse-glsp/sprotty";
import { sharedImport } from "../../sharedImport.js";
import { EditorSettingsManager } from "./editorSettingsManager.js";
import { UpdateEditorSettingsAction } from "@mdeo/protocol-common";

const { injectable, inject } = sharedImport("inversify");

/**
 * Client-side action handler for {@link UpdateEditorSettingsAction}.
 *
 * Receives settings pushed from the language server and delegates to
 * {@link EditorSettingsManager} which persists them and notifies all subscribers
 * (e.g. the Toolbox UI extension) so the UI is updated immediately.
 */
@injectable()
export class EditorSettingsActionHandler implements IActionHandler {
    @inject(EditorSettingsManager)
    declare protected readonly settingsManager: EditorSettingsManager;

    handle(action: Action): void | ICommand | Action {
        if (UpdateEditorSettingsAction.is(action)) {
            this.settingsManager.applyFromServer(action.settings);
        }
    }
}
