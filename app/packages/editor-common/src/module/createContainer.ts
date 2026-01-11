import type { Container, ContainerModule } from "inversify";
import type { PluginContext } from "../plugin/pluginContext.js";
import type { ContainerConfiguration, IDiagramOptions } from "@eclipse-glsp/client";

/**
 * Creates a glsp client container module from the given editor plugin
 *
 * @param context the plugin context with dependencies
 * @param plugin the container configuration to apply
 * @param options the diagram options to use
 * @returns the created container module
 */
export function createContainer(
    context: PluginContext,
    plugin: ContainerConfiguration,
    options: IDiagramOptions
): Container {
    const { "@eclipse-glsp/client": glspClient, inversify } = context;
    const Container = inversify.Container;

    return glspClient.initializeDiagramContainer(
        new Container() as any,
        glspClient.createDiagramOptionsModule(options),
        createDefaultContainerModule(context),
        glspClient.standaloneDefaultModule,
        glspClient.gridModule,
        ...plugin
    ) as unknown as Container;
}

/**
 * Creates the default container module for the editor, handling
 * - logging
 */
function createDefaultContainerModule(context: PluginContext): ContainerModule {
    const { "@eclipse-glsp/sprotty": glspSprotty, inversify } = context;
    const ContainerModule = inversify.ContainerModule;

    return new ContainerModule((bind, unbind, isBound, rebind) => {
        const bindContext = { bind, unbind, isBound, rebind };
        glspSprotty
            .bindOrRebind(bindContext, glspSprotty.TYPES.ILogger)
            .to(glspSprotty.ConsoleLogger)
            .inSingletonScope();
    });
}
