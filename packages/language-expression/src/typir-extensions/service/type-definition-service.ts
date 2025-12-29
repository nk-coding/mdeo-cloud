import type { TypirSpecifics } from "typir";
import type { ClassType, ValueType, FunctionType, BaseClassTypeRef } from "../config/type.js";
import { ClassTypeRef, GenericTypeRef, VoidType } from "../config/type.js";
import type { CustomFunctionType } from "../kinds/custom-function/custom-function-type.js";
import type { ExtendedTypirServices } from "./extendedTypirServices.js";
import type { CustomValueType } from "../kinds/custom-value/custom-value-type.js";

/**
 * Listener interface for receiving notifications about class type changes.
 * Implement this interface to react to class type additions and removals.
 */
export type TypeDefinitionListener = Partial<{
    /**
     * Called when a new class type is added to the registry
     * @param classType - The class type that was added
     */
    onAddedClassType: (classType: ClassType) => void;

    /**
     * Called when a class type is removed from the registry
     * @param classType - The class type that was removed
     */
    onRemovedClassType: (classType: ClassType) => void;
}>;

/**
 * Service for managing and resolving type definitions.
 * This service maintains a registry of all class types and provides lookup functionality.
 * Also handles resolution of type references to Typir custom types.
 */
export interface TypeDefinitionService {
    /**
     * Add a single class type to the registry.
     * Throws an error if a type with the same identifier already exists.
     *
     * @param classType - The class type definition to register
     * @throws Error if a type with the same identifier (package.name) already exists
     */
    addClassType(classType: ClassType): void;

    /**
     * Remove a class type from the registry by its identifier.
     * If the type doesn't exist, this method does nothing.
     *
     * @param identifier - The fully qualified identifier (e.g., "builtin.string")
     */
    removeClassType(classType: ClassType): void;

    /**
     * Look up a class type by its identifier (package.name).
     * Throws an error if the type is not found.
     *
     * @param identifier - The fully qualified identifier (e.g., "builtin.string")
     * @returns The class type definition
     * @throws Error if the type is not found
     */
    getClassType(identifier: string): ClassType;

    /**
     * Look up a class type by its identifier (package.name) if it exists.
     *
     * @param identifier - The fully qualified identifier (e.g., "builtin.string")
     * @returns The class type definition or undefined if not found
     */
    getClassTypeIfExisting(identifier: string): ClassType | undefined;

    /**
     * Get all registered class types.
     *
     * @returns Array of all registered class types
     */
    getAllClassTypes(): ClassType[];

    /**
     * Resolve a ValueType to a CustomClassType or CustomLambdaType.
     * This method handles generic type resolution and creates appropriate Typir custom types.
     *
     * @param type - The value type to resolve
     * @param genericTypeArgs - Optional map of generic type arguments for resolution
     * @returns The resolved custom type
     * @throws Error if a generic type cannot be resolved
     */
    resolveCustomClassOrLambdaType(type: ValueType, genericTypeArgs?: Map<string, CustomValueType>): CustomValueType;

    /**
     * Resolve a FunctionType to a CustomFunctionType.
     *
     * @param type - The function type to resolve
     * @param name - The name of the function
     * @param genericTypeArgs - Optional map of generic type arguments for resolution
     * @returns The resolved custom function type
     */
    resolveCustomFunctionType(
        type: FunctionType,
        name: string,
        genericTypeArgs?: Map<string, CustomValueType>
    ): CustomFunctionType;

    /**
     * Register the super types for lambda types.
     * These super types will be applied to all lambda type instances.
     *
     * @param superTypes - The super types to register for lambda types
     */
    registerLambdaSuperTypes(superTypes: BaseClassTypeRef[]): void;

    /**
     * Get the registered super types for lambda types.
     *
     * @returns The super types for lambda types
     */
    getLambdaSuperTypes(): BaseClassTypeRef[];

    /**
     * Add a listener to receive notifications about class type changes.
     *
     * @param listener - The listener to add
     */
    addListener(listener: TypeDefinitionListener): void;

