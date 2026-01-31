import type { AstReflection } from "@mdeo/language-common";
import {
    Association,
    PrimitiveType,
    type AssociationEndType,
    type AssociationType,
    type ClassType as MetamodelClassType,
    type EnumType as MetamodelEnumType,
    type MetaModelType,
    type MultiplicityType,
    type PropertyType,
    resolveClassChain,
    isMultipleMultiplicity,
    isOptionalMultiplicity
} from "@mdeo/language-metamodel";
import { sharedImport } from "@mdeo/language-shared";
import type { ValueType } from "../../typir-extensions/config/type.js";
import type { MetamodelClassInfo, MetamodelPropertyInfo, MetamodelRelationInfo } from "./metamodelClassInfo.js";
import { DefaultTypeNames } from "../../type-system/typeSystemConfig.js";
import { typeRef } from "../../typir-extensions/config/typeBuilder.js";

const { AstUtils } = sharedImport("langium");

/**
 * Configuration for creating collection types from element types.
 */
export interface CollectionTypeFactory {
    /**
     * Creates a list type from an element type.
     *
     * @param elementType The type of elements in the list
     * @returns A ValueType representing the list type
     */
    createListType: (elementType: ValueType) => ValueType;
}

/**
 * Default collection type factory for metamodel extraction.
 * Creates List types with the appropriate type arguments.
 */
export const DefaultCollectionTypeFactory: CollectionTypeFactory = {
    createListType: (elementType) => typeRef("List").withTypeArgs({ T: elementType }).build()
};

/**
 * Extracts class information from metamodel classes.
 * This function creates a new extractor instance, processes the classes, and returns the results.
 * The extractor should not be reused.
 *
 * With the simplified import system, the naming scheme uses the file where each class is DEFINED,
 * not the file that imports it. This ensures that the same class referenced from different files
 * has the same type identity.
 *
 * @param classes The classes to analyze (directly defined, not imports)
 * @param reflection The AST reflection for type checking
 * @param collectionTypeFactory Factory for creating list types
 * @returns Array of MetamodelClassInfo representing the extracted classes
 */
export function extractMetamodelClasses(
    classes: MetamodelClassType[],
    reflection: AstReflection,
    collectionTypeFactory: CollectionTypeFactory
): MetamodelClassInfo[] {
    const extractor = new MetamodelClassExtractor(reflection, collectionTypeFactory);
    return extractor.extractClasses(classes);
}

/**
 * Gets the package name for a metamodel class or enum based on its document path.
 * Uses the class's own document, not the importing metamodel's document.
 * This ensures consistent type identity across different import locations.
 *
 * @param node The class or enum to get the package for
 * @returns The package name as a string
 */
export function getPackage(node: MetamodelClassType | MetamodelEnumType): string {
    const document = AstUtils.getDocument(node);
    if (document == undefined) {
        throw new Error("Class or enum does not belong to a document");
    }
    const path = document.uri.path;
    return `metamodel.${path}`;
}

/**
 * Internal class for extracting class information from metamodel AST nodes.
 * Generates an intermediate representation that can be converted to TypedClasses or ClassTypes.
 * Should not be instantiated directly; use extractMetamodelClasses() instead.
 *
 * With the new import system, each class's package is determined by its own document path,
 * ensuring consistent type identity regardless of where the class is imported from.
 */
class MetamodelClassExtractor {
    private readonly reservedNames = new Set<string>();
    private readonly classNameMap = new Map<MetamodelClassType, string>();
    private readonly allClassesSet = new Set<MetamodelClassType>();

    /**
     * Creates a new MetamodelClassExtractor.
     *
     * @param reflection The AST reflection for type checking
     * @param collectionTypeFactory Factory for creating list types
     */
    constructor(
        private readonly reflection: AstReflection,
        private readonly collectionTypeFactory: CollectionTypeFactory
    ) {}

