import { ImportEntityActionHandler } from "./importEntityActionHandler.js";
import type { LangiumDocument } from "langium";
import type { EnumType, MetaModelType } from "../grammar/metamodelTypes.js";

/**
 * Handler for the "import-enum" action in the Metamodel language.
 *
 * This handler extends ImportEntityActionHandler to provide enum-specific
 * import functionality with a two-step dialog:
 * 1. Select the metamodel file to import from
 * 2. Select the enum and optional alias
 */
export class ImportEnumActionHandler extends ImportEntityActionHandler {
    protected override getEntityTypeName(): string {
        return "Enum";
    }

    /**
     * Extracts enum names from a metamodel document.
     *
     * @param document The document to extract enums from
     * @returns Array of enum names
     */
    protected override extractEntityNames(document: LangiumDocument): string[] {
        const parseResult = document.parseResult;
        if (parseResult == undefined || parseResult.value == undefined) {
            return [];
        }

        const root = parseResult.value as MetaModelType;
        if (root.elements == undefined) {
            return [];
        }

        const enumNames = root.elements
            .filter((item): item is EnumType => item.$type === "Enum")
            .map((enumItem) => enumItem.name)
            .filter((name): name is string => name != undefined);

        return [...new Set(enumNames)];
    }
}
