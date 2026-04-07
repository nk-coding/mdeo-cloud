import { sharedImport, DefaultExistingNamesProvider } from "@mdeo/language-shared";
import { collectImportedMetamodels } from "../importHelpers.js";
import { Class, Enum } from "../../grammar/metamodelTypes.js";

const { injectable } = sharedImport("inversify");

/**
 * Metamodel-specific implementation of {@link ExistingNamesProvider}.
 *
 * <p>Extends the default implementation by also including names of classes and
 * enums from all transitively imported metamodel files. This prevents naming
 * conflicts with types that are visible in the current document's scope via
 * the import mechanism, even though they are not exported by the document
 * itself.
 */
@injectable()
export class MetamodelExistingNamesProvider extends DefaultExistingNamesProvider {
    override async getExistingNames(): Promise<Set<string>> {
        const names = await super.getExistingNames();

        const document = this.modelState.sourceModel?.$document;
        if (!document) {
            return names;
        }

        const documents = this.modelState.languageServices.shared.workspace.LangiumDocuments;
        const importedModels = collectImportedMetamodels(document, documents);
        for (const model of importedModels) {
            for (const element of model.elements ?? []) {
                if ((element.$type === Class.name || element.$type === Enum.name) && "name" in element) {
                    names.add(element.name as string);
                }
            }
        }

        return names;
    }
}
