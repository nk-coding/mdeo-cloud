import type { ValidationAcceptor, ValidationChecks, AstNode } from "langium";
import type { AstReflection, ExtendedLangiumServices } from "@mdeo/language-common";
import { MultiMap } from "langium";
import {
    Association,
    Class,
    ClassOrEnumImport,
    Enum,
    EnumTypeReference,
    MetaModel,
    RangeMultiplicity,
    SingleMultiplicity,
    MetamodelAssociationOperators,
    type AssociationEndType,
    type AssociationType,
    type ClassType,
    type EnumEntryType,
    type EnumType,
    type MetaModelType,
    type MultiplicityType,
    type PropertyType
} from "../grammar/metamodelTypes.js";
import { resolveClassChain, resolveImport, resolveToClass, resolveToEnum } from "../features/semanticInformation.js";

/**
 * Interface mapping for metamodel AST types used in validation checks.
 */
interface MetamodelAstTypes {
    MetaModel: MetaModelType;
    Class: ClassType;
    Enum: EnumType;
    EnumEntry: EnumEntryType;
    Property: PropertyType;
    Association: AssociationType;
}

/**
 * Registers validation checks for the metamodel language.
 *
 * @param services The language services
 */
export function registerMetamodelValidationChecks(services: ExtendedLangiumServices): void {
    const registry = services.validation.ValidationRegistry;
    const validator = new MetamodelValidator(services);

    const checks: ValidationChecks<MetamodelAstTypes> = {
        MetaModel: validator.validateMetaModel.bind(validator),
        EnumEntry: validator.validateEnumEntryUniqueness.bind(validator),
        Property: validator.validateProperty.bind(validator),
        Association: validator.validateAssociation.bind(validator)
    };

    registry.register(checks, validator);
}

/**
 * Validator for metamodel language constructs.
 */
export class MetamodelValidator {
    private readonly reflection: AstReflection;

    constructor(private readonly services: ExtendedLangiumServices) {
        this.reflection = services.shared.AstReflection;
    }

    /**
     * Validates the entire metamodel for class/enum name uniqueness.
     * This validation considers:
     * - Classes/enums declared directly in the file
     * - Imported classes/enums (using their alias if specified, otherwise the original name)
     * - Enums used in properties of classes in the class chain
     */
    validateMetaModel(metaModel: MetaModelType, accept: ValidationAcceptor): void {
        this.validateClassEnumNameUniqueness(metaModel, accept);
    }

    /**
     * Validates that class and enum names are unique.
     * Collects names from:
     * 1. Classes directly declared in the file
     * 2. Enums directly declared in the file
     * 3. Imported entities (using alias if provided)
     * 4. Enums used in properties within the class chain (using import alias if imported under different name)
     */
    private validateClassEnumNameUniqueness(metaModel: MetaModelType, accept: ValidationAcceptor): void {
        const nameToNodes = new MultiMap<string, AstNode>();
        const importedClasses: ClassType[] = [];
        const importedEnumes: Set<EnumType> = new Set();

        for (const fileImport of metaModel.imports ?? []) {
            for (const entityImport of fileImport.imports ?? []) {
                const resolvedEntity = resolveImport(entityImport, this.reflection);
                if (resolvedEntity == undefined) {
                    continue;
                }
                nameToNodes.add(entityImport.name ?? entityImport.entity.$refText, entityImport);
                if (this.reflection.isInstance(resolvedEntity, Class)) {
                    importedClasses.push(resolvedEntity);
                } else if (this.reflection.isInstance(resolvedEntity, Enum)) {
                    importedEnumes.add(resolvedEntity);
                }
            }
        }

        const declaredClasses: ClassType[] = [];
        const declaredEnums: EnumType[] = [];

        for (const element of metaModel.elements ?? []) {
            if (this.reflection.isInstance(element, Class)) {
                declaredClasses.push(element);
                if (element.name) {
                    nameToNodes.add(element.name, element);
                }
            } else if (this.reflection.isInstance(element, Enum)) {
                declaredEnums.push(element);
                if (element.name) {
                    nameToNodes.add(element.name, element);
                }
            }
        }

        const usedEnums = new Set<EnumType>();

        for (const cls of [...declaredClasses, ...importedClasses]) {
            const classChain = resolveClassChain(cls, this.reflection);
            for (const chainClass of classChain) {
                for (const property of chainClass.properties ?? []) {
                    if (!this.reflection.isInstance(property.type, EnumTypeReference)) {
                        continue;
                    }
                    const enumRef = property.type.enum?.ref;
                    if (enumRef == undefined) {
                        continue;
                    }
                    const resolvedEnum = resolveToEnum(enumRef, this.reflection);
                    if (resolvedEnum == undefined || usedEnums.has(resolvedEnum)) {
                        continue;
                    }
                    usedEnums.add(resolvedEnum);
                    if (importedEnumes.has(resolvedEnum)) {
                        continue;
                    }

                    const usedEnumMetaModel = getContainingMetaModel(resolvedEnum, this.reflection);
                    const currentMetaModel = metaModel;
                    if (usedEnumMetaModel === currentMetaModel) {
                        continue;
                    }

                    if (resolvedEnum.name) {
                        nameToNodes.add(resolvedEnum.name, cls);
                    }
                }
            }
        }

        for (const [name, nodes] of nameToNodes.entriesGroupedByKey()) {
            if (nodes.length > 1) {
                for (const node of nodes) {
                    const nodeType = this.getNodeTypeDescription(node);
                    if (getContainingMetaModel(node, this.reflection) !== metaModel) {
                        continue;
                    }
                    accept("error", `Duplicate ${nodeType} name: '${name}'.`, {
                        node,
                        property: this.getNameProperty(node)
                    });
                }
            }
        }
    }

