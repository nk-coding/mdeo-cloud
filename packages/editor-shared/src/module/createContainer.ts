import { Container } from "inversify";
import {
    initializeDiagramContainer,
    createDiagramOptionsModule,
    gridModule,
    type IDiagramOptions,
    type ContainerConfiguration
} from "@eclipse-glsp/client";
import { defaultContainerModule } from "./defaultContainerModule.js";

/**
 * Creates a glsp client container module from the given editor plugin
 *
 * @param plugin the container configuration to apply
 * @param options the diagram options to use
 * @returns the created container module
 */
export function createContainer(plugin: ContainerConfiguration, options: IDiagramOptions): Container {
    return initializeDiagramContainer(
        new Container() as any,
        createDiagramOptionsModule(options),
        defaultContainerModule,
        gridModule,
        ...plugin
    ) as any;
}