    /**
     * Extracts class information from a list of metamodel classes.
     *
     * @param classes The classes to analyze
     * @returns Array of MetamodelClassInfo representing the extracted classes
     */
    extractClasses(classes: MetamodelClassType[]): MetamodelClassInfo[] {
        this.processTopLevelClasses(classes);
        return this.buildClassInfos();
    }

    /**
     * Processes top-level classes to reserve names and build the class map.
     *
     * @param classes The classes to analyze
     */
    private processTopLevelClasses(classes: MetamodelClassType[]): void {
        for (const classNode of classes) {
            const name = this.getClassName(classNode);
            this.reservedNames.add(name);
            this.classNameMap.set(classNode, name);
        }

        for (const classNode of classes) {
            const classChain = resolveClassChain(classNode, this.reflection) as MetamodelClassType[];
            for (const chainClass of classChain) {
                this.allClassesSet.add(chainClass);
                if (!this.classNameMap.has(chainClass)) {
                    const chainName = this.generateUniqueClassName(this.getClassName(chainClass));
                    this.classNameMap.set(chainClass, chainName);
                }
            }
        }
    }

    /**
     * Gets the name of a class. Helper to work around type inference issues.
     *
     * @param classNode The class node
     * @returns The class name
     */
    private getClassName(classNode: MetamodelClassType): string {
        return (classNode as unknown as { name: string }).name;
    }

    /**
     * Gets the extensions of a class. Helper to work around type inference issues.
     *
     * @param classNode The class node
     * @returns The extensions list or undefined
     */
    private getClassExtensions(
        classNode: MetamodelClassType
    ): { extensions: Array<{ class: { ref?: MetamodelClassType } }> } | undefined {
        return (classNode as unknown as { extensions?: { extensions: Array<{ class: { ref?: MetamodelClassType } }> } })
            .extensions;
    }

    /**
     * Gets the properties of a class. Helper to work around type inference issues.
     *
     * @param classNode The class node
     * @returns The properties list
     */
    private getClassProperties(classNode: MetamodelClassType): PropertyType[] {
        return (classNode as unknown as { properties: PropertyType[] }).properties;
    }

    /**
     * Generates a unique class name by appending #N suffix if needed.
     * Ensures no naming conflicts in the extracted class set.
     *
     * @param baseName The base name to make unique
     * @returns A unique class name
     */
    private generateUniqueClassName(baseName: string): string {
        if (!this.reservedNames.has(baseName)) {
            this.reservedNames.add(baseName);
            return baseName;
        }

        let counter = 1;
        let candidateName = `${baseName}#${counter}`;
        while (this.reservedNames.has(candidateName)) {
            counter++;
            candidateName = `${baseName}#${counter}`;
        }
        this.reservedNames.add(candidateName);
        return candidateName;
    }

    /**
     * Builds the complete class information for all extracted classes.
     * Collects associations and generates MetamodelClassInfo for each class.
     *
     * @returns Array of MetamodelClassInfo
     */
    private buildClassInfos(): MetamodelClassInfo[] {
        const associations = this.collectAssociations();
        const result: MetamodelClassInfo[] = [];

        for (const [classNode, simpleName] of this.classNameMap.entries()) {
            const info = this.buildClassInfo(classNode, simpleName, associations);
            result.push(info);
        }

        return result;
    }

    /**
     * Collects all associations from the metamodels that contain the extracted classes.
     *
     * @returns Array of associations found in the relevant metamodels
     */
    private collectAssociations(): AssociationType[] {
        const associations: AssociationType[] = [];
        const metamodelSet = new Set<MetaModelType>();

        for (const cls of this.allClassesSet) {
            const metamodel = this.getMetaModel(cls);
            if (metamodel != undefined) {
                metamodelSet.add(metamodel);
            }
        }

        for (const metamodel of metamodelSet) {
            for (const element of metamodel.elements ?? []) {
                if (this.reflection.isInstance(element, Association)) {
                    associations.push(element);
                }
            }
        }

        return associations;
    }

