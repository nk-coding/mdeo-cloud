import type { PaletteItem } from "@eclipse-glsp/sprotty";
import type { IconNode } from "lucide";

/**
 * Different types of tools the toolbox provides.
 */
export enum ToolType {
    POINTER = "pointer",
    HAND = "hand",
    MARQUEE = "marquee",
    CREATE_EDGE = "create-edge",
    LAYOUT = "layout",
    BOTTOM_PANEL_TOGGLE = "bottom-panel-toggle"
}

/**
 * Checks if the given tool type is a regular interaction tool.
 * Regular interaction tools do not change how users interact with the canvas.
 *
 * @param toolType The tool type to check
 * @returns True if the tool is a regular interaction tool
 */
export function isRegularInteractionTool(toolType: ToolType): boolean {
    return toolType === ToolType.POINTER;
}

/**
 * A tool definition in the toolbox toolbar.
 */
export interface ToolDefinition {
    /**
     * The unique identifier of the tool
     */
    id: ToolType;
    /**
     * The Lucide icon node to display
     */
    icon: IconNode;
    /**
     * The title/tooltip text for the tool
     */
    title: string;
}

/**
 * Extended palette item with additional toolbox-specific properties.
 */
export interface ToolboxPaletteItem extends PaletteItem {
    /**
     * The group this item belongs to
     */
    group?: string;
    /**
     * Keywords for search functionality
     */
    keywords?: string[];
}

/**
 * Entry in the toolbox edit list with search metadata.
 */
export interface ToolboxEditEntry {
    /**
     * The unique identifier for this edit
     */
    id: string;
    /**
     * The display name
     */
    name: string;
    /**
     * The group this entry belongs to
     */
    group: ToolboxGroupKey;
    /**
     * Keywords for search
     */
    keywords: string[];
    /**
     * The underlying palette item
     */
    paletteItem: PaletteItem;
}

/**
 * Defines a group in the toolbox palette, used for grouping related tools together.
 */
export class ToolboxGroupKey {
    constructor(
        readonly name: string,
        readonly sortString: string
    ) {}
}
