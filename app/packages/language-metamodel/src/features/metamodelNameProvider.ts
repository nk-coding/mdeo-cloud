import type { AstReflection, ExtendedLangiumServices } from "@mdeo/language-common";
import { sharedImport } from "@mdeo/language-shared";
import type { AstNode } from "langium";
import { ClassOrEnumImport } from "../grammar/metamodelTypes.js";

const { DefaultNameProvider } = sharedImport("langium");

/**
 * The name provider for the Metamodel language
 * Extends the default name provider to handle ClassImport nodes.
 */
export class MetamodelNameProvider extends DefaultNameProvider {
    private readonly reflection: AstReflection;

    constructor(services: ExtendedLangiumServices) {
        super();
        this.reflection = services.shared.AstReflection;
    }

    override getName(node: AstNode): string | undefined {
        const name = super.getName(node);
        if (name != undefined) {
            return name;
        }
        if (this.reflection.isInstance(node, ClassOrEnumImport)) {
            return node.name ?? node.entity.$refText;
        }
        return undefined;
    }
}