    /**
     * Finds the containing metamodel for a class by traversing up the AST.
     *
     * @param cls The class to find the metamodel for
     * @returns The containing metamodel or undefined if not found
     */
    private getMetaModel(cls: MetamodelClassType): MetaModelType | undefined {
        let container = cls.$container;
        while (container != undefined) {
            if ("elements" in container && "imports" in container) {
                return container as MetaModelType;
            }
            container = container.$container;
        }
        return undefined;
    }

    /**
     * Builds complete class information including properties, relations, and superclasses.
     * Uses the class's own document path for package naming, not the importing file.
     *
     * @param classNode The metamodel class node
     * @param simpleName The unique name assigned to this class
     * @param associations All associations from the metamodel
     * @returns Complete MetamodelClassInfo
     */
    private buildClassInfo(
        classNode: MetamodelClassType,
        simpleName: string,
        associations: AssociationType[]
    ): MetamodelClassInfo {
        const classPackage = getPackage(classNode);
        const superClasses = this.extractSuperClasses(classNode);
        const properties = this.extractProperties(classNode);
        const relations = this.extractRelations(classNode, associations);

        return {
            name: simpleName,
            package: classPackage,
            superClasses,
            properties,
            relations
        };
    }

    /**
     * Builds the fully qualified class name using the class's own document path.
     *
     * @param classNode The class node to build the name for
     * @param simpleName The simple class name
     * @returns The fully qualified class name
     */
    private buildFullClassName(classNode: MetamodelClassType, simpleName: string): string {
        const classPackage = getPackage(classNode);
        return `${classPackage}.${simpleName}`;
    }

    /**
     * Extracts superclass references from a class's extension declarations.
     *
     * @param classNode The class to extract superclasses from
     * @returns Array of fully qualified superclass names
     */
    private extractSuperClasses(classNode: MetamodelClassType): string[] {
        const superClasses: string[] = [];
        const extensions = this.getClassExtensions(classNode);
        for (const extension of extensions?.extensions ?? []) {
            const parentClass = extension.class.ref;
            if (parentClass != undefined) {
                const parentName = this.classNameMap.get(parentClass);
                if (parentName != undefined) {
                    superClasses.push(this.buildFullClassName(parentClass, parentName));
                }
            }
        }
        return superClasses;
    }

    /**
     * Extracts property information from a class's property declarations.
     *
     * @param classNode The class to extract properties from
     * @returns Array of property information
     */
    private extractProperties(classNode: MetamodelClassType): MetamodelPropertyInfo[] {
        return this.getClassProperties(classNode).map((prop) => ({
            name: prop.name,
            valueType: this.resolvePropertyType(prop)
        }));
    }

    /**
     * Resolves the complete type of a property including multiplicity.
     *
     * @param property The property to resolve the type for
     * @returns The complete ValueType including multiplicity handling
     */
    private resolvePropertyType(property: PropertyType): ValueType {
        const baseType = this.resolveBasePropertyType(property);
        return this.applyMultiplicity(baseType, property.multiplicity);
    }

    /**
     * Resolves the base type of a property without considering multiplicity.
     * Handles primitive types and defaults to string for enum types.
     *
     * @param property The property to resolve the base type for
     * @returns The base ValueType
     */
    private resolveBasePropertyType(property: PropertyType): ValueType {
        const typeValue = property.type;
        if (this.reflection.isInstance(typeValue, PrimitiveType)) {
            return this.resolvePrimitiveType(typeValue.name);
        }
        return { type: DefaultTypeNames.String, isNullable: false };
    }

    /**
     * Maps a primitive type name to the corresponding type system type name.
     *
     * @param name The primitive type name from the metamodel
     * @returns The ValueType for this primitive
     */
    private resolvePrimitiveType(name: string): ValueType {
        const typeNameMap: Record<string, string> = {
            int: DefaultTypeNames.Int,
            long: DefaultTypeNames.Long,
            float: DefaultTypeNames.Float,
            double: DefaultTypeNames.Double,
            string: DefaultTypeNames.String,
            boolean: DefaultTypeNames.Boolean
        };
        return { type: typeNameMap[name] ?? DefaultTypeNames.String, isNullable: false };
    }

