import { boundsModule } from "./bounds/featureModule.js";
import { changeBoundsToolModule } from "./change-bounds-tool/featureModule.js";
import { edgeEditToolModule } from "./edge-edit-tool/featureModule.js";
import { edgeRoutingModule } from "./edge-rourting/featureModule.js";
import { elementFinderModule } from "./element-finder/featureModule.js";
import { editLabelModule } from "./edit-label/featureModule.js";
import { metadataModule } from "./metadata/featureModule.js";
import { moveModule } from "./move/featureModule.js";
import { pointerToolModule } from "./pointer-tool/featureModule.js";
import { reconnectEdgeModule } from "./reconnect-edge/featureModule.js";
import { viewportModule } from "./viewport/featureModule.js";
import { toolboxModule } from "./toolbox/featureModule.js";

/**
 * Default modules for the editor shared features.
 * These modules are automatically included in editor configurations and provide:
 * - boundsModule: Custom bounds computation and feedback handling
 * - changeBoundsToolModule: Custom change bounds tool with SVG-based resize handles
 * - edgeEditToolModule: Custom edge editing tool for route manipulation
 * - elementFinderModule: Element finding from DOM elements or coordinates
 * - metadataModule: Metadata handling for diagram elements
 * - editLabelModule: Functionality for editing labels directly on the diagram
 * - edgeRoutingModule: Edge routing capabilities for automatic edge layout
 * - moveModule: Enhanced move command with custom edge morphing animations
 * - pointerToolModule: Pointer event handling and capture support
 * - reconnectEdgeModule: Reconnect edge operation support with metadata updates
 * - toolboxModule: Toolbox UI for tool selection and element palette
 */
export const DEFAULT_MODULES = [
    boundsModule,
    changeBoundsToolModule,
    edgeEditToolModule,
    elementFinderModule,
    metadataModule,
    editLabelModule,
    edgeRoutingModule,
    moveModule,
    pointerToolModule,
    reconnectEdgeModule,
    viewportModule,
    toolboxModule
];
