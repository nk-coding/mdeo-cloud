import { Container } from "inversify";
import type { EditorPlugin } from "../plugin/editorPlugin.js";
import * as glspClient from "@eclipse-glsp/client";
import * as glspSprotty from "@eclipse-glsp/sprotty";
import * as inversify from "inversify";
import { initializeDiagramContainer, createDiagramOptionsModule, gridModule } from "@eclipse-glsp/client";
import { defaultContainerModule } from "./defaultContainerModule.js";

/**
 * Creates a glsp client container module from the given editor plugin
 *
 * @param plugin the editor plugin
 * @returns the created container module
 */
export function createContainer(plugin: EditorPlugin, options: glspClient.IDiagramOptions): Container {
    return initializeDiagramContainer(
        new Container() as any,
        createDiagramOptionsModule(options),
        defaultContainerModule,
        gridModule,
        ...plugin.configure({
            "@eclipse-glsp/client": glspClient,
            "@eclipse-glsp/sprotty": glspSprotty,
            inversify: inversify
        })
    ) as any;
}
