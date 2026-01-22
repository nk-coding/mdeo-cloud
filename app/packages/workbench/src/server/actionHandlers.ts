import type { Connection } from "vscode-languageserver/browser.js";
import type { LangiumSharedCoreServices, LangiumCoreServices } from "langium";
import { createActionProtocol } from "@mdeo/language-common";
import type { ActionAdditionalServices, ActionProviderAdditionalServices } from "@mdeo/language-shared";
import type { PluginContext } from "@mdeo/language-common";

/**
 * Language services extended with action support.
 */
type ActionExtendedServices = LangiumCoreServices &
    Partial<ActionAdditionalServices> &
    Partial<ActionProviderAdditionalServices>;

/**
 * Finds language services by language ID from the service registry.
 *
 * @param services The shared Langium services containing the registry
 * @param languageId The language identifier to look up
 * @returns The language services if found, undefined otherwise
 */
function getServicesByLanguageId(
    services: LangiumSharedCoreServices,
    languageId: string
): ActionExtendedServices | undefined {
    const allServices = services.ServiceRegistry.all as readonly ActionExtendedServices[];
    return allServices.find((langServices) => langServices.LanguageMetaData.languageId === languageId);
}

/**
 * Registers action dialog request handlers on the LSP connection.
 * Handles action/start, action/submit, and action/getFileActions requests.
 *
 * @param connection The LSP connection to register handlers on
 * @param services The shared Langium services for accessing the service registry
 * @param pluginContext The plugin context providing protocol dependencies
 */
export function addActionHandlers(
    connection: Connection,
    services: LangiumSharedCoreServices,
    pluginContext: PluginContext
): void {
    const ActionProtocol = createActionProtocol(pluginContext["vscode-languageserver-protocol"]);

    connection.onRequest(ActionProtocol.ActionStartRequest, async (params) => {
        const languageServices = getServicesByLanguageId(services, params.languageId);
        if (languageServices == undefined) {
            throw new Error(`Language services not found for language: ${params.languageId}`);
        }

        const actionService = languageServices.action?.ActionHandler;
        if (actionService == undefined) {
            throw new Error(`Action service not available for language: ${params.languageId}`);
        }

        return await actionService.startAction(params);
    });

    connection.onRequest(ActionProtocol.ActionSubmitRequest, async (params) => {
        const languageId = params.config.languageId;
        const languageServices = getServicesByLanguageId(services, languageId);
        if (languageServices == undefined) {
            throw new Error(`Language services not found for language: ${languageId}`);
        }

        const actionService = languageServices.action?.ActionHandler;
        if (actionService == undefined) {
            throw new Error(`Action service not available for language: ${languageId}`);
        }

        return await actionService.submitAction(params);
    });

    connection.onRequest(ActionProtocol.GetFileActionsRequest, async (params) => {
        const languageServices = getServicesByLanguageId(services, params.languageId);
        if (languageServices == undefined) {
            return { actions: [] };
        }

        const actionProvider = languageServices.action?.ActionProvider;
        if (actionProvider == undefined) {
            return { actions: [] };
        }

        return await actionProvider.getFileActions(params);
    });
}
