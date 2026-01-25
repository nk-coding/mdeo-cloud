import { type AstReflection, type ExtendedLangiumServices } from "@mdeo/language-common";
import { getExportetEntitiesFromRelativeFile, sharedImport } from "@mdeo/language-shared";
import type { ReferenceInfo, Scope } from "langium";
import {
    ObjectInstance,
    PropertyAssignment,
    EnumValue,
    LinkEnd,
    type ModelType,
    type ObjectInstanceType,
    type PropertyAssignmentType,
    type LinkEndType,
    Model
} from "../grammar/modelTypes.js";
import {
    Class,
    ClassOrEnumImport,
    Enum,
    EnumTypeReference,
    resolveClassChain,
    type ClassOrEnumImportType,
    type ClassType,
    type EnumOrImportType,
    type EnumType,
    type PropertyType
} from "@mdeo/language-metamodel";
import { AssociationEndCache } from "./associationEndCache.js";

const { DefaultScopeProvider, AstUtils, EMPTY_SCOPE } = sharedImport("langium");

/**
 * The scope provider for the Model language.
 * Handles scoping for object instance classes, properties, enum values, and links.
 */
export class ModelScopeProvider extends DefaultScopeProvider {
    /**
     * The AST reflection service for type checking and model introspection.
     */
    private readonly astReflection: AstReflection;

    /**
     * Cache for association end lookups.
     */
    private readonly associationEndCache: AssociationEndCache;

    /**
     * Constructs a new ModelScopeProvider.
     * @param services The extended Langium services.
     */
    constructor(services: ExtendedLangiumServices) {
        super(services);
        this.astReflection = services.shared.AstReflection;
        this.associationEndCache = new AssociationEndCache(services);
    }

    /**
     * Gets the scope for a given reference context.
     *
     * @param context The reference context
     * @returns The scope for the reference
     */
    override getScope(context: ReferenceInfo): Scope {
        const document = AstUtils.getDocument(context.container);

        if (context.property === "class" && this.astReflection.isInstance(context.container, ObjectInstance)) {
            return this.getObjectClassScope(context, document);
        }
        if (context.property === "name" && this.astReflection.isInstance(context.container, PropertyAssignment)) {
            return this.getPropertyNameScope(context);
        }
        if (context.property === "value" && this.astReflection.isInstance(context.container, EnumValue)) {
            return this.getEnumValueScope(context);
        }
        if (context.property === "property" && this.astReflection.isInstance(context.container, LinkEnd)) {
            return this.getLinkPropertyScope(context);
        }
        if (context.property === "object" && this.astReflection.isInstance(context.container, LinkEnd)) {
            return this.getObjectInstancesScope(context);
        }

        return EMPTY_SCOPE;
    }

    /**
     * Gets the scope for object class references.
     */
    private getObjectClassScope(context: ReferenceInfo, document: any): Scope {
        const model = context.container.$container as ModelType;
        const metamodelImport = model.import;
        return getExportetEntitiesFromRelativeFile<ClassType | ClassOrEnumImportType>(
            document,
            metamodelImport.file,
            [Class, ClassOrEnumImport],
            this.indexManager
        );
    }

    /**
     * Gets the scope for property name references.
     *
     * @param context The reference context
     * @return The scope containing the properties of the object's class chain
     */
    private getPropertyNameScope(context: ReferenceInfo): Scope {
        let objectInstance = context.container.$container as ObjectInstanceType;
        if (this.astReflection.isInstance(objectInstance, PropertyAssignment)) {
            objectInstance = objectInstance.$container as ObjectInstanceType;
        }
        const classRef = objectInstance?.class?.ref;
        if (!classRef) {
            return EMPTY_SCOPE;
        }
        const classChain = resolveClassChain(classRef, this.astReflection);
        return this.createScopeForNodes(classChain.flatMap((cls) => cls.properties));
    }

    /**
     * Gets the scope for enum value references.
     *
     * @param context The reference context
     * @returns The scope containing the enum entries
     */
    private getEnumValueScope(context: ReferenceInfo): Scope {
        const propertyAssignment = this.findPropertyAssignment(context.container);
        if (propertyAssignment == undefined) {
            return EMPTY_SCOPE;
        }
        const propertyRef = propertyAssignment.name?.ref as PropertyType | undefined;
        if (propertyRef?.type == undefined) {
            return EMPTY_SCOPE;
        }
        return this.getEnumEntriesFromType(propertyRef.type);
    }

    /**
     * Gets the scope for object instance references in links.
     */
    private getObjectInstancesScope(context: ReferenceInfo): Scope {
        const model = AstUtils.getContainerOfType(context.container, (node) =>
            this.astReflection.isInstance(node, Model)
        );
        if (model == undefined) {
            return EMPTY_SCOPE;
        }
        return this.createScopeForNodes(model.objects);
    }

    /**
     * Gets the scope for property references in link ends.
     *
     * This method now properly handles associations instead of just primitive properties.
     * It traverses the class chain and for each class, looks up all association ends
     * that reference that class in the metamodel.
     *
     * @param context The reference context
     * @returns The scope containing the association ends of the linked object's class chain
     */
    private getLinkPropertyScope(context: ReferenceInfo): Scope {
        const linkEnd = context.container as LinkEndType;
        const objectRef = linkEnd.object?.ref;

        if (objectRef == undefined) {
            return EMPTY_SCOPE;
        }

        const classRef = objectRef.class?.ref;
        if (classRef == undefined) {
            return EMPTY_SCOPE;
        }

        // Get the full class chain (including superclasses)
        const classChain = resolveClassChain(classRef, this.astReflection);

        // For each class in the chain, get all association ends that reference it
        const allAssociationEnds = classChain.flatMap((cls) => {
            return this.associationEndCache.getAssociationEndsForClass(cls);
        });

        // Remove duplicates (same association end might be found through multiple paths)
        const uniqueAssociationEnds = Array.from(new Map(allAssociationEnds.map((end) => [end.name, end])).values());

        return this.createScopeForNodes(uniqueAssociationEnds);
    }

    /**
     * Finds the property assignment containing the given node.
     */
    private findPropertyAssignment(node: any): PropertyAssignmentType | undefined {
        let current = node.$container;
        while (current) {
            if (this.astReflection.isInstance(current, PropertyAssignment)) {
                return current as PropertyAssignmentType;
            }
            current = current.$container;
        }
        return undefined;
    }

    /**
     * Gets enum entries from a property type.
     */
    private getEnumEntriesFromType(type: any): Scope {
        if (this.astReflection.isInstance(type, EnumTypeReference)) {
            const enumRef = type.enum?.ref as EnumOrImportType | undefined;
            if (enumRef != undefined) {
                const resolvedEnum = this.resolveEnum(enumRef);
                if (resolvedEnum) {
                    return this.createScopeForNodes(resolvedEnum.entries);
                }
            }
        }
        return EMPTY_SCOPE;
    }

    /**
     * Resolves an enum reference to the actual enum.
     *
     * @param enumOrImport The enum or import reference
     * @returns The resolved enum or undefined if not found
     */
    private resolveEnum(enumOrImport: EnumOrImportType): EnumType | undefined {
        if (this.astReflection.isInstance(enumOrImport, Enum)) {
            return enumOrImport;
        } else {
            const importRef = enumOrImport as ClassOrEnumImportType;
            return importRef.entity?.ref as EnumType | undefined;
        }
    }
}
