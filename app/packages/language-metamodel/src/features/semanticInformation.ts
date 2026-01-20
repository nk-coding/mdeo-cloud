import type { AstReflection } from "@mdeo/language-common";
import { Class, ClassImport, type ClassOrImportType, type ClassType } from "../grammar/metamodelTypes.js";

/**
 * Resolves the full class chain including all extended classes for a given class or class import.
 * Guarantees topological order (derived classes come after base classes).
 *
 * @param classOrImport the starting class or class import
 * @param reflection the AST reflection to check node types
 * @returns an array of classes in topological order
 */
export function resolveClassChain(classOrImport: ClassOrImportType, reflection: AstReflection): ClassType[] {
    const result: ClassType[] = [];
    const resultSet = new Set<ClassType>();
    const queue: ClassOrImportType[] = [classOrImport];

    while (queue.length > 0) {
        const current = queue.shift()!;
        let currentClass: ClassType;
        if (reflection.isInstance(current, Class)) {
            currentClass = current;
        } else if (reflection.isInstance(current, ClassImport)) {
            currentClass = current.entity.ref!;
        } else {
            throw new Error("Unexpected type in class chain resolution");
        }
        if (resultSet.has(currentClass)) {
            continue;
        }
        resultSet.add(currentClass);
        result.push(currentClass);
        for (const classExtension of currentClass.extensions?.extensions ?? []) {
            const reference = classExtension.class.ref;
            if (reference != undefined) {
                queue.push(reference);
            }
        }
    }
    return result;
}
