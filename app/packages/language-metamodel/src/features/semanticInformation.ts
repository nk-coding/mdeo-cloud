import type { AstReflection } from "@mdeo/language-common";
import {
    Class,
    ClassOrEnumImport,
    Enum,
    type ClassOrEnumImportType,
    type ClassOrImportType,
    type ClassType,
    type EnumType
} from "../grammar/metamodelTypes.js";
import type { AstNode } from "langium";

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
        const currentClass = resolveToClass(current, reflection);
        if (currentClass == undefined) {
            continue;
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

/**
 * Resolves a ClassOrImport to its actual Class.
 *
 * @param classOrImport the class or import to resolve
 * @param reflection the AST reflection to check node types
 * @returns the resolved Class, or undefined if it cannot be resolved
 */
export function resolveToClass(classOrImport: AstNode | undefined, reflection: AstReflection): ClassType | undefined {
    if (classOrImport == undefined) {
        return undefined;
    }
    if (reflection.isInstance(classOrImport, Class)) {
        return classOrImport;
    }
    const resolved = resolveImport(classOrImport as ClassOrEnumImportType, reflection);
    if (resolved != undefined && reflection.isInstance(resolved, Class)) {
        return resolved;
    }
    return undefined;
}

/**
 * Resolves an EnumOrImport to its actual Enum.
 *
 * @param enumOrImport the enum or import to resolve
 * @param reflection the AST reflection to check node types
 * @returns the resolved Enum, or undefined if it cannot be resolved
 */
export function resolveToEnum(enumOrImport: AstNode | undefined, reflection: AstReflection): EnumType | undefined {
    if (enumOrImport == undefined) {
        return undefined;
    }
    if (reflection.isInstance(enumOrImport, Enum)) {
        return enumOrImport;
    }
    const resolved = resolveImport(enumOrImport as ClassOrEnumImportType, reflection);
    if (resolved != undefined && reflection.isInstance(resolved, Enum)) {
        return resolved;
    }
    return undefined;
}

/**
 * Resolves a chain of ClassOrEnumImports to the actual Class or Enum.
 * 
 * @param node the starting ClassOrEnumImport node
 * @param reflection the AST reflection to check node types
 * @returns the resolved Class or Enum, or undefined if it cannot be resolved
 */
export function resolveImport(node: ClassOrEnumImportType | undefined, reflection: AstReflection): ClassType | EnumType | undefined {
    let current = node;
    while (current != undefined) {
        const entityRef = current.entity?.ref;
        if (entityRef == undefined) {
            return undefined;
        }
        if (reflection.isInstance(entityRef, ClassOrEnumImport)) {
            current = entityRef;
        } else if (reflection.isInstance(entityRef, Class) || reflection.isInstance(entityRef, Enum)) {
            return entityRef;
        } else {
            return undefined;
        }
    }
    return undefined;
}
