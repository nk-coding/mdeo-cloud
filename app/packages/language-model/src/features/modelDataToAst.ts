import type {
    ModelType,
    ObjectInstanceType,
    PropertyAssignmentType,
    LinkType,
    MetamodelFileImportType
} from "../grammar/modelTypes.js";
import type { ModelData, ModelDataInstance, ModelDataLink, ModelDataPropertyValue } from "./modelData.js";
import type { LangiumDocument } from "langium";

/**
 * Builds a synthetic Model AST from ModelData and a resolved metamodel relative path.
 * The resulting AST can be serialized using the model language's AstSerializer.
 *
 * @param modelData The model data to convert
 * @param metamodelRelativePath The relative path to the metamodel from the target file
 * @param enumEntryMap Map from enum entry name to enum type name
 * @returns A synthetic Model AST node
 */
export function buildModelAst(
    modelData: ModelData,
    metamodelRelativePath: string,
    enumEntryMap: Map<string, string>
): ModelType {
    const importNode = {
        $type: "MetamodelFileImport",
        file: metamodelRelativePath
    } as unknown as MetamodelFileImportType;

    const objects = modelData.instances.map((inst) => buildObjectInstance(inst, enumEntryMap));
    const links = modelData.links.map((link) => buildLink(link));

    return {
        $type: "Model",
        import: importNode,
        objects,
        links
    } as unknown as ModelType;
}

/**
 * Builds a synthetic ObjectInstance AST node from a ModelDataInstance.
 */
function buildObjectInstance(instance: ModelDataInstance, enumEntryMap: Map<string, string>): ObjectInstanceType {
    const properties = Object.entries(instance.properties)
        .filter(([, value]) => value !== null)
        .map(([name, value]) => buildPropertyAssignment(name, value!, enumEntryMap));

    return {
        $type: "ObjectInstance",
        name: instance.name,
        class: { $refText: instance.className },
        properties
    } as unknown as ObjectInstanceType;
}

/**
 * Builds a synthetic PropertyAssignment AST node.
 */
function buildPropertyAssignment(
    name: string,
    value: ModelDataPropertyValue | ModelDataPropertyValue[],
    enumEntryMap: Map<string, string>
): PropertyAssignmentType {
    return {
        $type: "PropertyAssignment",
        name: { $refText: name },
        value: buildLiteralValue(value, enumEntryMap)
    } as unknown as PropertyAssignmentType;
}

/**
 * Builds a synthetic literal value (SimpleValue, EnumValue, or ListValue).
 */
function buildLiteralValue(
    value: ModelDataPropertyValue | ModelDataPropertyValue[],
    enumEntryMap: Map<string, string>
): unknown {
    if (Array.isArray(value)) {
        return {
            $type: "ListValue",
            values: value.map((v) => buildSingleValue(v, enumEntryMap))
        };
    }
    return buildSingleValue(value, enumEntryMap);
}

/**
 * Builds a synthetic single value (SimpleValue or EnumValue).
 */
function buildSingleValue(value: ModelDataPropertyValue, enumEntryMap: Map<string, string>): unknown {
    if (value === null) {
        return { $type: "SimpleValue" };
    }
    if (typeof value === "string") {
        return { $type: "SimpleValue", stringValue: value };
    }
    if (typeof value === "number") {
        return { $type: "SimpleValue", numberValue: value };
    }
    if (typeof value === "boolean") {
        return { $type: "SimpleValue", booleanValue: value };
    }
    if (typeof value === "object" && "enum" in value) {
        return buildEnumValue(value.enum, enumEntryMap);
    }
    return { $type: "SimpleValue" };
}

/**
 * Builds a synthetic EnumValue AST node.
 * Resolves the enum type name from the entry name using the provided map.
 */
function buildEnumValue(entryName: string, enumEntryMap: Map<string, string>): unknown {
    // If the value already contains ".", it's in "EnumName.Entry" format
    if (entryName.includes(".")) {
        const dotIndex = entryName.indexOf(".");
        return {
            $type: "EnumValue",
            enumRef: { $refText: entryName.substring(0, dotIndex) },
            value: { $refText: entryName.substring(dotIndex + 1) }
        };
    }

    // Look up the enum type from the map
    const enumName = enumEntryMap.get(entryName);
    if (enumName) {
        return {
            $type: "EnumValue",
            enumRef: { $refText: enumName },
            value: { $refText: entryName }
        };
    }

    // Fallback: use entry name as both enum and entry
    return {
        $type: "EnumValue",
        enumRef: { $refText: entryName },
        value: { $refText: entryName }
    };
}

/**
 * Builds a synthetic Link AST node from a ModelDataLink.
 */
function buildLink(link: ModelDataLink): LinkType {
    const source: Record<string, unknown> = {
        $type: "LinkEnd",
        object: { $refText: link.sourceName }
    };
    if (link.sourceProperty) {
        source.property = { $refText: link.sourceProperty };
    }

    const target: Record<string, unknown> = {
        $type: "LinkEnd",
        object: { $refText: link.targetName }
    };
    if (link.targetProperty) {
        target.property = { $refText: link.targetProperty };
    }

    return {
        $type: "Link",
        source,
        target
    } as unknown as LinkType;
}

/**
 * Builds a map from enum entry names to their parent enum type names.
 * Scans all metamodel documents in the workspace.
 *
 * @param metamodelDocs Metamodel documents to scan
 * @returns Map from entry name to enum type name
 */
export function buildEnumEntryMap(metamodelDocs: LangiumDocument[]): Map<string, string> {
    const map = new Map<string, string>();
    for (const doc of metamodelDocs) {
        const root = doc.parseResult.value as unknown as Record<string, unknown>;
        const enums = root.enums as Array<{ name: string; entries: Array<{ name: string }> }> | undefined;
        if (!Array.isArray(enums)) continue;

        for (const enumDef of enums) {
            if (!enumDef.name || !Array.isArray(enumDef.entries)) continue;
            for (const entry of enumDef.entries) {
                if (entry.name) {
                    map.set(entry.name, enumDef.name);
                }
            }
        }
    }
    return map;
}

/**
 * Creates a minimal LangiumDocument for AST serialization.
 * The serializer needs a document for text/position lookups, but for
 * synthetic AST nodes these are not meaningful.
 */
export function createMinimalDocument(uri: string): LangiumDocument {
    return {
        textDocument: {
            getText: () => "",
            positionAt: () => ({ line: 0, character: 0 }),
            uri
        },
        uri: { path: uri, scheme: "fake", authority: "" }
    } as unknown as LangiumDocument;
}