    /**
     * Validates that enum entry names are unique within their parent enum.
     */
    validateEnumEntryUniqueness(entry: EnumEntryType, accept: ValidationAcceptor): void {
        const parentEnum = entry.$container;
        if (parentEnum == undefined || !this.reflection.isInstance(parentEnum, Enum)) {
            return;
        }

        const enumType = parentEnum as EnumType;
        const entryName = entry.name;
        if (!entryName) {
            return;
        }

        let count = 0;
        for (const e of enumType.entries ?? []) {
            if (e.name === entryName) {
                count++;
            }
        }

        if (count > 1) {
            accept("error", `Duplicate enum entry name: '${entryName}'.`, {
                node: entry,
                property: "name"
            });
        }
    }

    /**
     * Validates a property, including:
     * - Name uniqueness within class hierarchy
     * - Multiplicity format
     */
    validateProperty(property: PropertyType, accept: ValidationAcceptor): void {
        this.validatePropertyMultiplicity(property, accept);
        this.validatePropertyUniquenessInHierarchy(property, accept);
    }

    /**
     * Validates that a property name is unique within the class hierarchy,
     * including properties from parent classes and association ends.
     */
    private validatePropertyUniquenessInHierarchy(property: PropertyType, accept: ValidationAcceptor): void {
        const parentClass = property.$container;
        if (!parentClass || !this.reflection.isInstance(parentClass, Class)) {
            return;
        }

        const propertyName = property.name;
        if (!propertyName) {
            return;
        }

        const allPropertyNames = collectAllPropertyNames(parentClass, this.reflection);

        let count = 0;
        for (const name of allPropertyNames) {
            if (name === propertyName) {
                count++;
            }
        }

        if (count > 1) {
            accept("error", `Duplicate property name: '${propertyName}' in class hierarchy.`, {
                node: property,
                property: "name"
            });
        }
    }

    /**
     * Validates that multiplicity ranges are valid (upper >= lower).
     */
    private validatePropertyMultiplicity(property: PropertyType, accept: ValidationAcceptor): void {
        const multiplicity = property.multiplicity;
        if (!multiplicity) {
            return;
        }

        this.validateMultiplicityRange(multiplicity, property, accept);
    }

    /**
     * Validates a range multiplicity (upper bound >= lower bound).
     */
    private validateMultiplicityRange(multiplicity: MultiplicityType, node: AstNode, accept: ValidationAcceptor): void {
        if (this.reflection.isInstance(multiplicity, RangeMultiplicity)) {
            const lower = multiplicity.lower;
            const upperNumeric = multiplicity.upperNumeric;

            if (multiplicity.upper === "*") {
                return;
            }

            if (upperNumeric !== undefined && upperNumeric < lower) {
                accept(
                    "error",
                    `Invalid multiplicity range: upper bound (${upperNumeric}) must be >= lower bound (${lower}).`,
                    { node, property: "multiplicity" }
                );
            }
        }
    }

