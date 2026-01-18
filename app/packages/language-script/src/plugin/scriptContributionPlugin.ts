import type { ServerContributionPlugin } from "@mdeo/plugin";

/**
 * Plugin for contributing stdlib and syntax extensions for the script language
 */
export interface ScriptContributionPlugin extends ServerContributionPlugin {
    /**
     * Identifies the plugin as a Script language contribution.
     */
    type: "script-language-contribution";
}
