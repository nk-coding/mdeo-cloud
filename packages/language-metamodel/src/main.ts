import type {
    LangiumCoreServices,
    LangiumGeneratedCoreServices,
    LangiumGeneratedSharedCoreServices,
    LangiumSharedCoreServices,
    LanguageMetaData,
    Module
} from "langium";
import { inject } from "langium";
import {
    createDefaultModule,
    createDefaultSharedModule,
    type DefaultSharedModuleContext,
    type LangiumServices,
    type LangiumSharedServices,
    type PartialLangiumServices
} from "langium/lsp";
import { MetaModelGrammar } from "./language/module.js";

export type MetamodelAddedServices = {};

export type MetamodelServices = LangiumServices & MetamodelAddedServices;

export const MetamodelModule: Module<MetamodelServices, PartialLangiumServices & MetamodelAddedServices> = {};

export const MetamodelLanguageMetaData = {
    languageId: "metamodel",
    fileExtensions: [".metamodel"],
    caseInsensitive: false,
    mode: "development"
} as const satisfies LanguageMetaData;

export const MetamodelGeneratedSharedModule: Module<LangiumSharedCoreServices, LangiumGeneratedSharedCoreServices> = {
    AstReflection: () => MetaModelGrammar.reflection
};

export const MetamodelGeneratedModule: Module<LangiumCoreServices, LangiumGeneratedCoreServices> = {
    Grammar: () => MetaModelGrammar.grammar,
    LanguageMetaData: () => MetamodelLanguageMetaData,
    parser: {}
};

export function createMetamodelServices(context: DefaultSharedModuleContext): {
    shared: LangiumSharedServices;
    Metamodel: MetamodelServices;
} {
    const shared = inject(createDefaultSharedModule(context), MetamodelGeneratedSharedModule);
    const Metamodel = inject(createDefaultModule({ shared }), MetamodelGeneratedModule, MetamodelModule);
    shared.ServiceRegistry.register(Metamodel);
    if (!context.connection) {
        shared.workspace.ConfigurationProvider.initialized({});
    }
    return { shared, Metamodel };
}
