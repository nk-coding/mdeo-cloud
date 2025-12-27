import type { PluginContext } from "@mdeo/language-common";
import type { LangiumTypeCreator, TypirLangiumServices, TypirLangiumSpecifics } from "typir-langium";
import type { ExtendedTypirServices } from "../service/extendedTypirServices.js";
import type { ClassType } from "../config/type.js";
import type { TypeDefinitionListener, TypeDefinitionService } from "../service/type-definition-service.js";

/**
 * Generates a TypeCreator class that integrates with the TypeDefinitionService
 * to manage class type definitions based on document lifecycle events.
 *
 * @param context The plugin context providing access to shared dependencies
 * @returns An object containing the TypeCreator class provider
 */
export function generateTypeCreator<Specifics extends TypirLangiumSpecifics>(
    context: PluginContext
): {
    TypeCreator: (services: ExtendedTypirServices<Specifics> & TypirLangiumServices<Specifics>) => LangiumTypeCreator;
} {
    const typirLangium = context["typir-langium"];

    class ExtendedTypeCreator
        extends typirLangium.DefaultLangiumTypeCreator<Specifics>
        implements TypeDefinitionListener
    {
        private readonly documentTypeDefinitionMap: Map<string, ClassType[]> = new Map();
        private readonly typeDefinition: TypeDefinitionService;

        constructor(services: ExtendedTypirServices<Specifics> & TypirLangiumServices<Specifics>) {
            super(services);
            this.typeDefinition = services.TypeDefinitions;
            this.typeDefinition.addListener(this);
        }

        protected override invalidateTypesOfDocument(documentKey: string): void {
            super.invalidateTypesOfDocument(documentKey);
            (this.documentTypeDefinitionMap.get(documentKey) ?? []).forEach((classTypeToRemove) =>
                this.typeDefinition.removeClassType(classTypeToRemove)
            );
        }

        onAddedClassType(classType: ClassType): void {
            if (this.currentDocumentKey != undefined) {
                let types = this.documentTypeDefinitionMap.get(this.currentDocumentKey);
                if (types == undefined) {
                    types = [];
                    this.documentTypeDefinitionMap.set(this.currentDocumentKey, types);
                }
                types.push(classType);
            }
        }

        onRemovedClassType(): void {}
    }

    return {
        TypeCreator: (services) => new ExtendedTypeCreator(services)
    };
}
