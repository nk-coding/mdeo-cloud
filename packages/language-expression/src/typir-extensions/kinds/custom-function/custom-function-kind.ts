import type { Kind } from "typir";
import type { TypirSpecifics, TypeDetails } from "typir";
import type { FunctionType } from "../../config/type.js";
import {
    CustomFunctionTypeProvider,
    type CustomFunctionType,
    type CustomFunctionTypeConstructor,
} from "./custom-function-type.js";
import type { ExtendedTypirServices } from "../../service/extendedTypirServices.js";
import type { CustomValueType } from "../custom-value/custom-value-type.js";

/**
 * Type details specific to custom function types.
 *
 * @template Specifics Language-specific types extending TypirSpecifics
 */
export interface CustomFunctionDetails<Specifics extends TypirSpecifics> extends TypeDetails<Specifics> {
    /**
     * The function type definition containing signatures
     */
    definition: FunctionType;

    /**
     * The name of the function
     */
    name: string;

    /**
     * Map of generic type parameter names to their resolved concrete types
     */
    typeArgs: Map<string, CustomValueType>;
}

/**
 * Factory service interface for creating custom function types.
 *
 * @template Specifics Language-specific types extending TypirSpecifics
 */
export interface CustomFunctionFactoryService<Specifics extends TypirSpecifics> {
    /**
     * The custom function type constructor
     */
    CustomFunctionType: CustomFunctionTypeConstructor;

    /**
     * Get an existing custom function type or create a new one.
     * Uses caching to ensure type identity.
     *
     * @param details The function type details
     * @returns The custom function type instance
     */
    create(details: CustomFunctionDetails<Specifics>): CustomFunctionType;

    /**
     * Type guard to check if a value is a CustomFunctionType.
     *
     * @param type The value to check
     * @returns true if the value is a CustomFunctionType
     */
    isCustomFunctionType(type: unknown): type is CustomFunctionType;
}

/**
 * The kind name for custom function types
 */
export const CustomFunctionKindName = "CustomFunction";

/**
 * Kind implementation for custom function types.
 * Manages creation and caching of custom function type instances.
 *
 * @template Specifics Language-specific types extending TypirSpecifics
 */
export class CustomFunctionKind<Specifics extends TypirSpecifics>
    implements Kind, CustomFunctionFactoryService<Specifics>
{
    readonly $name: string = CustomFunctionKindName;

    readonly CustomFunctionType: CustomFunctionTypeConstructor;

    /**
     * Creates a new custom function kind.
     * Automatically registers itself with the kind registry.
     *
     * @param services Extended Typir services
     */
    constructor(readonly services: ExtendedTypirServices<Specifics>) {
        services.infrastructure.Kinds.register(this);
        this.CustomFunctionType = CustomFunctionTypeProvider(services);
    }

    /**
     * Get an existing custom function type or create a new one.
     * Types are cached by their identifier to ensure type identity.
     *
     * @param details The function type details
     * @returns The custom function type instance
     */
    create(details: CustomFunctionDetails<Specifics>): CustomFunctionType {
        return new this.CustomFunctionType(this as unknown as CustomFunctionKind<TypirSpecifics>, details);
    }

    /**
     * Type guard to check if a value is a CustomFunctionType.
     *
     * @param type The value to check
     * @returns true if the value is a CustomFunctionType
     */
    isCustomFunctionType(type: unknown): type is CustomFunctionType {
        return type instanceof this.CustomFunctionType;
    }
}
