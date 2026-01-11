import type { TypirLangiumSpecifics } from "typir-langium";
import type {
    AdditionalTypirServices,
    ExtendedTypirLangiumServices
} from "../typir-extensions/service/extendedTypirServices.js";
import type { ExtendedLangiumSharedServices } from "@mdeo/language-common";

/**
 * Additional services specific to the expression language,
 * extending both TypirLangium and custom Typir services.
 *
 * @template Specifics Language-specific types extending TypirLangiumSpecifics
 */
export type ExpressionAddedServices<Specifics extends TypirLangiumSpecifics> = AdditionalTypirServices<Specifics> & {
    readonly langium: {
        readonly LangiumServices: ExtendedLangiumSharedServices;
    };
};

/**
 * Complete set of Typir services for the expression language,
 * combining base Typir services, TypirLangium services,
 * and additional expression-specific services.
 *
 * @template Specifics Language-specific types extending TypirLangiumSpecifics
 */
export type ExpressionTypirServices<Specifics extends TypirLangiumSpecifics> = ExtendedTypirLangiumServices<Specifics> &
    ExpressionAddedServices<Specifics>;
