import {
    sharedImport,
    getExportedEntitiesFromGlobalScope,
    getImportedEntitiesFromCurrentFile,
    isImportReference,
    isReferenceToImport,
    createLocalScope
} from "@mdeo/language-shared";
import type { ReferenceInfo, Scope } from "langium";
import {
    Class,
    ClassOrImport,
    Enum,
    EnumOrImport,
    metamodelFileScopingConfig,
    type MetaModelType
} from "../grammar/metamodelTypes.js";
import type { AstReflection, ExtendedLangiumServices } from "@mdeo/language-common";

const { DefaultScopeProvider, AstUtils, EMPTY_SCOPE } = sharedImport("langium");

/**
 * The scope provider for the Metamodel language.
 *
 * This scope provider handles resolution of references to classes, supporting both:
 * - Local references to classes/enums defined in the same file
 * - Cross-file references via imports
 *
 * It distinguishes between:
 * - Import references (the entity being imported in an import statement)
 * - References to imported entities (usage of imported classes/enums in the model)
 */
export class MetamodelScopeProvider extends DefaultScopeProvider {
    /**
     * The AST reflection service for type checking and model introspection.
     */
    private readonly astReflection: AstReflection;

    /**
     * Constructs a new MetamodelScopeProvider.
     * @param services The extended Langium services.
     */
    constructor(services: ExtendedLangiumServices) {
        super(services);
        this.astReflection = services.shared.AstReflection;
    }

    override getScope(referenceInfo: ReferenceInfo): Scope {
        const document = AstUtils.getDocument(referenceInfo.container);
        const model = document.parseResult.value as MetaModelType;

        if (isImportReference(referenceInfo, metamodelFileScopingConfig)) {
            return getExportedEntitiesFromGlobalScope(
                document,
                referenceInfo,
                metamodelFileScopingConfig,
                this.indexManager
            );
        } else if (isReferenceToImport(referenceInfo, ClassOrImport, this.astReflection)) {
            const importScope = getImportedEntitiesFromCurrentFile(
                model.imports,
                this.nameProvider,
                this.descriptions,
                (imp) => this.astReflection.isInstance(imp.entity.ref, Class)
            );
            return createLocalScope(referenceInfo, document, this.astReflection, importScope);
        } else if (isReferenceToImport(referenceInfo, EnumOrImport, this.astReflection)) {
            const importScope = getImportedEntitiesFromCurrentFile(
                model.imports,
                this.nameProvider,
                this.descriptions,
                (imp) => this.astReflection.isInstance(imp.entity.ref, Enum)
            );
            return createLocalScope(referenceInfo, document, this.astReflection, importScope);
        }

        return EMPTY_SCOPE;
    }
}
