import { sharedImport } from "../../sharedImport.js";
import { ToolType } from "./toolboxTypes.js";

const { injectable } = sharedImport("inversify");

/**
 * Provider interface for accessing the current tool type.
 */
export interface ToolTypeProvider {
    /**
     * The currently selected tool type
     */
    readonly toolType: ToolType;
}

/**
 * Injectable class that manages the current tool state in the toolbox.
 * Tracks the currently selected tool and whether it's locked.
 */
@injectable()
export class ToolState implements ToolTypeProvider {
    /**
     * The current tool type
     */
    toolType: ToolType = ToolType.POINTER;

    /**
     * Whether the current tool is locked (stays active after use)
     */
    isLocked: boolean = false;

    /**
     * Resets the tool state to defaults.
     */
    reset(): void {
        this.toolType = ToolType.POINTER;
        this.isLocked = false;
    }
}
