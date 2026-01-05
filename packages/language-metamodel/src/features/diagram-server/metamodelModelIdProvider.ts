import { sharedImport, type ModelIdProvider } from "@mdeo/language-shared";
import type { AstNode } from "langium";
import type {
    MetaClassType,
    PropertyType,
    AssociationType,
    AssociationEndType,
    MetaModelType
} from "../../grammar/metamodelTypes.js";

const { injectable } = sharedImport("inversify");

/**
 * Provides unique IDs for metamodel AST nodes based on semantic information.
 * IDs are constructed to be deterministic and meaningful.
 */
@injectable()
export class MetamodelModelIdProvider implements ModelIdProvider {
    /**
     * Generates a unique ID for the given metamodel AST node.
     *
     * @param node - The AST node to generate an ID for
     * @returns The generated ID or undefined if the node type is not supported
     */
    getId(node: AstNode): string | undefined {
        const nodeType = node.$type;

        switch (nodeType) {
            case "MetaModel":
                return this.getMetaModelId(node as MetaModelType);
            case "MetaClass":
                return this.getMetaClassId(node as MetaClassType);
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
    private getMetaModelId(_node: MetaModelType): string {
        return "metamodel-graph";
    }

    /**
     * Generates ID for MetaClass node based on class name.
     */
    private getMetaClassId(node: MetaClassType): string {
        return `metaclass_${node.name}`;
    }

    /**
     * Generates ID for Property node based on parent class and property name.
     */
    private getPropertyId(node: PropertyType): string {
        const parent = node.$container;
        if (parent && parent.$type === "MetaClass") {
            const parentClass = parent as MetaClassType;
            return `${this.getMetaClassId(parentClass)}_prop_${node.name}`;
        }
        return `property_${node.name}`;
    }

    /**
     * Generates ID for Association based on start and end classes.
     * Uses class names and properties to create a semantic ID.
     */
    private getAssociationId(node: AssociationType): string {
        const startClassName = this.resolveClassName(node.start);
        const targetClassName = this.resolveClassName(node.target);
        const startProperty = node.start.property || "";
        const targetProperty = node.target.property || "";

        // Create semantic ID from association endpoints
        return `association_${startClassName}_${startProperty}_${node.operator}_${targetClassName}_${targetProperty}`;
    }

    /**
     * Generates ID for AssociationEnd.
     */
    private getAssociationEndId(node: AssociationEndType): string {
        const className = this.resolveClassName(node);
        const property = node.property || "noProperty";
        return `assocend_${className}_${property}`;
    }

    /**
     * Resolves the class name from an AssociationEnd.
     */
    private resolveClassName(end: AssociationEndType): string {
        const resolved = end.class.ref;
        if (!resolved) {
            return "unresolved";
        }

        if (resolved.$type === "MetaClass") {
            return (resolved as MetaClassType).name;
        }

        if (resolved.$type === "MetaClassImport") {
            const importNode = resolved as any;
            return importNode.name || importNode.element?.ref?.name || "imported";
        }

        return "unknown";
    }
}
