import type { LangiumDocument } from "langium";
import { DefaultDocumentPackageCacheService } from "@mdeo/language-expression";
import type { ScriptType } from "../grammar/scriptTypes.js";

/**
 * Script-specific package cache service that resolves the metamodel import
 * from the `metamodelImport.file` property of the Script root node.
 */
export class ScriptDocumentPackageCacheService extends DefaultDocumentPackageCacheService {
    protected override getMetamodelImportFile(document: LangiumDocument): string | undefined {
        const root = document.parseResult?.value as ScriptType | undefined;
        return root?.metamodelImport?.file;
    }
}
