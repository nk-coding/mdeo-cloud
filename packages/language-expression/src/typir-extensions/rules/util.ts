import type { ClassType } from "../config/type.js";

/**
 * Get a unique identifier for a ClassType based on its package and name.
 *
 * @param classType The class type definition
 * @returns A unique string identifier
 */
export function getClassTypeIdentifier(classType: ClassType): string {
    return classType.package + "." + classType.name;
}
