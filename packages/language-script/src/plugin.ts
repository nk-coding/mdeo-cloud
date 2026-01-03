import { HIDDEN_NEWLINE, ML_COMMENT, SL_COMMENT, WS, type LanguagePlugin } from "@mdeo/language-common";
import type { AstSerializerAdditionalServices } from "@mdeo/language-shared";
import {
    IdValueConverter,
    NewlineAwareTokenBuilder,
    DefaultAstSerializer,
    SerializerFormatter,
    registerDefaultTokenSerializers,
    sharedImport
} from "@mdeo/language-shared";
import { ScriptRule } from "./grammar/rule.js";
import {
    defaultExtendedTypirServices,
    type AdditionalTypirServices,
    type ExpressionTypirServices,
    registerExpressionSerializers,
    registerStatementSerializers,
    registerTypeSerializers
} from "@mdeo/language-expression";
import { type TypirLangiumSpecifics } from "typir-langium";
import { ScriptTypeSystem } from "./features/typeSystem.js";
import { ScriptScopeProvider } from "./features/scopeProvider.js";
import { registerScriptSerializers } from "./features/scriptSerializers.js";
import { expressionTypes, statementTypes, typeTypes } from "./grammar/types.js";

const { createTypirLangiumServicesWithAdditionalServices, initializeLangiumTypirServices } =
    sharedImport("typir-langium");

/**
 * The Typir specifics for the Script language.
 */
export type ScriptTypirSpecifics = TypirLangiumSpecifics;

/**
 * The additional services for the Script language.
 */
export type ScriptServices = {
    typir: ExpressionTypirServices<ScriptTypirSpecifics>;
} & AstSerializerAdditionalServices;

/**
 * The plugin for the Script language.
 */
export const scriptPlugin: LanguagePlugin<ScriptServices> = {
    rootRule: ScriptRule,
    additionalTerminals: [WS, HIDDEN_NEWLINE, ML_COMMENT, SL_COMMENT],
    module: {
        parser: {
            TokenBuilder: () => new NewlineAwareTokenBuilder(new Set(["{"]), new Set(["("]), new Set(["}", ")"])),
            ValueConverter: () => new IdValueConverter()
        },
        typir: (services) =>
            createTypirLangiumServicesWithAdditionalServices<
                ScriptTypirSpecifics,
                AdditionalTypirServices<ScriptTypirSpecifics>
            >(
                services.shared as any,
                services.shared.AstReflection as any,
                new ScriptTypeSystem(),
                defaultExtendedTypirServices<ScriptTypirSpecifics>(),
                {
                    ScopeProvider: (services) =>
                        new ScriptScopeProvider(services as ExpressionTypirServices<ScriptTypirSpecifics>)
                }
            ) as ExpressionTypirServices<ScriptTypirSpecifics>,
        lsp: {
            Formatter: (services) => new SerializerFormatter(services)
        },
        AstSerializer: (services) => new DefaultAstSerializer(services)
    },
    postCreate(services) {
        initializeLangiumTypirServices(services as any, services.typir);
        registerDefaultTokenSerializers(services);
        registerTypeSerializers(services, typeTypes);
        registerExpressionSerializers(services, expressionTypes);
        registerStatementSerializers(services, statementTypes);
        registerScriptSerializers(services);
    }
};
