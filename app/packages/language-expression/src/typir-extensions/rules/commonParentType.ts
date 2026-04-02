import type { CustomClassType } from "../kinds/custom-class/custom-class-type.js";
import { isCustomClassType } from "../kinds/custom-class/custom-class-type.js";
import type { CustomValueType } from "../kinds/custom-value/custom-value-type.js";
import { isCustomNullType } from "../kinds/custom-null/custom-null-type.js";
import type { ClassType, ValueType } from "../config/type.js";
import type { ExtendedTypirServices } from "../service/extendedTypirServices.js";
import type { TypirSpecifics } from "typir";
import { getClassTypeIdentifier } from "./util.js";

/**
 * Finds the closest common parent type between two class types.
 * Uses Dijkstra's algorithm to find the nearest common ancestor in the type hierarchy.
 *
 * @param typeA First class type
 * @param typeB Second class type
 * @param services Extended Typir services for type operations
 * @returns The common parent type, or the registered Any type if none exists
 */
export function findCommonParentType<Specifics extends TypirSpecifics>(
    typeA: CustomValueType,
    typeB: CustomValueType,
    services: ExtendedTypirServices<Specifics>
): CustomValueType {
    if (isCustomNullType(typeA)) {
        return typeB.asNullable;
    } else if (isCustomNullType(typeB)) {
        return typeA.asNullable;
    } else if (!isCustomClassType(typeA) || !isCustomClassType(typeB)) {
        return services.TypeDefinitions.getAnyType(typeA.isNullable || typeB.isNullable);
    }
    const superTypesA = findSuperTypesWithDistance(typeA.asNonNullable as CustomClassType, services);
    const superTypesB = findSuperTypesWithDistance(typeB.asNonNullable as CustomClassType, services);

    let bestType: CustomClassType | undefined = undefined;
    let bestDistance = Number.MAX_SAFE_INTEGER;

    for (const [superType, distanceA] of superTypesA.entries()) {
        const distanceB = superTypesB.get(superType);
        if (distanceB != undefined) {
            const totalDistance = distanceA + distanceB;
            if (totalDistance < bestDistance) {
                bestDistance = totalDistance;
                bestType = superType;
            }
        }
    }
    if (bestType != undefined) {
        return typeA.isNullable || typeB.isNullable ? bestType.asNullable : bestType.asNonNullable;
    }
    return services.TypeDefinitions.getAnyType(typeA.isNullable || typeB.isNullable);
}

/**
 * Search for a specific supertype using Dijkstra's algorithm.
 * Returns the supertype instance with its resolved type arguments if found.
 *
 * @param type The class type to search from
 * @param targetDefinition The target supertype definition to find
 * @returns Object with the supertype and its type arguments, or undefined if not found
 */
export function findSuperTypeWithTypeArgs(
    type: CustomClassType,
    targetDefinition: ClassType
): { type: CustomClassType; typeArgs: Map<string, CustomValueType> } | undefined {
    const targetIdentifier = getClassTypeIdentifier(targetDefinition);
    const visited = new Set<string>();

    const queue: Array<{ type: CustomClassType; distance: number }> = [{ type, distance: 0 }];

    while (queue.length > 0) {
        queue.sort((a, b) => a.distance - b.distance);
        const current = queue.shift()!;

        const currentIdentifier = getClassTypeIdentifier(current.type.details.definition);

        if (currentIdentifier === targetIdentifier) {
            return {
                type: current.type,
                typeArgs: current.type.details.typeArgs
            };
        }

        if (visited.has(currentIdentifier)) {
            continue;
        }

        visited.add(currentIdentifier);

        for (const superType of current.type.superClasses) {
            const nonNullableSuperType = superType.asNonNullable;
            if (isCustomClassType(nonNullableSuperType)) {
                const superIdentifier = getClassTypeIdentifier(nonNullableSuperType.details.definition);
                const newDistance = current.distance + 1;

                if (!visited.has(superIdentifier)) {
                    queue.push({
                        type: nonNullableSuperType,
                        distance: newDistance
                    });
                }
            }
        }
    }

    return undefined;
}

/**
 * Finds all supertypes of a given class type with their distances in the hierarchy.
 * Uses Dijkstra's algorithm to compute shortest paths to all supertypes.
 *
 * @param type The class type to search from
 * @param services Extended Typir services for type operations
 * @returns Map of supertype to its distance from the original type
 */
export function findSuperTypesWithDistance<Specifics extends TypirSpecifics>(
    type: CustomClassType,
    services: ExtendedTypirServices<Specifics>
): Map<CustomClassType, number> {
    const distances = new Map<CustomClassType, number>();
    const visited = new Set<string>();

    const queue: Array<{ type: CustomClassType; distance: number }> = [{ type, distance: 0 }];

    distances.set(type, 0);

    while (queue.length > 0) {
        queue.sort((a, b) => a.distance - b.distance);
        const current = queue.shift()!;

        const currentIdentifier = getClassTypeIdentifier(current.type.details.definition);

        if (visited.has(currentIdentifier)) {
            continue;
        }

        visited.add(currentIdentifier);

        const superTypes = current.type.details.definition.superTypes;
        if (superTypes != undefined) {
            for (const superTypeRef of superTypes) {
                const superType = services.TypeDefinitions.resolveCustomClassOrLambdaType({
                    ...superTypeRef,
                    isNullable: false
                } as ValueType);

                if (isCustomClassType(superType)) {
                    const superIdentifier = getClassTypeIdentifier(superType.details.definition);
                    const newDistance = current.distance + 1;

                    if (!visited.has(superIdentifier)) {
                        const existingDistance = distances.get(superType);
                        if (existingDistance === undefined || newDistance < existingDistance) {
                            distances.set(superType, newDistance);
                            queue.push({
                                type: superType,
                                distance: newDistance
                            });
                        }
                    }
                }
            }
        }
    }

    return distances;
}
