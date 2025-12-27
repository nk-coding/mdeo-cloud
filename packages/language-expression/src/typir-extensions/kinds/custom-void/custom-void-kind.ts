import type { Kind, TypirSpecifics } from "typir";
import { CustomVoidTypeProvider, type CustomVoidType, type CustomVoidTypeConstructor } from "./custom-void-type.js";
import type { ExtendedTypirServices } from "../../service/extendedTypirServices.js";
import type { CustomVoidDetails } from "./custom-void-type.js";

/**
 * Factory service interface for the void type singleton.
 *
 * @template Specifics Language-specific types extending TypirSpecifics
 */
export interface CustomVoidFactoryService {
    /**
     * The custom void type constructor
     */
    CustomVoidType: CustomVoidTypeConstructor;

    /**
     * Get the void type singleton.
     * Always returns the same instance.
     *
     * @returns The void type singleton instance
     */
    getOrCreate(): CustomVoidType;

    /**
     * Type guard to check if a value is a CustomVoidType.
     *
     * @param type The value to check
     * @returns true if the value is a CustomVoidType
     */
    isCustomVoidType(type: unknown): type is CustomVoidType;
}

/**
 * The kind name for the void type
 */
export const CustomVoidKindName = "CustomVoid";

/**
 * Kind implementation for the void type.
 * Manages the void type singleton.
 *
 * @template Specifics Language-specific types extending TypirSpecifics
 */
export class CustomVoidKind<Specifics extends TypirSpecifics> implements Kind, CustomVoidFactoryService {
    readonly $name: string = CustomVoidKindName;

    readonly CustomVoidType: CustomVoidTypeConstructor;

    /**
     * Cached singleton instance of the void type
     */
    private voidTypeInstance: CustomVoidType | undefined = undefined;

    /**
     * Creates a new custom void kind.
     * Automatically registers itself with the kind registry.
     *
     * @param services Extended Typir services
     */
    constructor(readonly services: ExtendedTypirServices<Specifics>) {
        services.infrastructure.Kinds.register(this);
        this.CustomVoidType = CustomVoidTypeProvider(services);
    }

    /**
     * Get the void type singleton.
     * Creates it on first call, then returns the cached instance.
     *
     * @returns The void type singleton instance
     */
    getOrCreate(): CustomVoidType {
        if (this.voidTypeInstance != undefined) {
            return this.voidTypeInstance;
        }

        const existingType = this.services.infrastructure.Graph.getType("void");
        if (existingType != undefined) {
            this.voidTypeInstance = existingType as CustomVoidType;
            return this.voidTypeInstance;
        }

        const details: CustomVoidDetails<Specifics> = {} as CustomVoidDetails<Specifics>;
        const newType = new this.CustomVoidType(this as unknown as CustomVoidKind<TypirSpecifics>, details);
        this.services.infrastructure.Graph.addNode(newType);
        this.voidTypeInstance = newType;
        return newType;
    }

    /**
     * Type guard to check if a value is a CustomVoidType.
     *
     * @param type The value to check
     * @returns true if the value is a CustomVoidType
     */
    isCustomVoidType(type: unknown): type is CustomVoidType {
        return type instanceof this.CustomVoidType;
    }
}
