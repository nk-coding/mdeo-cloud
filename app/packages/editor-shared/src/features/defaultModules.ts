import { boundsModule } from "./bounds/featureModule.js";
import { changeBoundsToolModule } from "./change-bounds-tool/featureModule.js";
import { edgeEditToolModule } from "./edge-edit-tool/featureModule.js";
import { edgeRoutingModule } from "./edge-rourting/featureModule.js";
import { editLabelModule } from "./edit-label/featureModule.js";
import { metadataModule } from "./metadata/featureModule.js";
import { moveModule } from "./move/featureModule.js";
import { viewportModule } from "./viewport/featureModule.js";

/**
 * Default modules for the editor shared features.
 * These modules are automatically included in editor configurations and provide:
 * - boundsModule: Custom bounds computation and feedback handling
 * - changeBoundsToolModule: Custom change bounds tool with SVG-based resize handles
 * - edgeEditToolModule: Custom edge editing tool for route manipulation
 * - metadataModule: Metadata handling for diagram elements
 * - editLabelModule: Functionality for editing labels directly on the diagram
 * - edgeRoutingModule: Edge routing capabilities for automatic edge layout
 * - moveModule: Enhanced move command with custom edge morphing animations
 */
export const DEFAULT_MODULES = [
    boundsModule,
    changeBoundsToolModule,
    edgeEditToolModule,
    metadataModule,
    editLabelModule,
    edgeRoutingModule,
    moveModule,
    viewportModule
];