    /**
     * Applies multiplicity modifiers to a base type (list, optional, or required).
     *
     * @param baseType The base type to modify
     * @param multiplicity The multiplicity specification
     * @returns The modified ValueType with multiplicity applied
     */
    private applyMultiplicity(baseType: ValueType, multiplicity: MultiplicityType | undefined): ValueType {
        if (multiplicity == undefined) {
            return baseType;
        } else if (isMultipleMultiplicity(multiplicity, this.reflection)) {
            return this.collectionTypeFactory.createListType(baseType);
        } else if (isOptionalMultiplicity(multiplicity, this.reflection)) {
            return { ...baseType, isNullable: true };
        } else {
            return baseType;
        }
    }

    /**
     * Extracts relation information from associations relevant to a class.
     *
     * @param classNode The class to extract relations for
     * @param associations All associations from the metamodel
     * @returns Array of relation information
     */
    private extractRelations(classNode: MetamodelClassType, associations: AssociationType[]): MetamodelRelationInfo[] {
        const relations: MetamodelRelationInfo[] = [];

        for (const assoc of associations) {
            this.processAssociationForClass(classNode, assoc, relations);
        }

        return relations;
    }

    /**
     * Processes a single association to extract relations for a specific class.
     * Adds relation entries for both source and target ends if applicable.
     *
     * @param classNode The class to extract relations for
     * @param assoc The association to process
     * @param relations Output array to add relations to
     */
    private processAssociationForClass(
        classNode: MetamodelClassType,
        assoc: AssociationType,
        relations: MetamodelRelationInfo[]
    ): void {
        const sourceClass = assoc.source?.class?.ref;
        const targetClass = assoc.target?.class?.ref;

        if (sourceClass == undefined || targetClass == undefined) {
            return;
        }

        if (!this.allClassesSet.has(sourceClass) || !this.allClassesSet.has(targetClass)) {
            return;
        }

        if (sourceClass === classNode && assoc.source.name != undefined) {
            const oppositePropertyName = assoc.target.name;
            const relation = this.createRelationInfo(assoc.source, targetClass, oppositePropertyName, true);
            if (relation != undefined) {
                relations.push(relation);
            }
        }

        if (targetClass === classNode && assoc.target.name != undefined) {
            const oppositePropertyName = assoc.source.name;
            const relation = this.createRelationInfo(assoc.target, sourceClass, oppositePropertyName, false);
            if (relation != undefined) {
                relations.push(relation);
            }
        }
    }

    /**
     * Creates relation information from an association end.
     * Uses the opposite class's own document path for type naming.
     *
     * @param end The association end to create a relation from
     * @param oppositeClass The class at the opposite end
     * @param oppositePropertyName The property name at the opposite end, or undefined if unnamed
     * @param isOutgoing Whether this is an outgoing relation
     * @returns The relation information or undefined if invalid
     */
    private createRelationInfo(
        end: AssociationEndType,
        oppositeClass: MetamodelClassType,
        oppositePropertyName: string | undefined,
        isOutgoing: boolean
    ): MetamodelRelationInfo | undefined {
        if (end.name == undefined) {
            return undefined;
        }

        const oppositeSimpleName = this.classNameMap.get(oppositeClass);
        if (oppositeSimpleName == undefined) {
            return undefined;
        }

        const oppositeClassName = this.buildFullClassName(oppositeClass, oppositeSimpleName);
        const baseType: ValueType = { type: oppositeClassName, isNullable: false };
        const valueType = this.applyMultiplicity(baseType, end.multiplicity);

        return {
            property: end.name,
            oppositeProperty: oppositePropertyName,
            oppositeClassName,
            isOutgoing,
            valueType
        };
    }
}