    /**
     * Validates an association, including:
     * - Property name uniqueness at each end
     * - Correct property definition based on operator
     * - Composition multiplicity constraints
     */
    validateAssociation(association: AssociationType, accept: ValidationAcceptor): void {
        this.validateAssociationPropertyRequirements(association, accept);
        this.validateAssociationPropertyUniqueness(association, accept);
        this.validateAssociationEndMultiplicity(association.source, association, accept);
        this.validateAssociationEndMultiplicity(association.target, association, accept);
        this.validateCompositionMultiplicity(association, accept);
    }

    /**
     * Validates that association end property names don't conflict with existing properties
     * in the class hierarchy (including regular properties and other association ends).
     */
    private validateAssociationPropertyUniqueness(association: AssociationType, accept: ValidationAcceptor): void {
        if (association.source?.name) {
            const sourceClass = resolveToClass(association.source.class?.ref, this.reflection);
            if (sourceClass) {
                const existingNames = this.collectPropertyNamesExcludingCurrentEnd(sourceClass, association, "source");
                if (existingNames.includes(association.source.name)) {
                    accept(
                        "error",
                        `Property name '${association.source.name}' conflicts with an existing property in the class hierarchy.`,
                        { node: association.source, property: "name" }
                    );
                }
            }
        }

        if (association.target?.name) {
            const targetClass = resolveToClass(association.target.class?.ref, this.reflection);
            if (targetClass) {
                const existingNames = this.collectPropertyNamesExcludingCurrentEnd(targetClass, association, "target");
                if (existingNames.includes(association.target.name)) {
                    accept(
                        "error",
                        `Property name '${association.target.name}' conflicts with an existing property in the class hierarchy.`,
                        { node: association.target, property: "name" }
                    );
                }
            }
        }
    }

    /**
     * Collects all property names in the class hierarchy, excluding the current association end.
     */
    private collectPropertyNamesExcludingCurrentEnd(
        classType: ClassType,
        currentAssociation: AssociationType,
        currentEnd: "source" | "target"
    ): string[] {
        const propertyNames: string[] = [];
        const classChain = resolveClassChain(classType, this.reflection);

        for (const cls of classChain) {
            for (const prop of cls.properties ?? []) {
                if (prop.name) {
                    propertyNames.push(prop.name);
                }
            }

            const metaModel = getContainingMetaModel(cls, this.reflection);
            if (metaModel) {
                for (const element of metaModel.elements ?? []) {
                    if (this.reflection.isInstance(element, Association)) {
                        const assoc = element;

                        const isCurrentSourceEnd = assoc === currentAssociation && currentEnd === "source";
                        const isCurrentTargetEnd = assoc === currentAssociation && currentEnd === "target";

                        const sourceClass = resolveToClass(assoc.source?.class?.ref, this.reflection);
                        if (sourceClass === cls && assoc.source?.name && !isCurrentSourceEnd) {
                            propertyNames.push(assoc.source.name);
                        }

                        const targetClass = resolveToClass(assoc.target?.class?.ref, this.reflection);
                        if (targetClass === cls && assoc.target?.name && !isCurrentTargetEnd) {
                            propertyNames.push(assoc.target.name);
                        }
                    }
                }
            }
        }

        return propertyNames;
    }

