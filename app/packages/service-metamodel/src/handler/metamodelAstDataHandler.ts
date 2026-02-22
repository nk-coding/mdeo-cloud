import type { MetamodelServices } from "@mdeo/language-metamodel";
import { MetaModel } from "@mdeo/language-metamodel";
import type { SingleMultiplicityType, RangeMultiplicityType } from "@mdeo/language-metamodel";
import { hasErrors, type FileDataHandler } from "@mdeo/service-common";
import { resolveRelativePath } from "@mdeo/language-shared";

/**
 * Key for the metamodel AST data handler.
 */
export const METAMODEL_AST_DATA_HANDLER_KEY = "metamodel";

/**
 * A resolved multiplicity with lower and upper bounds.
 * upper is -1 to represent an unbounded (∞) upper bound.
 */
export interface MultiplicityData {
    lower: number;
    upper: number;
}

/**
 * Data for a single enum definition in the metamodel.
 */
export interface EnumData {
    /**
     * The enum name.
     */
    name: string;
    /**
     * List of enum entry names.
     */
    entries: string[];
}

/**
 * Data for a single property of a metamodel class.
 */
export interface PropertyData {
    /**
     * The property name.
     */
    name: string;
    /**
     * The enum name if this property has an enum type.
     * Mutually exclusive with primitiveType.
     */
    enumType?: string;
    /**
     * The primitive type name if this property has a primitive type.
     * Mutually exclusive with enumType.
     */
    primitiveType?: string;
    /**
     * The multiplicity of the property, defaulting to {lower: 1, upper: 1} if not specified.
     */
    multiplicity: MultiplicityData;
}

/**
 * Data for a single metamodel class.
 */
export interface ClassData {
    /**
     * The class name.
     */
    name: string;
    /**
     * Whether the class is abstract.
     */
    isAbstract: boolean;
    /**
     * Names of parent classes this class extends.
     */
    extends: string[];
    /**
     * The properties defined on this class.
     */
    properties: PropertyData[];
}

/**
 * Data for one end of an association.
 */
export interface AssociationEndData {
    /**
     * The name of the class at this end.
     */
    class: string;
    /**
     * Optional role name for this end.
     */
    name?: string;
    /**
     * The multiplicity at this end, defaulting to {lower: 1, upper: 1} if not specified.
     */
    multiplicity: MultiplicityData;
}

/**
 * Data for a metamodel association.
 */
export interface AssociationData {
    source: AssociationEndData;
    operator: string;
    target: AssociationEndData;
}

/**
 * The result produced by the metamodel AST data handler.
 */
export interface MetamodelAstData {
    /**
     * All class definitions in the metamodel (enums are excluded).
     */
    classes: ClassData[];
    /**
     * All enum definitions in the metamodel.
     */
    enums: EnumData[];
    /**
     * All association definitions in the metamodel.
     */
    associations: AssociationData[];
    /**
     * Absolute file-system paths of all metamodel files imported by this metamodel.
     * These are resolved relative to the metamodel file's location.
     */
    importedMetamodelPaths: string[];
}

function convertMultiplicity(mult: SingleMultiplicityType | RangeMultiplicityType | undefined): MultiplicityData {
    if (mult == undefined) {
        return { lower: 1, upper: 1 };
    }

    const type = (mult as any).$type as string | undefined;

    if (type?.includes("Range")) {
        const range = mult as RangeMultiplicityType;
        const upper = range.upper === "*" ? -1 : (range.upperNumeric ?? 0);
        return { lower: range.lower ?? 0, upper };
    }

    const single = mult as SingleMultiplicityType;
    if (single.value === "*") return { lower: 0, upper: -1 };
    if (single.value === "+") return { lower: 1, upper: -1 };
    if (single.value === "?") return { lower: 0, upper: 1 };
    const n = single.numericValue;
    if (n != undefined) return { lower: n, upper: n };
    return { lower: 1, upper: 1 };
}

/**
 * File data handler that extracts structured class and association data from a
 * parsed metamodel document.  The result is suitable for downstream consumers
 * (e.g. optimisation services) that need a typed view of the metamodel without
 * dealing with the full Langium AST.
 */
export const metamodelAstDataHandler: FileDataHandler<MetamodelAstData | null, MetamodelServices> = async (context) => {
    const { instance, fileInfo, serverApi } = context;

    if (fileInfo == undefined) {
        return {
            data: null,
            ...serverApi.getTrackedRequests()
        };
    }

    const document = await instance.buildDocument(fileInfo.uri);

    if (hasErrors(document)) {
        return {
            data: null,
            ...serverApi.getTrackedRequests()
        };
    }

    const root = document.parseResult?.value;
    const reflection = instance.services.shared.AstReflection;
    if (!reflection.isInstance(root, MetaModel)) {
        throw new Error("Document root is not a MetaModel");
    }

    const importedMetamodelPaths: string[] = (root.imports ?? []).flatMap((importItem) => {
        const relPath = importItem.file;
        if (relPath == undefined) {
            return [];
        }
        return [resolveRelativePath(document, relPath).fsPath];
    });

    const classes: ClassData[] = [];
    const enums: EnumData[] = [];
    const associations: AssociationData[] = [];

    for (const element of root.elements) {
        const elementType = (element as any).$type as string | undefined;

        if (elementType === "Class") {
            const cls = element as any;
            classes.push({
                name: cls.name ?? "",
                isAbstract: cls.isAbstract ?? false,
                extends: (cls.extensions?.extensions ?? []).map((ext: any) => ext.class?.$refText ?? ""),
                properties: (cls.properties ?? []).map((prop: any) => {
                    const propType = (prop.type as any)?.$type as string | undefined;
                    const isEnumType = propType?.includes("EnumTypeReference");
                    const result: PropertyData = {
                        name: prop.name ?? "",
                        multiplicity: convertMultiplicity(prop.multiplicity)
                    };
                    if (isEnumType) {
                        result.enumType = (prop.type as any).enum?.$refText ?? "";
                    } else {
                        result.primitiveType = (prop.type as any)?.name ?? "";
                    }
                    return result;
                })
            });
        } else if (elementType === "Enum") {
            const enumDef = element as any;
            enums.push({
                name: enumDef.name ?? "",
                entries: (enumDef.entries ?? []).map((entry: any) => entry.name ?? "")
            });
        } else if (elementType === "Association") {
            const assoc = element as any;

            const mapEnd = (end: any): AssociationEndData => {
                const result: AssociationEndData = {
                    class: end.class?.$refText ?? "",
                    multiplicity: convertMultiplicity(end.multiplicity)
                };
                if (end.name != undefined) result.name = end.name;
                return result;
            };

            associations.push({
                source: mapEnd(assoc.source),
                operator: assoc.operator ?? "",
                target: mapEnd(assoc.target)
            });
        }
    }

    return {
        data: { classes, enums, associations, importedMetamodelPaths },
        ...serverApi.getTrackedRequests()
    };
};
