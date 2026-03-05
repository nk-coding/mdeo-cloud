import { sharedImport } from "../../sharedImport.js";
import type { ToolDefinition } from "./toolboxTypes.js";
import { ToolType } from "./toolboxTypes.js";
import type { Toolbox } from "./toolbox.js";

const { MousePointer, Hand, SquareDashedMousePointer, ArrowUpRight, WandSparkles, PanelBottom } =
    sharedImport("lucide");

/**
 * The available tools in the toolbox toolbar.
 */
export const toolboxTools: ToolDefinition[] = [
    {
        id: ToolType.HAND,
        icon: Hand,
        title: "Hand (H)"
    },
    {
        id: ToolType.POINTER,
        icon: MousePointer,
        title: "Select (V)"
    },
    {
        id: ToolType.MARQUEE,
        icon: SquareDashedMousePointer,
        title: "Marquee Select"
    },
    {
        id: ToolType.CREATE_EDGE,
        icon: ArrowUpRight,
        title: "Create Edge"
    },
    {
        id: ToolType.LAYOUT,
        icon: WandSparkles,
        title: "Auto Layout"
    },
    {
        id: ToolType.BOTTOM_PANEL_TOGGLE,
        icon: PanelBottom,
        title: "Toggle Bottom Panel"
    }
];

/**
 * Enables the specified tool in the toolbox.
 *
 * @param context The toolbox context
 * @param tool The tool type to enable
 */
export function enableTool(context: Toolbox, tool: ToolType): void {
    if (context.toolType !== tool) {
        context.updateTool(tool);
    }
}
