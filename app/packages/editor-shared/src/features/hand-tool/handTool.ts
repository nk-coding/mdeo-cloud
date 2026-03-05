import { sharedImport } from "../../sharedImport.js";

const { injectable } = sharedImport("inversify");
const { BaseTool, ModifyCSSFeedbackAction, CursorCSS } = sharedImport("@eclipse-glsp/client");

export const GRAB_CURSOR_CSS = "grab-mode";

/**
 * Hand tool that enables viewport panning by dragging over any element.
 * While active, all other interactive features (node dragging, edge editing, etc.)
 * are automatically disabled because this is registered as TYPES.ITool.
 * The tool remains active until the user explicitly switches to another tool.
 *
 * Actual panning is handled by EditorScrollMouseListener (scrollAlways mode).
 * Selection is suppressed by HandAwareSelectMouseListener.
 */
@injectable()
export class HandTool extends BaseTool {
    static readonly ID = "mdeo.hand-tool";

    get id(): string {
        return HandTool.ID;
    }

    /**
     * The hand tool works in both edit and readonly mode.
     */
    get isEditTool(): boolean {
        return false;
    }

    enable(): void {
        this.toDisposeOnDisable.push(
            this.createFeedbackEmitter()
                .add(
                    ModifyCSSFeedbackAction.create({
                        add: [GRAB_CURSOR_CSS],
                        remove: Object.values(CursorCSS)
                    }),
                    ModifyCSSFeedbackAction.create({
                        remove: [...Object.values(CursorCSS), GRAB_CURSOR_CSS]
                    })
                )
                .submit()
        );
    }
}