    /**
     * Validates that properties are defined on the correct ends based on the association operator.
     *
     * Operator rules:
     * - --> (NAVIGABLE_TO_TARGET): left side needs property
     * - <-- (NAVIGABLE_TO_SOURCE): right side needs property
     * - <--> (BIDIRECTIONAL): both sides need properties
     * - *--> (COMPOSITION_SOURCE_NAVIGABLE_TARGET): both sides need properties
     * - *-- (COMPOSITION_SOURCE): right side needs property
     * - <--* (COMPOSITION_TARGET_NAVIGABLE_SOURCE): left side needs property
     * - --* (COMPOSITION_TARGET): left side needs property
     */
    private validateAssociationPropertyRequirements(association: AssociationType, accept: ValidationAcceptor): void {
        const operator = association.operator;
        const sourceHasProperty = !!association.source?.name;
        const targetHasProperty = !!association.target?.name;

        switch (operator) {
            case MetamodelAssociationOperators.NAVIGABLE_TO_TARGET:
                if (!sourceHasProperty) {
                    accept("error", `Association with '-->' requires a property name on the source (left) side.`, {
                        node: association,
                        property: "source"
                    });
                }
                if (targetHasProperty) {
                    accept(
                        "error",
                        `Association with '-->' must not have a property name on the target (right) side.`,
                        {
                            node: association,
                            property: "target"
                        }
                    );
                }
                break;

            case MetamodelAssociationOperators.NAVIGABLE_TO_SOURCE:
                if (!targetHasProperty) {
                    accept("error", `Association with '<--' requires a property name on the target (right) side.`, {
                        node: association,
                        property: "target"
                    });
                }
                if (sourceHasProperty) {
                    accept("error", `Association with '<--' must not have a property name on the source (left) side.`, {
                        node: association,
                        property: "source"
                    });
                }
                break;

            case MetamodelAssociationOperators.BIDIRECTIONAL:
                if (!sourceHasProperty) {
                    accept(
                        "error",
                        `Bidirectional association '<-->' requires a property name on the source (left) side.`,
                        {
                            node: association,
                            property: "source"
                        }
                    );
                }
                if (!targetHasProperty) {
                    accept(
                        "error",
                        `Bidirectional association '<-->' requires a property name on the target (right) side.`,
                        {
                            node: association,
                            property: "target"
                        }
                    );
                }
                break;

            case MetamodelAssociationOperators.COMPOSITION_SOURCE_NAVIGABLE_TARGET:
                if (!sourceHasProperty) {
                    accept(
                        "error",
                        `Composition association '*-->' requires a property name on the source (left) side.`,
                        {
                            node: association,
                            property: "source"
                        }
                    );
                }
                if (!targetHasProperty) {
                    accept(
                        "error",
                        `Composition association '*-->' requires a property name on the target (right) side.`,
                        {
                            node: association,
                            property: "target"
                        }
                    );
                }
                break;

            case MetamodelAssociationOperators.COMPOSITION_SOURCE:
                if (!targetHasProperty) {
                    accept(
                        "error",
                        `Composition association '*--' requires a property name on the target (right) side.`,
                        {
                            node: association,
                            property: "target"
                        }
                    );
                }
                if (sourceHasProperty) {
                    accept(
                        "error",
                        `Composition association '*--' must not have a property name on the source (left) side.`,
                        {
                            node: association,
                            property: "source"
                        }
                    );
                }
                break;

            case MetamodelAssociationOperators.COMPOSITION_TARGET_NAVIGABLE_SOURCE:
                if (!sourceHasProperty) {
                    accept(
                        "error",
                        `Composition association '<--*' requires a property name on the source (left) side.`,
                        {
                            node: association,
                            property: "source"
                        }
                    );
                }
                if (!targetHasProperty) {
                    accept(
                        "error",
                        `Composition association '<--*' requires a property name on the target (right) side.`,
                        {
                            node: association,
                            property: "target"
                        }
                    );
                }
                break;

            case MetamodelAssociationOperators.COMPOSITION_TARGET:
                if (!sourceHasProperty) {
                    accept(
                        "error",
                        `Composition association '--*' requires a property name on the source (left) side.`,
                        {
                            node: association,
                            property: "source"
                        }
                    );
                }
                if (targetHasProperty) {
                    accept(
                        "error",
                        `Composition association '--*' must not have a property name on the target (right) side.`,
                        {
                            node: association,
                            property: "target"
                        }
                    );
                }
                break;
        }

        if (sourceHasProperty && association.source?.class?.ref != undefined) {
            if (resolveToClass(association.source.class.ref, this.reflection)?.$container !== association.$container) {
                accept(
                    "error",
                    `Cannot define a property on an association end for imported class '${association.source.name}'.`,
                    {
                        node: association.source,
                        property: "name"
                    }
                );
            }
        }
        if (targetHasProperty && association.target?.class?.ref != undefined) {
            if (resolveToClass(association.target.class.ref, this.reflection)?.$container !== association.$container) {
                accept(
                    "error",
                    `Cannot define a property on an association end for imported class '${association.target.name}'.`,
                    {
                        node: association.target,
                        property: "name"
                    }
                );
            }
        }
    }

