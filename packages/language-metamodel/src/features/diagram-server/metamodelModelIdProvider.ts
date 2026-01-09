import { sharedImport, type ModelIdProvider } from "@mdeo/language-shared";
import type { AstNode } from "langium";
import type {
    ClassType,
    PropertyType,
    AssociationType,
    AssociationEndType,
    MetaModelType
} from "../../grammar/metamodelTypes.js";
import type {
    PartialClass,
    PartialProperty,
    PartialAssociation,
    PartialAssociationEnd,
    PartialMetaModel
} from "../../grammar/metamodelPartialTypes.js";

const { injectable } = sharedImport("inversify");

/**
 * Provides unique IDs for metamodel AST nodes based on semantic information.
 * IDs are constructed to be deterministic and meaningful.
 */
@injectable()
export class MetamodelModelIdProvider implements ModelIdProvider {
    getId(node: AstNode): string | undefined {
        const nodeType = node.$type;

        switch (nodeType) {
            case "MetaModel":
                return this.getMetaModelId(node as MetaModelType);
            case "Class":
                return this.getClassId(node as ClassType);
            case "Property":
                return this.getPropertyId(node as PropertyType);
            case "Association":
                return this.getAssociationId(node as AssociationType);
            case "AssociationEnd":
                return this.getAssociationEndId(node as AssociationEndType);
            default:
                return undefined;
        }
    }

    /**
     * Generates ID for MetaModel root node.
     */
    private getMetaModelId(_node: PartialMetaModel): string {
        return "metamodel-graph";
    }

    /**
     * Generates ID for Class node based on class name.
     */
    private getClassId(node: PartialClass): string {
        const name = node.name ?? "unnamed";
        return `class_${name}`;
    }

    /**
     * Generates ID for Property node based on parent class and property name.
     */
    private getPropertyId(node: PartialProperty): string {
        const propName = node.name ?? "unnamed";
        const parent = node.$container;
        if (parent?.$type === "Class") {
            const parentClass = parent as PartialClass;
            return `${this.getClassId(parentClass)}_prop_${propName}`;
        }
        return `property_${propName}`;
    }

    /**
     * Generates ID for Association based on start and end classes.
     * Uses class names and properties to create a semantic ID.
     */
    private getAssociationId(node: PartialAssociation): string {
        const startClassName = node.start ? this.resolveClassName(node.start) : "unknown";
        const targetClassName = node.target ? this.resolveClassName(node.target) : "unknown";
        const startProperty = node.start?.property ?? "";
        const targetProperty = node.target?.property ?? "";
        const operator = node.operator ?? "--";

        return `association_${startClassName}_${startProperty}_${operator}_${targetClassName}_${targetProperty}`;
    }

    /**
     * Generates ID for AssociationEnd.
     */
    private getAssociationEndId(node: PartialAssociationEnd): string {
        const className = this.resolveClassName(node);
        const property = node.property ?? "noProperty";
        return `assocend_${className}_${property}`;
    }

    /**
     * Resolves the class name from an AssociationEnd.
     */
    private resolveClassName(end: PartialAssociationEnd): string {
        const resolved = end.class?.ref;
        if (!resolved) {
            return "unresolved";
        }

        if (resolved.$type === "Class") {
            const classNode = resolved as PartialClass;
            return classNode.name ?? "unnamed";
        }

        if (resolved.$type === "ClassImport") {
            const importNode = resolved as any;
            return importNode.name ?? importNode.element?.ref?.name ?? "imported";
        }

        return "unknown";
    }
}
