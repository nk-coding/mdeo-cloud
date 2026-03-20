import type { AstReflection } from "@mdeo/language-common";
import {
    Property,
    resolveClassChain,
    isOptionalMultiplicity,
    type ClassType,
    type PropertyType
} from "@mdeo/language-metamodel";
import { AssociationResolver, type ResolvedAssociation } from "./associationResolver.js";
import type { ModelData, ModelDataInstance, ModelDataLink, ModelDataPropertyValue } from "./modelData.js";
import {
    Model,
    SimpleValue,
    EnumValue,
    ListValue,
    type ModelType,
    type ObjectInstanceType,
    type LinkType,
    type LiteralValueType,
    type SimpleValueType,
    type EnumValueType,
    type ListValueType,
    type SingleValueType
} from "../grammar/modelTypes.js";

/**
 * Converter for transforming Model AST nodes to the ModelData format.
 * Handles extraction of instances, links, and property values.
 */
export class ModelDataConverter {
    /**
     * Resolver for associations between classes.
     */
    private readonly associationResolver: AssociationResolver;

    /**
     * Creates a new ModelDataConverter.
     *
     * @param reflection The AST reflection for type checking
     */
    constructor(private readonly reflection: AstReflection) {
        this.associationResolver = new AssociationResolver(reflection);
    }

    /**
     * Converts a Model AST node to the ModelData format.
     *
     * @param model The Model AST node to convert
     * @returns The ModelData representation
     */
    convertModel(model: ModelType): ModelData {
        if (!this.reflection.isInstance(model, Model)) {
            throw new Error("Expected Model AST node");
        }

        const instances = this.convertInstances(model.objects ?? []);
        const links = this.convertLinks(model.links ?? []);

        return {
            metamodelPath: model.import?.file ?? "",
            instances,
            links
        };
    }

    /**
     * Converts an array of ObjectInstance AST nodes to ModelDataInstance format.
     *
     * @param objects The ObjectInstance AST nodes
     * @returns Array of ModelDataInstance
     */
    private convertInstances(objects: ObjectInstanceType[]): ModelDataInstance[] {
        return objects.map((obj) => this.convertInstance(obj));
    }

    /**
     * Converts a single ObjectInstance AST node to ModelDataInstance format.
     *
     * @param obj The ObjectInstance AST node
     * @returns The ModelDataInstance representation
     */
    private convertInstance(obj: ObjectInstanceType): ModelDataInstance {
        const classType = obj.class?.ref;
        const className = classType?.name ?? "";

        const properties = this.buildPropertyList(obj, classType);

        return {
            name: obj.name ?? "",
            className,
            properties
        };
    }

    /**
     * Builds the complete property record for an instance, including unset optional properties.
     *
     * @param obj The ObjectInstance AST node
     * @param classType The resolved class type, if available
     * @returns Record of property name to value
     */
    private buildPropertyList(
        obj: ObjectInstanceType,
        classType: ClassType | undefined
    ): Record<string, ModelDataPropertyValue | ModelDataPropertyValue[]> {
        const assignedProperties = this.extractAssignedProperties(obj);

        if (!classType) {
            return Object.fromEntries(assignedProperties);
        }

        return this.mergeWithClassProperties(assignedProperties, classType);
    }

    /**
     * Extracts properties that are explicitly assigned in the instance.
     *
     * @param obj The ObjectInstance AST node
     * @returns Map of property name to value
     */
    private extractAssignedProperties(
        obj: ObjectInstanceType
    ): Map<string, ModelDataPropertyValue | ModelDataPropertyValue[]> {
        const result = new Map<string, ModelDataPropertyValue | ModelDataPropertyValue[]>();

        for (const propAssign of obj.properties ?? []) {
            const propRef = propAssign.name?.ref;
            if (!propRef || !this.reflection.isInstance(propRef, Property)) {
                continue;
            }

            const prop = propRef as PropertyType;
            if (!prop.name) {
                continue;
            }

            const value = this.convertPropertyValue(propAssign.value);
            result.set(prop.name, value);
        }

        return result;
    }

    /**
     * Merges assigned properties with all class properties, adding null for unset optional ones.
     *
     * @param assignedProperties Map of assigned properties
     * @param classType The class type
     * @returns Complete record of property name to value
     */
    private mergeWithClassProperties(
        assignedProperties: Map<string, ModelDataPropertyValue | ModelDataPropertyValue[]>,
        classType: ClassType
    ): Record<string, ModelDataPropertyValue | ModelDataPropertyValue[]> {
        const result: Record<string, ModelDataPropertyValue | ModelDataPropertyValue[]> = {};
        const classChain = resolveClassChain(classType, this.reflection);

        for (const cls of classChain) {
            for (const prop of cls.properties ?? []) {
                if (!prop.name) {
                    continue;
                }

                if (assignedProperties.has(prop.name)) {
                    result[prop.name] = assignedProperties.get(prop.name)!;
                } else if (isOptionalMultiplicity(prop.multiplicity, this.reflection)) {
                    result[prop.name] = null;
                }
            }
        }

        return result;
    }