    /**
     * Validates multiplicity at an association end.
     */
    private validateAssociationEndMultiplicity(
        end: AssociationEndType | undefined,
        association: AssociationType,
        accept: ValidationAcceptor
    ): void {
        if (!end?.multiplicity) {
            return;
        }

        this.validateMultiplicityRange(end.multiplicity, association, accept);
    }

    /**
     * Validates composition multiplicity constraints.
     * The opposite side of a composition must have multiplicity 0..1 or nothing.
     * - If there is exactly one composition pointing to the class: 1 or nothing is valid
     * - If there are multiple compositions pointing to the class: must be 0..1 or nothing or ?
     */
    private validateCompositionMultiplicity(association: AssociationType, accept: ValidationAcceptor): void {
        const operator = association.operator;

        let compositeEnd: AssociationEndType | undefined;
        let oppositeEnd: AssociationEndType | undefined;

        switch (operator) {
            case MetamodelAssociationOperators.COMPOSITION_SOURCE:
            case MetamodelAssociationOperators.COMPOSITION_SOURCE_NAVIGABLE_TARGET:
                compositeEnd = association.source;
                oppositeEnd = association.target;
                break;

            case MetamodelAssociationOperators.COMPOSITION_TARGET:
            case MetamodelAssociationOperators.COMPOSITION_TARGET_NAVIGABLE_SOURCE:
                compositeEnd = association.target;
                oppositeEnd = association.source;
                break;

            default:
                return;
        }

        if (!oppositeEnd || !compositeEnd) {
            return;
        }

        const oppositeClass = resolveToClass(oppositeEnd.class?.ref, this.reflection);
        if (!oppositeClass) {
            return;
        }

        const compositionCount = this.countCompositionsToClass(oppositeClass, association);

        this.validateOppositeCompositionMultiplicity(compositeEnd, compositionCount, association, accept);
    }

    /**
     * Counts how many compositions point to a given class (considering class chain).
     */
    private countCompositionsToClass(targetClass: ClassType, currentAssociation: AssociationType): number {
        const metaModel = this.getContainingMetaModel(currentAssociation);
        if (!metaModel) {
            return 1;
        }

        const targetClassChain = new Set(resolveClassChain(targetClass, this.reflection));
        let count = 0;

        for (const element of metaModel.elements ?? []) {
            if (!this.reflection.isInstance(element, Association)) {
                continue;
            }

            const assoc = element;
            const operator = assoc.operator;

            switch (operator) {
                case MetamodelAssociationOperators.COMPOSITION_SOURCE:
                case MetamodelAssociationOperators.COMPOSITION_SOURCE_NAVIGABLE_TARGET: {
                    const targetCls = resolveToClass(assoc.target?.class?.ref, this.reflection);
                    if (targetCls && targetClassChain.has(targetCls)) {
                        count++;
                    }
                    break;
                }

                case MetamodelAssociationOperators.COMPOSITION_TARGET:
                case MetamodelAssociationOperators.COMPOSITION_TARGET_NAVIGABLE_SOURCE: {
                    const sourceCls = resolveToClass(assoc.source?.class?.ref, this.reflection);
                    if (sourceCls && targetClassChain.has(sourceCls)) {
                        count++;
                    }
                    break;
                }
            }
        }

        return count;
    }

