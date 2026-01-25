import { ImportEntityActionHandler } from "./importEntityActionHandler.js";
import type { LangiumDocument } from "langium";
import type { ClassType, MetaModelType } from "../grammar/metamodelTypes.js";

/**
 * Handler for the "import-class" action in the Metamodel language.
 *
 * This handler extends ImportEntityActionHandler to provide class-specific
 * import functionality with a two-step dialog:
 * 1. Select the metamodel file to import from
 * 2. Select the class and optional alias
 */
export class ImportClassActionHandler extends ImportEntityActionHandler {
    protected override getEntityTypeName(): string {
        return "Class";
    }

    protected override extractEntityNames(document: LangiumDocument): string[] {
        const parseResult = document.parseResult;
        if (parseResult == undefined || parseResult.value == undefined) {
            return [];
        }

        const root = parseResult.value as MetaModelType;
        if (root.elements == undefined) {
            return [];
        }

        const classNames = root.elements
            .filter((item): item is ClassType => item.$type === "Class")
            .map((cls) => cls.name)
            .filter((name): name is string => name != undefined);

        return [...new Set(classNames)];
    }
}
