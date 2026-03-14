import type { GModelElement } from "@eclipse-glsp/server";

/**
 * Checks whether a diagram model element was imported from an external metamodel file.
 *
 * Imported elements are decorated with the {@code "imported"} CSS class by the
 * {@link MetamodelGModelFactory} at model-creation time. Context action handlers
 * use this helper to suppress editing operations on elements that cannot be
 * modified because they live in a different file.
 *
 * @param element The diagram model element to inspect
 * @returns {@code true} when the element carries the {@code "imported"} CSS class
 */
export function isImportedElement(element: GModelElement): boolean {
    return (element as unknown as { cssClasses?: string[] }).cssClasses?.includes("imported") ?? false;
}

/**
 * Derives a default property name from a class name by lower-casing the first letter.
 *
 * For example, {@code "MyClass"} becomes {@code "myClass"} and {@code "B"} becomes {@code "b"}.
 * This mirrors the convention used in {@code MetamodelCreateEdgeSchemaResolver.computePropertyName}.
 *
 * @param className The class name to derive the property name from
 * @returns The default property name, or {@code "ref"} when the class name is empty
 */
export function generateDefaultPropertyName(className: string): string {
    if (!className) {
        return "ref";
    }
    return className.charAt(0).toLowerCase() + className.slice(1);
}
