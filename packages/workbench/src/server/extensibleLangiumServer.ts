import { createConnection, BrowserMessageReader, BrowserMessageWriter } from "vscode-languageserver/browser.js";
import * as langium from "langium";
import * as langiumLsp from "langium/lsp";
import type { ServerLanguagePlugin } from "@/data/plugin/serverPlugin";
import type { DefaultSharedModuleContext } from "langium/lsp";
import { createModule, type LanguagePluginProvider, type PluginContext } from "@mdeo/language-common";
import type { ResolvedServerLanguagePlugin } from "./types";
import type {
    LangiumCoreServices,
    LangiumGeneratedCoreServices,
    LangiumGeneratedSharedCoreServices,
    LangiumSharedCoreServices,
    LanguageMetaData,
    Module
} from "langium";

const messageReader = new BrowserMessageReader(self);
const messageWriter = new BrowserMessageWriter(self);

const connection = createConnection(messageReader, messageWriter);

// TODO generalize this
const plugins: ServerLanguagePlugin[] = [
    {
        type: "language",
        languageId: "metamodel",
        extension: ".mm",
        import: "/modules/defaultPlugin.js"
    }
];

const pluginContext: PluginContext = {
    langium,
    "langium/lsp": langiumLsp
};

const resolvedPlugins: ResolvedServerLanguagePlugin[] = await Promise.all(
    plugins.map(async (plugin) => {
        const module = await import(/* @vite-ignore */ plugin.import);
        return {
            ...plugin,
            languagePlugin: (module.default as LanguagePluginProvider<any>).generate(pluginContext)
        };
    })
);

function createLanguageServices(context: DefaultSharedModuleContext) {
    const languageModule = createModule(resolvedPlugins.map((plugin) => plugin.languagePlugin));

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

const shared = createLanguageServices({ connection, ...langium.EmptyFileSystem });

langiumLsp.startLanguageServer(shared);
