import type { AstNodeDescription, ReferenceInfo } from "langium";
import {
    ID,
    type AstSerializer,
    type AstSerializerAdditionalServices,
    type ExtendedLangiumServices
} from "@mdeo/language-common";
import { sharedImport } from "../sharedImport.js";
import type { CompletionContext, CompletionValueItem } from "langium/lsp";

const { DefaultCompletionProvider } = sharedImport("langium/lsp");

/**
 * Base completion provider that serializes cross-reference candidate names using
 * `AstSerializer` before returning them as completion labels.
 *
 * This ensures identifiers that require backtick-quoting (those containing special
 * characters, dots, or matching reserved keywords) are suggested in their properly
 * escaped form and are therefore always valid when inserted into the source file.
 */
export class BaseCompletionProvider extends DefaultCompletionProvider {
    protected readonly astSerializer: AstSerializer;

    constructor(services: ExtendedLangiumServices & AstSerializerAdditionalServices) {
        super(services);
        this.astSerializer = services.AstSerializer;
    }

    protected override createReferenceCompletionItem(
        nodeDescription: AstNodeDescription,
        _refInfo: ReferenceInfo,
        _context: CompletionContext
    ): CompletionValueItem {
        const kind = this.nodeKindProvider.getCompletionItemKind(nodeDescription);
        const documentation = this.getReferenceDocumentation(nodeDescription);
        const insertText = this.astSerializer.serializePrimitive({ value: nodeDescription.name }, ID);
        return {
            nodeDescription,
            kind,
            documentation,
            detail: nodeDescription.type,
            sortText: "0",
            label: nodeDescription.name,
            insertText
        };
    }
}
