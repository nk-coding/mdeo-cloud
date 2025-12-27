import type { Kind, TypirSpecifics } from "typir";
import { CustomNullTypeProvider, type CustomNullType, type CustomNullTypeConstructor } from "./custom-null-type.js";
import type { ExtendedTypirServices } from "../../service/extendedTypirServices.js";
import type { CustomNullDetails } from "./custom-null-type.js";
import type { CustomValueType } from "../custom-value/custom-value-type.js";

/**
 * Factory service interface for the null type singleton.
 *
 * @template Specifics Language-specific types extending TypirSpecifics
 */
export interface CustomNullFactoryService {
    /**
     * The custom null type constructor
     */
    CustomNullType: CustomNullTypeConstructor;

    /**
     * Get the null type singleton.
     * Always returns the same instance.
     *
     * @returns The null type singleton instance
     */
    getOrCreate(): CustomNullType;

    /**
     * Type guard to check if a value is a CustomNullType.
     *
     * @param type The value to check
     * @returns true if the value is a CustomNullType
     */
    isCustomNullType(type: unknown): type is CustomNullType;
}

/**
 * The kind name for the null type
 */
export const CustomNullKindName = "CustomNull";

/**
 * Kind implementation for the null type.
 * Manages the null type singleton.
 *
 * @template Specifics Language-specific types extending TypirSpecifics
 */
export class CustomNullKind<Specifics extends TypirSpecifics> implements Kind, CustomNullFactoryService {
    readonly $name: string = CustomNullKindName;

    readonly CustomNullType: CustomNullTypeConstructor;

    /**
     * Cached singleton instance of the null type
     */
    private nullTypeInstance: CustomNullType | undefined = undefined;

    /**
     * Creates a new custom null kind.
     * Automatically registers itself with the kind registry.
     *
     * @param services Extended Typir services
     */
    constructor(readonly services: ExtendedTypirServices<Specifics>) {
        services.infrastructure.Kinds.register(this);
        this.CustomNullType = CustomNullTypeProvider(services);
    }

    /**
     * Get the null type singleton.
     * Creates it on first call, then returns the cached instance.
     *
     * @returns The null type singleton instance
     */
    getOrCreate(): CustomNullType {
        if (this.nullTypeInstance != undefined) {
            return this.nullTypeInstance;
        }

        const existingType = this.services.infrastructure.Graph.getType("null");
        if (existingType != undefined) {
            this.nullTypeInstance = existingType as CustomNullType;
            return this.nullTypeInstance;
        }

        const details: CustomNullDetails<Specifics> = {
            typeArgs: new Map<string, CustomValueType>(),
            superTypes: []
        };
        const newType = new this.CustomNullType(this as unknown as CustomNullKind<TypirSpecifics>, details);
        this.services.infrastructure.Graph.addNode(newType);
        this.nullTypeInstance = newType;
        return newType;
    }

    /**
     * Type guard to check if a value is a CustomNullType.
     *
     * @param type The value to check
     * @returns true if the value is a CustomNullType
     */
    isCustomNullType(type: unknown): type is CustomNullType {
        return type instanceof this.CustomNullType;
    }
}
