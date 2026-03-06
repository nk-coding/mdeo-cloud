import type { AstReflection } from "@mdeo/language-common";
import {
    Association,
    Class,
    MetaModel,
    resolveClassChain,
    type AssociationType,
    type ClassType
} from "@mdeo/language-metamodel";
import type { AstNode } from "langium";

/**
 * Abstract base class providing shared metamodel navigation utilities.
 *
 * Encapsulates common logic for traversing class hierarchies, collecting
 * metamodel roots, resolving class references, and finding associations
 * between classes. Subclasses extend this to implement higher-level
 * association-related logic without duplicating traversal code.
 */
export abstract class BaseMetamodelHelper {
    /**
     * Creates a new instance.
     *
     * @param reflection The AST reflection used for runtime type checks
     */
    constructor(protected readonly reflection: AstReflection) {}

    /**
     * Finds all associations that connect two classes, considering inheritance
     * and both directions.
     *
     * An association is included when its source class is in the inheritance
     * chain of either argument and its target class is in the chain of the other.
     *
     * @param class1 The first class
     * @param class2 The second class
     * @returns All associations that connect the two classes
     */
    public findAssociationsBetweenClasses(class1: ClassType, class2: ClassType): AssociationType[] {
        const result: AssociationType[] = [];
        const class1Chain = resolveClassChain(class1, this.reflection);
        const class2Chain = resolveClassChain(class2, this.reflection);
        const metamodels = this.collectMetamodels([...class1Chain, ...class2Chain]);
        const class1ChainSet = new Set(class1Chain);
        const class2ChainSet = new Set(class2Chain);

        for (const metaModel of metamodels) {
            for (const element of metaModel.elements ?? []) {
                if (!this.reflection.isInstance(element, Association)) {
                    continue;
                }

                const assoc = element;
                const sourceClass = this.resolveToClass(assoc.source?.class?.ref);
                const targetClass = this.resolveToClass(assoc.target?.class?.ref);

                if (!sourceClass || !targetClass) {
                    continue;
                }

                const connects =
                    (class1ChainSet.has(sourceClass) && class2ChainSet.has(targetClass)) ||
                    (class2ChainSet.has(sourceClass) && class1ChainSet.has(targetClass));

                if (connects) {
                    result.push(assoc);
                }
            }
        }

        return result;
    }

    /**
     * Collects metamodel roots by walking up the container hierarchy for each
     * class in the provided list.
     *
     * @param classes The classes whose enclosing metamodels should be collected
     * @returns The set of discovered metamodel roots
     */
    protected collectMetamodels(classes: ClassType[]): Set<{ elements?: AstNode[] }> {
        const metamodels = new Set<{ elements?: AstNode[] }>();
        for (const cls of classes) {
            const metaModel = this.getMetaModel(cls);
            if (metaModel) {
                metamodels.add(metaModel);
            }
        }
        return metamodels;
    }

    /**
     * Gets the containing metamodel root for a class by walking up the AST
     * container chain until a MetaModel node is found.
     *
     * @param classType The class for which to find the containing metamodel
     * @returns The metamodel root, or undefined if none is found
     */
    protected getMetaModel(classType: ClassType): { elements?: AstNode[] } | undefined {
        let current: AstNode | undefined = classType;
        while (current != undefined) {
            if (this.reflection.isInstance(current, MetaModel)) {
                return current as { elements?: AstNode[] };
            }
            current = current.$container;
        }
        return undefined;
    }

    /**
     * Narrows an AST node to a class type using runtime type reflection.
     *
     * @param node The candidate AST node
     * @returns The class type when the node is a Class, otherwise undefined
     */
    protected resolveToClass(node: AstNode | undefined): ClassType | undefined {
        if (node != undefined && this.reflection.isInstance(node, Class)) {
            return node;
        }
        return undefined;
    }
}