    /**
     * Converts a LiteralValue AST node to a ModelDataPropertyValue or array thereof.
     *
     * @param value The LiteralValue AST node
     * @returns The ModelDataPropertyValue or array representation
     */
    private convertPropertyValue(
        value: LiteralValueType | undefined
    ): ModelDataPropertyValue | ModelDataPropertyValue[] {
        if (!value) {
            return null;
        }

        if (this.reflection.isInstance(value, ListValue)) {
            return this.convertListValue(value as ListValueType);
        }

        return this.convertSingleValue(value as SingleValueType);
    }

    /**
     * Converts a ListValue AST node to an array of ModelDataPropertyValue.
     *
     * @param listValue The ListValue AST node
     * @returns Array of ModelDataPropertyValue
     */
    private convertListValue(listValue: ListValueType): ModelDataPropertyValue[] {
        return (listValue.values ?? []).map((v) => this.convertSingleValue(v));
    }

    /**
     * Converts a SingleValue AST node to a primitive ModelDataPropertyValue.
     *
     * @param value The SingleValue AST node
     * @returns The primitive ModelDataPropertyValue
     */
    private convertSingleValue(value: SingleValueType): ModelDataPropertyValue {
        if (this.reflection.isInstance(value, SimpleValue)) {
            return this.extractSimpleValue(value as SimpleValueType);
        }

        if (this.reflection.isInstance(value, EnumValue)) {
            return this.extractEnumValue(value as EnumValueType);
        }

        return null;
    }

    /**
     * Extracts the primitive value from a SimpleValue AST node.
     *
     * @param simpleValue The SimpleValue AST node
     * @returns The primitive value (string, number, or boolean)
     */
    private extractSimpleValue(simpleValue: SimpleValueType): ModelDataPropertyValue {
        if (simpleValue.stringValue !== undefined) {
            return simpleValue.stringValue;
        }
        if (simpleValue.numberValue !== undefined) {
            return simpleValue.numberValue;
        }
        if (simpleValue.booleanValue !== undefined) {
            return simpleValue.booleanValue;
        }
        return null;
    }

    /**
     * Extracts the enum entry name from an EnumValue AST node.
     *
     * @param enumValue The EnumValue AST node
     * @returns The enum object with entry name
     */
    private extractEnumValue(enumValue: EnumValueType): ModelDataPropertyValue {
        const entryRef = enumValue.value?.ref;
        const entryName = entryRef?.name;
        return entryName ? { enum: entryName } : null;
    }

    /**
     * Converts an array of Link AST nodes to ModelDataLink format.
     *
     * @param links The Link AST nodes
     * @returns Array of ModelDataLink
     */
    private convertLinks(links: LinkType[]): ModelDataLink[] {
        return links.map((link) => this.convertLink(link));
    }

    /**
     * Converts a single Link AST node to ModelDataLink format.
     *
     * Always outputs the link in the metamodel's canonical direction (metamodel-source
     * as `sourceName`, metamodel-target as `targetName`). When the user writes the
     * link in the opposite direction, source and target are swapped so the backend
     * always receives consistently-directed links.
     *
     * @param link The Link AST node
     * @returns The ModelDataLink representation, normalised to metamodel direction
     */
    private convertLink(link: LinkType): ModelDataLink {
        const sourceObj = link.source.object.ref;
        const targetObj = link.target.object.ref;
        const sourceInstanceName = sourceObj?.name ?? "";
        const targetInstanceName = targetObj?.name ?? "";

        if (sourceObj == undefined || targetObj == undefined) {
            throw new Error(
                `Link with undefined source or target object: ${sourceInstanceName} -> ${targetInstanceName}`
            );
        }

        const { matchesDirection, association } = this.resolveAssociation(link, sourceObj, targetObj);

        if (!matchesDirection) {
            return {
                sourceName: targetInstanceName,
                sourceProperty: association.source?.name ?? null,
                targetName: sourceInstanceName,
                targetProperty: association.target?.name ?? null
            };
        }

        return {
            sourceName: sourceInstanceName,
            sourceProperty: association.source?.name ?? null,
            targetName: targetInstanceName,
            targetProperty: association.target?.name ?? null
        };
    }

    /**
     * Resolves association and direction for a link based on its source and target objects.
     *
     * @param link The Link AST node
     * @param sourceObj The source ObjectInstance
     * @param targetObj The target ObjectInstance
     * @returns Object containing the resolved association and whether the link direction matches the association direction
     */
    private resolveAssociation(
        link: LinkType,
        sourceObj: ObjectInstanceType,
        targetObj: ObjectInstanceType
    ): ResolvedAssociation {
        const sourceClass = this.resolveObjectClass(sourceObj);
        const targetClass = this.resolveObjectClass(targetObj);
        const sourceEnd = link.source.property?.ref;
        const targetEnd = link.target.property?.ref;

        const resolved = this.associationResolver.resolveAssociation(sourceClass, targetClass, sourceEnd, targetEnd);

        if (resolved == undefined) {
            throw new Error(
                `No association found between ${sourceClass.name} and ${targetClass.name} for link with source property '${sourceEnd?.name}' and target property '${targetEnd?.name}'`
            );
        }

        return resolved;
    }

    /**
     * Resolves the class type for an object instance.
     *
     * @param obj The ObjectInstance AST node
     * @returns The ClassType or undefined
     */
    private resolveObjectClass(obj: ObjectInstanceType): ClassType {
        const classRef = obj.class.ref;
        if (classRef == undefined) {
            throw new Error(`ObjectInstance ${obj.name} has no class reference`);
        }
        return classRef;
    }
}
