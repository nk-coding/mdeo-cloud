import type { Kind } from "typir";
import type { TypirSpecifics } from "typir";
import {
    buildCustomClassIdentifier,
    CustomClassTypeProvider,
    type CustomClassType,
    type CustomClassTypeConstructor
} from "./custom-class-type.js";
import type { ClassType } from "../../config/type.js";
import type { ExtendedTypirServices } from "../../service/extendedTypirServices.js";
import type { CustomValueTypeDetail } from "../custom-value/custom-value-type.js";

/**
 * Type details specific to custom class types.
 *
 * @template Specifics Language-specific types extending TypirSpecifics
 */
export interface CustomClassDetails<Specifics extends TypirSpecifics> extends CustomValueTypeDetail<Specifics> {
    /**
     * The class type definition containing properties, methods, and inheritance information
     */
    definition: ClassType;

    /**
     * Whether this class type is nullable
     */
    isNullable: boolean;
}

/**
 * Factory service interface for creating custom class types.
 *
 * @template Specifics Language-specific types extending TypirSpecifics
 */
export interface CustomClassFactoryService<Specifics extends TypirSpecifics> {
    /**
     * The custom class type constructor
     */
    CustomClassType: CustomClassTypeConstructor;

    /**
     * Get an existing custom class type or create a new one.
     * Uses caching to ensure type identity.
     *
     * @param details The class type details
     * @returns The custom class type instance
     */
    getOrCreate(details: CustomClassDetails<Specifics>): CustomClassType;

    /**
     * Type guard to check if a value is a CustomClassType.
     *
     * @param type The value to check
     * @returns true if the value is a CustomClassType
     */
    isCustomClassType(type: unknown): type is CustomClassType;
}

/**
 * The kind name for custom class types
 */
export const CustomClassKindName = "CustomClass";

/**
 * Kind implementation for custom class types.
 * Manages creation and caching of custom class type instances.
 *
 * @template Specifics Language-specific types extending TypirSpecifics
 */
export class CustomClassKind<Specifics extends TypirSpecifics> implements Kind, CustomClassFactoryService<Specifics> {
    readonly $name: string = CustomClassKindName;

    readonly CustomClassType: CustomClassTypeConstructor;

    /**
     * Creates a new custom class kind.
     * Automatically registers itself with the kind registry.
     *
     * @param services Extended Typir services
     */
    constructor(readonly services: ExtendedTypirServices<Specifics>) {
        services.infrastructure.Kinds.register(this);
        this.CustomClassType = CustomClassTypeProvider(services);
    }

    /**
     * Get an existing custom class type or create a new one.
     * Types are cached by their identifier to ensure type identity.
     *
     * @param details The class type details
     * @returns The custom class type instance
     */
    getOrCreate(details: CustomClassDetails<Specifics>): CustomClassType {
        const key = buildCustomClassIdentifier(details);
        const existingType = this.services.infrastructure.Graph.getType(key);
        if (existingType != undefined) {
            return existingType as CustomClassType;
        } else {
            const newType = new this.CustomClassType(this as unknown as CustomClassKind<TypirSpecifics>, details);
            this.services.infrastructure.Graph.addNode(newType);
            return newType;
        }
    }

    /**
     * Type guard to check if a value is a CustomClassType.
     *
     * @param type The value to check
     * @returns true if the value is a CustomClassType
     */
    isCustomClassType(type: unknown): type is CustomClassType {
        return type instanceof this.CustomClassType;
    }
}
