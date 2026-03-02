import type { TypirServices, TypirSpecifics } from "typir";
import type { CustomClassFactoryService } from "../kinds/custom-class/custom-class-kind.js";
import type { CustomLambdaFactoryService } from "../kinds/custom-lambda/custom-lambda-kind.js";
import type { CustomFunctionFactoryService } from "../kinds/custom-function/custom-function-kind.js";
import type { CustomValueFactoryService } from "../kinds/custom-value/custom-value-kind.js";
import type { CustomVoidFactoryService } from "../kinds/custom-void/custom-void-kind.js";
import type { CustomNullFactoryService } from "../kinds/custom-null/custom-null-kind.js";
import type { TypeDefinitionService } from "./typeDefinitionService.js";
import type { ScopeProviderCaching } from "../scope/scopeProviderCache.js";
import type { ScopeProvider } from "../scope/scopeProvider.js";
import type { TypirLangiumAddedServices, TypirLangiumSpecifics } from "typir-langium";

/**
 * Additional services extending the base Typir services.
 * Provides factory services for custom types and type definition management.
 *
 * @template Specifics Language-specific types extending TypirSpecifics
 */
export interface AdditionalTypirServices<Specifics extends TypirSpecifics> {
    /**
     * Factory services for creating custom type instances
     */
    readonly factory: {
        /**
         * Factory for creating and retrieving custom class types
         */
        readonly CustomClasses: CustomClassFactoryService<Specifics>;

        /**
         * Factory for creating and retrieving custom lambda types
         */
        readonly CustomLambdas: CustomLambdaFactoryService<Specifics>;

        /**
         * Factory for creating and retrieving custom function types
         */
        readonly CustomFunctions: CustomFunctionFactoryService<Specifics>;

        /**
         * Factory for creating and retrieving custom value types
         */
        readonly CustomValues: CustomValueFactoryService;

        /**
         * Factory for the void type singleton
         */
        readonly CustomVoid: CustomVoidFactoryService;

        /**
         * Factory for the null type singleton
         */
        readonly CustomNull: CustomNullFactoryService;
    };

    /**
     * Caching services
     */
    readonly caching: {
        /**
         * Cache for scope providers to optimize scope resolution
         */
        readonly ScopeProvider: ScopeProviderCaching<Specifics>;
    };

    /**
     * Service for managing and resolving type definitions
     */
    readonly TypeDefinitions: TypeDefinitionService;

    /**
     * Scope provider service for resolving scopes of language nodes
     */
    readonly ScopeProvider: ScopeProvider<Specifics>;
}

/**
 * Extended Typir services combining base Typir services with additional custom services.
 * This type represents the complete set of services available in the extended type system.
 *
 * @template Specifics Language-specific types extending TypirSpecifics
 */
export type ExtendedTypirServices<Specifics extends TypirSpecifics> = TypirServices<Specifics> &
    AdditionalTypirServices<Specifics>;

/**
 * Extended Typir Langium services combining extended Typir services with Typir Langium added services.
 */
export type ExtendedTypirLangiumServices<Specifics extends TypirLangiumSpecifics> = ExtendedTypirServices<Specifics> &
    TypirLangiumAddedServices<Specifics>;
