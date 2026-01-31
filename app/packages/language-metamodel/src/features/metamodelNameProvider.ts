import type { ExtendedLangiumServices } from "@mdeo/language-common";
import { sharedImport } from "@mdeo/language-shared";

const { DefaultNameProvider } = sharedImport("langium");

/**
 * The name provider for the Metamodel language.
 * Extends the default name provider to handle metamodel-specific nodes.
 */
export class MetamodelNameProvider extends DefaultNameProvider {
    /**
     * Constructs a new MetamodelNameProvider.
     *
     * @param _services The extended Langium services
     */
    constructor(_services: ExtendedLangiumServices) {
        super();
    }
}