    /**
     * Validates that the composite end has appropriate multiplicity.
     * - If exactly one composition: must be 1 or nothing
     * - If multiple compositions: must be 0..1 or nothing or ?
     */
    private validateOppositeCompositionMultiplicity(
        compositeEnd: AssociationEndType,
        compositionCount: number,
        association: AssociationType,
        accept: ValidationAcceptor
    ): void {
        const multiplicity = compositeEnd.multiplicity;

        if (!multiplicity) {
            return;
        }

        if (this.reflection.isInstance(multiplicity, SingleMultiplicity)) {
            const value = multiplicity.value;
            const numericValue = multiplicity.numericValue;

            if (compositionCount > 1) {
                if (value === "?" || numericValue === 0) {
                    return;
                }
                if (numericValue === 1) {
                    accept(
                        "error",
                        `Composition multiplicity must be optional (0..1, ?, or 0) when multiple compositions point to this class.`,
                        { node: association, property: "source" }
                    );
                }
                if (value === "*" || value === "+") {
                    accept(
                        "error",
                        `Composition multiplicity cannot be '${value}' - a class can only be contained by one parent.`,
                        { node: association, property: "source" }
                    );
                }
            } else {
                if (value === "*" || value === "+") {
                    accept(
                        "error",
                        `Composition multiplicity cannot be '${value}' - a class can only be contained by one parent.`,
                        { node: association, property: "source" }
                    );
                }
                if (numericValue !== undefined && numericValue > 1) {
                    accept(
                        "error",
                        `Composition multiplicity cannot exceed 1 - a class can only be contained by one parent.`,
                        { node: association, property: "source" }
                    );
                }
            }
        } else if (this.reflection.isInstance(multiplicity, RangeMultiplicity)) {
            const lower = multiplicity.lower;
            const upper = multiplicity.upper;
            const upperNumeric = multiplicity.upperNumeric;

            if (upper === "*") {
                accept(
                    "error",
                    `Composition multiplicity cannot have unbounded upper limit - a class can only be contained by one parent.`,
                    { node: association, property: "source" }
                );
            } else if (upperNumeric !== undefined && upperNumeric > 1) {
                accept(
                    "error",
                    `Composition multiplicity upper bound cannot exceed 1 - a class can only be contained by one parent.`,
                    { node: association, property: "source" }
                );
            }

            if (compositionCount > 1) {
                if (lower !== 0) {
                    accept(
                        "error",
                        `Composition multiplicity lower bound must be 0 when multiple compositions point to this class.`,
                        { node: association, property: "source" }
                    );
                }
            }
        }
    }

    /**
     * Gets the containing MetaModel for an AST node.
     */
    private getContainingMetaModel(node: AstNode): MetaModelType | undefined {
        let current: AstNode | undefined = node;
        while (current) {
            if (this.reflection.isInstance(current, MetaModel)) {
                return current;
            }
            current = current.$container;
        }
        return undefined;
    }

    /**
     * Gets a description of the node type for error messages.
     */
    private getNodeTypeDescription(node: AstNode): string {
        if (this.reflection.isInstance(node, Class)) {
            return "class";
        }
        if (this.reflection.isInstance(node, Enum)) {
            return "enum";
        }
        if (this.reflection.isInstance(node, ClassOrEnumImport)) {
            return "imported entity";
        }
        return "element";
    }

    /**
     * Gets the property to highlight for a given node.
     */
    private getNameProperty(node: AstNode): string | undefined {
        if (this.reflection.isInstance(node, Class) || this.reflection.isInstance(node, Enum)) {
            return "name";
        }
        if (this.reflection.isInstance(node, ClassOrEnumImport)) {
            return "name";
        }
        return undefined;
    }
}

/**
 * Collects all property names in the class hierarchy, including:
 * - Properties from the class itself
 * - Properties from parent classes
 * - Properties from association ends relevant to classes in the hierarchy
 *
 * @param classType The class to collect property names for
 * @param reflection The AST reflection utility
 * @returns Array of property names (may contain duplicates if there are conflicts)
 */
export function collectAllPropertyNames(classType: ClassType, reflection: AstReflection): string[] {
    const propertyNames: string[] = [];
    const classChain = resolveClassChain(classType, reflection);

    for (const cls of classChain) {
        for (const prop of cls.properties ?? []) {
            if (prop.name) {
                propertyNames.push(prop.name);
            }
        }

        const metaModel = getContainingMetaModel(cls, reflection);
        if (metaModel) {
            for (const element of metaModel.elements ?? []) {
                if (reflection.isInstance(element, Association)) {
                    const assoc = element;

                    const sourceClass = resolveToClass(assoc.source?.class?.ref, reflection);
                    if (sourceClass === cls && assoc.source?.name) {
                        propertyNames.push(assoc.source.name);
                    }

                    const targetClass = resolveToClass(assoc.target?.class?.ref, reflection);
                    if (targetClass === cls && assoc.target?.name) {
                        propertyNames.push(assoc.target.name);
                    }
                }
            }
        }
    }

    return propertyNames;
}

/**
 * Gets the containing MetaModel for an AST node.
 */
function getContainingMetaModel(node: AstNode, reflection: AstReflection): MetaModelType | undefined {
    let current: AstNode | undefined = node;
    while (current) {
        if (reflection.isInstance(current, MetaModel)) {
            return current;
        }
        current = current.$container;
    }
    return undefined;
}
