import { sharedImport } from "../../sharedImport.js";
import { ToolType, isCreationTool } from "../toolbox/toolboxTypes.js";

const { injectable } = sharedImport("inversify");

/**
 * Central, injectable singleton that exposes the currently active toolbox tool.
 *
 * Maintained by {@link Toolbox} and queried by any component that needs to adapt its
 * behaviour based on the active tool — for example {@link ContextActionsUIExtension}
 * suppresses context rails while a creation tool is active.
 */
@injectable()
export class ToolStateManager {
    private _activeTool: ToolType = ToolType.POINTER;

    /**
     * Returns the currently active tool type.
     *
     * @returns The active {@link ToolType}.
     */
    getActiveTool(): ToolType {
        return this._activeTool;
    }

    /**
     * Updates the active tool.  Called by {@link Toolbox} whenever the tool changes.
     *
     * @param tool The newly active tool type.
     */
    setActiveTool(tool: ToolType): void {
        this._activeTool = tool;
    }

    /**
     * Returns `true` while a node- or edge-creation tool is active.
     * Components can use this to suppress contextual overlays and other UI
     * elements that would distract from the creation workflow.
     *
     * @returns Whether the active tool is a creation tool.
     */
    isCreationMode(): boolean {
        return isCreationTool(this._activeTool);
    }
}
