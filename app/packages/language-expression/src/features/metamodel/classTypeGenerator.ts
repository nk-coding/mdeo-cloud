import type { ClassType, Member } from "../../typir-extensions/config/type.js";
import type { MetamodelClassInfo, MetamodelPropertyInfo, MetamodelRelationInfo } from "./metamodelClassInfo.js";

/**
 * Generates ClassType definitions from MetamodelClassInfo for the Typir type system.
 * This function creates a new generator instance, processes the classes, and returns the results.
 * The generator should not be reused.
 *
 * @param classInfos The intermediate class representations to convert
 * @returns Array of ClassType definitions for Typir
 */
export function generateClassTypes(classInfos: MetamodelClassInfo[]): ClassType[] {
    const generator = new ClassTypeGenerator();
    return generator.generateClassTypes(classInfos);
}

/**
 * Internal class for converting MetamodelClassInfo to ClassType for the Typir type system.
 * Should not be instantiated directly; use generateClassTypes() instead.
 */
class ClassTypeGenerator {
    /**
     * Converts a list of MetamodelClassInfo to ClassType definitions.
     *
     * @param classInfos The intermediate class representations to convert
     * @returns Array of ClassType definitions for Typir
     */
    generateClassTypes(classInfos: MetamodelClassInfo[]): ClassType[] {
        return classInfos.map((info) => this.generateClassType(info));
    }

    /**
     * Converts a single MetamodelClassInfo to a ClassType.
     *
     * @param info The class information to convert
     * @returns The generated ClassType
     */
    private generateClassType(info: MetamodelClassInfo): ClassType {
        const members: Record<string, Member> = {};

        for (const prop of info.properties) {
            members[prop.name] = this.createPropertyMember(prop);
        }

        for (const rel of info.relations) {
            members[rel.property] = this.createRelationMember(rel);
        }

        return {
            name: info.name,
            package: info.package,
            members,
            superTypes: info.superClasses.map((superClass) => ({
                type: superClass
            }))
        };
    }

    /**
     * Creates a Member definition from a property.
     *
     * @param prop The property information
     * @returns The Member definition
     */
    private createPropertyMember(prop: MetamodelPropertyInfo): Member {
        return {
            name: prop.name,
            isProperty: true,
            type: prop.valueType
        };
    }

    /**
     * Creates a Member definition from a relation.
     * Type references are already fully qualified in the relation info.
     *
     * @param rel The relation information
     * @returns The Member definition
     */
    private createRelationMember(rel: MetamodelRelationInfo): Member {
        return {
            name: rel.property,
            isProperty: true,
            type: rel.valueType
        };
    }
}
