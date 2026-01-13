import { AstReflectionKey, BaseModelIdProvider, sharedImport } from "@mdeo/language-shared";
import type { AstNode, Reference } from "langium";
import {
    Class,
    Property,
    Association,
    AssociationEnd,
    MetaModel,
    ClassImport,
    ClassExtension
} from "../../grammar/metamodelTypes.js";
import type {
    PartialClass,
    PartialProperty,
    PartialAssociation,
    PartialAssociationEnd,
    PartialMetaModel,
    PartialClassImport,
    PartialClassExtension
} from "../../grammar/metamodelPartialTypes.js";
import type { AstReflection } from "@mdeo/language-common";

const { injectable, inject } = sharedImport("inversify");

/**
 * Provides unique IDs for metamodel AST nodes based on semantic information.
 * IDs are constructed to be deterministic and meaningful.
 */
@injectable()
export class MetamodelModelIdProvider extends BaseModelIdProvider {
    /**
     * Injected AST reflection service for type checking and model introspection.
     */
    @inject(AstReflectionKey)
    protected reflection!: AstReflection;

    getName(node: AstNode): string | undefined {
        if (this.reflection.isInstance(node, MetaModel)) {
            return this.getMetaModelName(node);
        } else if (this.reflection.isInstance(node, Class)) {
            return this.getClassName(node);
        } else if (this.reflection.isInstance(node, ClassImport)) {
            return this.getClassImportName(node);
        } else if (this.reflection.isInstance(node, Property)) {
            return this.getPropertyName(node);
        } else if (this.reflection.isInstance(node, Association)) {
            return this.getAssociationName(node);
        } else if (this.reflection.isInstance(node, AssociationEnd)) {
            return this.getAssociationEndName(node);
        } else if (this.reflection.isInstance(node, ClassExtension)) {
            return this.getClassExtensionName(node);
        }
        return undefined;
    }

    /**
     * Generates ID for MetaModel root node.
     */
    private getMetaModelName(_node: PartialMetaModel): string {
        return "metamodel-graph";
    }

    /**
     * Generates ID for Class node based on class name.
     */
    private getClassName(node: PartialClass): string {
        return node.name ?? "unnamed";
    }

    /**
     * Generates ID for ClassImport node based on the imported class name or alias.
     */
    private getClassImportName(node: PartialClassImport): string {
        return node.name ?? node.entity?.ref?.name ?? "unnamed";
    }

    /**
     * Generates ID for Property node based on parent class and property name.
     */
    private getPropertyName(node: PartialProperty): string {
        const propName = node.name ?? "unnamed";
        const parent = node.$container;
        if (parent != undefined && this.reflection.isInstance(parent, Class)) {
            const parentClass = parent as PartialClass;
            return `${this.getClassName(parentClass)}_prop_${propName}`;
        }
        return propName;
    }

    /**
     * Generates ID for Association based on start and end classes.
     * Uses class names and properties to create a semantic ID.
     */
    private getAssociationName(node: PartialAssociation): string {
        const startClassName = this.resolveClassName(node.start?.class);
        const targetClassName = this.resolveClassName(node.target?.class);
        const startProperty = node.start?.property ?? "";
        const targetProperty = node.target?.property ?? "";
        const operator = node.operator ?? "--";

        return `${startClassName}_${startProperty}_${operator}_${targetClassName}_${targetProperty}`;
    }

    /**
     * Generates ID for AssociationEnd.
     */
    private getAssociationEndName(node: PartialAssociationEnd): string {
        const className = this.resolveClassName(node.class);
        const property = node.property ?? "noProperty";
        return `${className}_${property}`;
    }

    /**
     * Generates ID for ClassExtension based on parent class and extension name.
     */
    private getClassExtensionName(node: PartialClassExtension): string {
        const owningClass = node.$container as PartialClass | undefined;
        const parentClassName = owningClass != undefined ? this.getClassName(owningClass) : "unknownParent";
        const extensionName = this.resolveClassName(node.class);
        return `${parentClassName}_${extensionName}`;
    }

    /**
     * Resolves the class name from a class reference.
     */
    private resolveClassName(clsReference: Reference<AstNode> | undefined): string {
        if (clsReference === undefined || clsReference.error != undefined) {
            return "unresolved";
        }
        const resolved = clsReference.ref;

        if (this.reflection.isInstance(resolved, Class)) {
            return this.getClassName(resolved);
        }

        if (this.reflection.isInstance(resolved, ClassImport)) {
            return this.getClassImportName(resolved);
        }

        return "unknown";
    }
}
