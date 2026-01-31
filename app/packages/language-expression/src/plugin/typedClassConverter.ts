import type { TypedClass, TypedProperty, TypedRelation } from "./typedAst.js";
import type {
    MetamodelClassInfo,
    MetamodelPropertyInfo,
    MetamodelRelationInfo
} from "../features/metamodel/metamodelClassInfo.js";
import type { TypeDefinitionService } from "../typir-extensions/service/type-definition-service.js";
import type { TypedAstConverter } from "./typedAstConverter.js";

/**
 * Converts MetamodelClassInfo to TypedClass for the TypedAst.
 * Resolves types through the Typir type system to get indices into the types array.
 */
export class TypedClassConverter {
    /**
     * Creates a new TypedClassConverter.
     *
     * @param typeDefinitions The type definition service for resolving types
     * @param typedAstConverter Function to get indices into the types array
     */
    constructor(
        private readonly typeDefinitions: TypeDefinitionService,
        private readonly typedAstConverter: TypedAstConverter
    ) {}

    /**
     * Converts a list of MetamodelClassInfo to TypedClass array.
     *
     * @param classInfos The intermediate class representations to convert
     * @returns Array of TypedClass for the TypedAst
     */
    convertToTypedClasses(classInfos: MetamodelClassInfo[]): TypedClass[] {
        return classInfos.map((info) => this.convertToTypedClass(info));
    }

    /**
     * Converts a single MetamodelClassInfo to TypedClass.
     *
     * @param info The class information to convert
     * @returns The TypedClass representation
     */
    private convertToTypedClass(info: MetamodelClassInfo): TypedClass {
        return {
            name: info.name,
            package: info.package,
            superClasses: [...info.superClasses],
            properties: info.properties.map((prop) => this.convertProperty(prop)),
            relations: info.relations.map((rel) => this.convertRelation(rel))
        };
    }

    /**
     * Converts a property to TypedProperty by resolving its type.
     *
     * @param prop The property information
     * @returns The TypedProperty with resolved type index
     */
    private convertProperty(prop: MetamodelPropertyInfo): TypedProperty {
        const type = this.typeDefinitions.resolveCustomClassOrLambdaType(prop.valueType);
        return {
            name: prop.name,
            typeIndex: this.typedAstConverter.getTypeIndexForType(type)
        };
    }

    /**
     * Converts a relation to TypedRelation by resolving its type.
     *
     * @param rel The relation information
     * @returns The TypedRelation with resolved type index
     */
    private convertRelation(rel: MetamodelRelationInfo): TypedRelation {
        const type = this.typeDefinitions.resolveCustomClassOrLambdaType(rel.valueType);
        return {
            property: rel.property,
            oppositeProperty: rel.oppositeProperty,
            oppositeClassName: rel.oppositeClassName,
            isOutgoing: rel.isOutgoing,
            typeIndex: this.typedAstConverter.getTypeIndexForType(type)
        };
    }
}
