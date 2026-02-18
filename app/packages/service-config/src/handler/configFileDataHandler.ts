import { ConfigContributionPlugin, getWrapperInterfaceName } from "@mdeo/language-config";
import type {
    ConfigAdditionalServices,
    ConfigPluginDependencyData,
    ConfigPluginRequestBody,
    ConfigType
} from "@mdeo/language-config";
import type { FileDataHandler } from "@mdeo/service-common";
import type { AstNode } from "langium";

/**
 * The key for the configuration file-data handler.
 * Plugin request handlers in contribution plugins must use the same key.
 */
export const CONFIG_DATA_KEY = "config";

export type { ConfigPluginDependencyData, ConfigPluginRequestBody } from "@mdeo/language-config";

/**
 * The overall data returned by the config file-data handler.
 * Keys are plugin short names, values are the data returned by that plugin's request handler
 * (typically a mapping from section name to section-specific data).
 */
export type ConfigFileData = Record<string, Record<string, unknown>>;

/**
 * Sorts ConfigContributionPlugins in topological order based on their sectionDependencies.
 * Plugins whose sections are required by others are placed first.
 *
 * @param plugins All active ConfigContributionPlugins
 * @returns Plugins sorted so that dependencies are processed before dependents
 */
function sortBySectionDependencies(plugins: ConfigContributionPlugin[]): ConfigContributionPlugin[] {
    const pluginByName = new Map(plugins.map((plugin) => [plugin.shortName, plugin]));
    const sorted: ConfigContributionPlugin[] = [];
    const visited = new Set<string>();
    const visiting = new Set<string>();

    function visit(pluginName: string): void {
        if (visited.has(pluginName)) return;
        if (visiting.has(pluginName)) {
            throw new Error(`Circular sectionDependency detected involving plugin: ${pluginName}`);
        }

        visiting.add(pluginName);
        const plugin = pluginByName.get(pluginName);
        if (plugin != undefined) {
            const dependencyPluginNames = new Set(plugin.sectionDependencies.map((d) => d.pluginName));
            for (const depName of dependencyPluginNames) {
                if (pluginByName.has(depName)) {
                    visit(depName);
                }
            }
            visited.add(pluginName);
            visiting.delete(pluginName);
            sorted.push(plugin);
        }
    }

    for (const plugin of plugins) {
        visit(plugin.shortName);
    }

    return sorted;
}

/**
 * File-data handler that computes structured data for all contribution plugin sections
 * in a config file.
 *
 * Algorithm:
 * 1. Parses the config file and extracts section nodes per plugin.
 * 2. Processes plugins in topological order based on sectionDependencies.
 * 3. For each plugin:
 *    a. Builds a partial config text containing only that plugin's sections,
 *       using simple (non-qualified) section keywords.
 *    b. Sends a plugin request to the plugin's language service via the backend proxy,
 *       including previously computed dependency data.
 *    c. Collects the plugin's result (a section-name → data map).
 * 4. Returns a map of plugin short name → plugin result.
 */
export const configDataHandler: FileDataHandler<ConfigFileData, ConfigAdditionalServices> = async (context) => {
    const { fileInfo, instance, contributionPlugins, serverApi } = context;

    if (fileInfo == undefined) {
        return { data: {}, fileDependencies: [], dataDependencies: [] };
    }

    const document = await instance.buildDocument(fileInfo.uri);
    const config = document.parseResult.value as ConfigType | undefined;
    if (config == undefined || !Array.isArray(config.sections)) {
        return { data: {}, fileDependencies: [], dataDependencies: [] };
    }

    const activeConfigPlugins = contributionPlugins.filter(ConfigContributionPlugin.is);

    const sortedPlugins = sortBySectionDependencies(activeConfigPlugins);

    const sectionLookup = new Map(config.sections.map((section) => [section.$type, section]));

    const result: ConfigFileData = {};

    for (const plugin of sortedPlugins) {
        const partialLines: string[] = [];
        if (plugin.sections.length === 0) {
            continue;
        }
        for (const pluginSection of plugin.sections) {
            const section = sectionLookup.get(getWrapperInterfaceName(pluginSection.name, plugin.shortName));
            if (section == undefined) {
                continue;
            }
            const content = (section as AstNode & { content: AstNode }).content;
            const contentText: string = content?.$cstNode?.text ?? "";
            partialLines.push(`${pluginSection.name} ${contentText}`);
        }
        const partialText = partialLines.join("\n");

        const dependencyData: ConfigPluginDependencyData = {};
        for (const dep of plugin.sectionDependencies) {
            const depPluginData = result[dep.pluginName];
            if (depPluginData != undefined && dep.sectionName in depPluginData) {
                if (dependencyData[dep.pluginName] == undefined) {
                    dependencyData[dep.pluginName] = {};
                }
                dependencyData[dep.pluginName][dep.sectionName] = depPluginData[dep.sectionName];
            }
        }

        const requestBody: ConfigPluginRequestBody = {
            dependencyData,
            text: partialText,
            configFileUri: fileInfo.uri.toString()
        };

        const pluginResult = (await serverApi.sendPluginRequest(
            plugin.languageKey,
            CONFIG_DATA_KEY,
            requestBody
        )) as Record<string, unknown>;

        result[plugin.shortName] = pluginResult;
    }

    const trackedRequests = serverApi.getTrackedRequests();
    return {
        data: result,
        fileDependencies: trackedRequests.fileDependencies,
        dataDependencies: trackedRequests.dataDependencies
    };
};
