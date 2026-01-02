import {
    createConnection,
    BrowserMessageReader,
    BrowserMessageWriter,
    type RequestMessage
} from "vscode-languageserver/browser.js";
import * as langium from "langium";
import * as langiumLsp from "langium/lsp";
import * as langiumGrammar from "langium/grammar";
import * as typirLangium from "typir-langium";
import * as typir from "typir";
import * as prettier from "prettier";
import * as glspServer from "@eclipse-glsp/server";
import * as glspProtocol from "@eclipse-glsp/protocol";
import * as inversify from "inversify";
import type { ServerPlugin } from "@/data/plugin/serverPlugin";
import type { DefaultSharedModuleContext } from "langium/lsp";
import { createModule, type LanguagePluginProvider, type PluginContext } from "@mdeo/language-common";
import { type ResolvedServerLanguagePlugin } from "./types";
import { GetPluginsRequest, ServerReadyNotification } from "./protocol";
import type {
    LangiumCoreServices,
    LangiumGeneratedCoreServices,
    LangiumGeneratedSharedCoreServices,
    LangiumSharedCoreServices,
    LanguageMetaData,
    Module
} from "langium";
import { lspFileSystem } from "./lspFileSystem";

const messageReader = new BrowserMessageReader(self);
const messageWriter = new BrowserMessageWriter(self);

const plugins = await requestPluginsFromClient();

/**
 * Requests the plugin configuration from the client using temporary message listening
 */
async function requestPluginsFromClient(): Promise<ServerPlugin[]> {
    return new Promise((resolve) => {
        const requestId = "request/plugins";

        messageReader.listen((message: any) => {
            if (message.id === requestId && message.result !== undefined && message.result.plugins !== undefined) {
                resolve(message.result.plugins);
            }
        });

        const requestMessage: RequestMessage = {
            jsonrpc: "2.0",
            id: requestId,
            method: GetPluginsRequest.method,
            params: {}
        };
        messageWriter.write(requestMessage);
    });
}

const pluginContext: PluginContext = {
    langium,
    "langium/lsp": langiumLsp,
    "langium/grammar": langiumGrammar,
    "typir-langium": typirLangium,
    typir,
    prettier,
    "@eclipse-glsp/server": glspServer,
    "@eclipse-glsp/protocol": glspProtocol,
    inversify
};

const resolvedPlugins: ResolvedServerLanguagePlugin[] = await Promise.all(
    plugins
        .filter((plugin) => plugin.type === "language")
        .map(async (plugin) => {
            const module = await import(/* @vite-ignore */ plugin.import);
            return {
                ...plugin,
                languagePlugin: (module.default as LanguagePluginProvider<any>).generate(pluginContext)
            };
        })
);

const connection = createConnection(messageReader, messageWriter);

function createLanguageServices(context: DefaultSharedModuleContext) {
    const languageModule = createModule(
        pluginContext,
        resolvedPlugins.map((plugin) => plugin.languagePlugin)
    );

    const generatedSharedModule: Module<LangiumSharedCoreServices, LangiumGeneratedSharedCoreServices> = {
        AstReflection: () => languageModule.reflection
    };

    const shared = langium.inject(langiumLsp.createDefaultSharedModule(context), generatedSharedModule);

    for (const plugin of resolvedPlugins) {
        const grammar = languageModule.grammars.get(plugin.languagePlugin)!;
        const languageMetaData: LanguageMetaData = {
            languageId: plugin.languageId,
            fileExtensions: [plugin.extension],
            caseInsensitive: false,
            mode: "development"
        };
        const generatedModule: Module<LangiumCoreServices, LangiumGeneratedCoreServices> = {
            Grammar: () => grammar,
            LanguageMetaData: () => languageMetaData,
            parser: {}
        };
        const services = langium.inject(
            langiumLsp.createDefaultModule({ shared }),
            generatedModule,
            plugin.languagePlugin.module
        );
        plugin.services = services;
    }
    for (const plugin of resolvedPlugins) {
        shared.ServiceRegistry.register(plugin.services!);
    }
    for (const plugin of resolvedPlugins) {
        if (plugin.languagePlugin.postCreate != undefined) {
            plugin.languagePlugin.postCreate(plugin.services!, context);
        }
    }

    return shared;
}

const shared = createLanguageServices({ connection, ...lspFileSystem(connection) });

connection.sendNotification(ServerReadyNotification.method, {});

langiumLsp.startLanguageServer(shared);
