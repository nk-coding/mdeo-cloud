import type { TypirSpecifics } from "typir";
import type { ClassType, ValueType, FunctionType, GenericTypeRef, BaseClassTypeRef } from "../config/type.js";
import type { CustomFunctionType } from "../kinds/custom-function/custom-function-type.js";
import type { ExtendedTypirServices } from "./extendedTypirServices.js";
import type { CustomValueType } from "../kinds/custom-value/custom-value-type.js";

/**
 * Service for managing and resolving type definitions.
 * This service maintains a registry of all class types and provides lookup functionality.
 * Also handles resolution of type references to Typir custom types.
 */
export interface TypeDefinitionService {
    /**
     * Register class types with the service
     *
     * @param classTypes - Array of class type definitions to register
     */
    registerClassTypes(classTypes: ClassType[]): void;

    /**
     * Look up a class type by its identifier (package.name)
     *
     * @param identifier - The fully qualified identifier (e.g., "builtin.string")
     * @returns The class type definition or undefined if not found
     */
    getClassType(identifier: string): ClassType;

    /**
     * Get all registered class types
     * @returns Array of all registered class types
     */
    getAllClassTypes(): ClassType[];

    /**
     * Resolve a ValueType to a CustomClassType or CustomLambdaType
     *
     * @param type - The value type to resolve
     * @param genericTypeArgs - Optional map of generic type arguments
     * @returns The resolved custom type
     */
    resolveCustomClassOrLambdaType(type: ValueType, genericTypeArgs?: Map<string, CustomValueType>): CustomValueType;

    /**
     * Resolve a FunctionType to a CustomFunctionType
     *
     * @param type - The function type to resolve
     * @param name - The name of the function
     * @param genericTypeArgs - Optional map of generic type arguments
     * @returns The resolved custom function type
     */
    resolveCustomFunctionType(
        type: FunctionType,
        name: string,
        genericTypeArgs?: Map<string, CustomValueType>
    ): CustomFunctionType;

    /**
     * Registers the super types for lambda types
     *
     * @param superTypes the super types to register
     */
    registerLambdaSuperTypes(superTypes: BaseClassTypeRef[]): void;
}

/**
 * Default implementation of the TypeDefinitionService
 */
export class DefaultTypeDefinitionService<Specifics extends TypirSpecifics> implements TypeDefinitionService {
    private readonly typeMap = new Map<string, ClassType>();

    private lambdaSuperTypes: BaseClassTypeRef[] = [];

    constructor(private readonly services: ExtendedTypirServices<Specifics>) {}

    registerClassTypes(classTypes: ClassType[]): void {
        for (const classType of classTypes) {
            const identifier = `${classType.package}.${classType.name}`;
            this.typeMap.set(identifier, classType);
        }
    }

    getAllClassTypes(): ClassType[] {
        return Array.from(this.typeMap.values());
    }

    resolveCustomClassOrLambdaType(type: ValueType, genericTypeArgs?: Map<string, CustomValueType>): CustomValueType {
        if ("generic" in type) {
            return this.resolveGenericTypeRef(type, genericTypeArgs);
        } else if ("type" in type) {
            const classTypeDef = this.getClassType(type.type);

            return this.services.factory.CustomClasses.getOrCreate({
                definition: classTypeDef,
                isNullable: type.isNullable,
                typeArgs: new Map(
                    Array.from(type.typeArgs ?? new Map()).map(([key, typeArg]) => [
                        key,
                        this.resolveCustomClassOrLambdaType(typeArg, genericTypeArgs)
                    ])
                ),
                superTypes: classTypeDef.superTypes ?? []
            });
        } else {
            return this.services.factory.CustomLambdas.getOrCreate({
                definition: type,
                returnType: this.resolveCustomClassOrLambdaType(type.returnType, genericTypeArgs),
                parameterTypes: type.parameters.map((paramType) =>
                    this.resolveCustomClassOrLambdaType(paramType.type, genericTypeArgs)
                ),
                typeArgs: new Map(),
                superTypes: this.lambdaSuperTypes
            });
        }
    }

    getClassType(typeIdentifier: string): ClassType {
        const identifier = typeIdentifier.includes(".") ? typeIdentifier : `builtin.${typeIdentifier}`;

        const classType = this.typeMap.get(identifier);
        if (classType === undefined) {
            throw new Error(`Type '${typeIdentifier}' not found in type definitions`);
        }
        return classType;
    }

    private resolveGenericTypeRef(
        type: GenericTypeRef,
        genericTypeArgs?: Map<string, CustomValueType>
    ): CustomValueType {
        if (genericTypeArgs == undefined || !genericTypeArgs.has(type.generic)) {
            throw new Error(`Unresolved generic type reference: ${type.generic}`);
        }
        const genericType = genericTypeArgs.get(type.generic)!;
        if (type.isNullable != undefined && type.isNullable != genericType.isNullable) {
            if (genericType.isNullable) {
                return genericType.asNonNullable;
            } else {
                return genericType.asNullable;
            }
        } else {
            return genericType;
        }
    }

    resolveCustomFunctionType(
        type: FunctionType,
        name: string,
        genericTypeArgs?: Map<string, CustomValueType>
    ): CustomFunctionType {
        return this.services.factory.CustomFunctions.getOrCreate({
            definition: type,
            name,
            typeArgs: genericTypeArgs ?? new Map()
        });
    }

    registerLambdaSuperTypes(superTypes: BaseClassTypeRef[]): void {
        this.lambdaSuperTypes = superTypes;
    }
}