    /**
     * Remove a previously added listener.
     *
     * @param listener - The listener to remove
     */
    removeListener(listener: TypeDefinitionListener): void;
}

/**
 * Default implementation of the TypeDefinitionService.
 * Manages a registry of class types and notifies listeners of changes.
 */
export class DefaultTypeDefinitionService<Specifics extends TypirSpecifics> implements TypeDefinitionService {
    /** Map of type identifiers to class type definitions */
    private readonly typeMap = new Map<string, ClassType>();

    /** List of registered listeners */
    private readonly listeners: TypeDefinitionListener[] = [];

    /** Super types that apply to all lambda types */
    private lambdaSuperTypes: BaseClassTypeRef[] = [];

    constructor(private readonly services: ExtendedTypirServices<Specifics>) {}

    addClassType(classType: ClassType): void {
        const identifier = `${classType.package}.${classType.name}`;

        if (this.typeMap.has(identifier)) {
            throw new Error(`Type '${identifier}' already exists in the registry`);
        }

        this.typeMap.set(identifier, classType);

        for (const listener of this.listeners) {
            listener.onAddedClassType?.(classType);
        }
    }

    removeClassType(classType: ClassType): void {
        const identifier = `${classType.package}.${classType.name}`;
        const existingClassType = this.typeMap.get(identifier);

        if (existingClassType != undefined) {
            this.typeMap.delete(identifier);

            for (const listener of this.listeners) {
                listener.onRemovedClassType?.(existingClassType);
            }
        }
    }

    getAllClassTypes(): ClassType[] {
        return Array.from(this.typeMap.values());
    }

    resolveCustomClassOrLambdaType(type: ValueType, genericTypeArgs?: Map<string, CustomValueType>): CustomValueType {
        if (GenericTypeRef.is(type)) {
            return this.resolveGenericTypeRef(type, genericTypeArgs);
        } else if (ClassTypeRef.is(type)) {
            const classTypeDef = this.getClassType(type.type);

            return this.services.factory.CustomClasses.getOrCreate({
                definition: classTypeDef,
                isNullable: type.isNullable,
                typeArgs: new Map(
                    Array.from(type.typeArgs ?? new Map()).map(([key, typeArg]) => [
                        key,
                        this.resolveCustomClassOrLambdaType(typeArg, genericTypeArgs)
                    ])
                )
            });
        } else {
            const returnType = type.returnType;
            const resolvedReturnType = VoidType.is(returnType)
                ? this.services.factory.CustomVoid.getOrCreate()
                : this.resolveCustomClassOrLambdaType(returnType as ValueType, genericTypeArgs);

            return this.services.factory.CustomLambdas.getOrCreate({
                returnType: resolvedReturnType,
                parameterTypes: type.parameters.map((paramType) =>
                    this.resolveCustomClassOrLambdaType(paramType.type, genericTypeArgs)
                ),
                typeArgs: new Map(),
                isNullable: type.isNullable
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

    getClassTypeIfExisting(typeIdentifier: string): ClassType | undefined {
        const identifier = typeIdentifier.includes(".") ? typeIdentifier : `builtin.${typeIdentifier}`;
        return this.typeMap.get(identifier);
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
        return this.services.factory.CustomFunctions.create({
            definition: type,
            name,
            typeArgs: genericTypeArgs ?? new Map()
        });
    }

    registerLambdaSuperTypes(superTypes: BaseClassTypeRef[]): void {
        this.lambdaSuperTypes = superTypes;
    }

    getLambdaSuperTypes(): BaseClassTypeRef[] {
        return this.lambdaSuperTypes;
    }

    addListener(listener: TypeDefinitionListener): void {
        this.listeners.push(listener);
    }

    removeListener(listener: TypeDefinitionListener): void {
        const index = this.listeners.indexOf(listener);
        if (index !== -1) {
            this.listeners.splice(index, 1);
        }
    }
}
