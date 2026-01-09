import { boundsModule } from "./bounds/featureModule.js";
import { changeBoundsToolModule } from "./change-bounds-tool/featureModule.js";
import { metadataModule } from "./metadata/featureModule.js";

/**
 * Default modules for the editor shared features.
 * These modules are automatically included in editor configurations and provide:
 * - boundsModule: Custom bounds computation and feedback handling
 * - changeBoundsToolModule: Custom change bounds tool with SVG-based resize handles
 */
export const DEFAULT_MODULES = [boundsModule, changeBoundsToolModule, metadataModule];
