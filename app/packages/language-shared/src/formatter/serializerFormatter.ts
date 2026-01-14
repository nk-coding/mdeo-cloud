import type { AstSerializer, AstSerializerAdditionalServices } from "@mdeo/language-common";
import type { LangiumCoreServices, LangiumDocument, MaybePromise } from "langium";
import type { Formatter } from "langium/lsp";
import type { CancellationToken } from "vscode-jsonrpc";
import type {
    DocumentFormattingParams,
    DocumentRangeFormattingParams,
    DocumentOnTypeFormattingParams,
    DocumentOnTypeFormattingOptions
} from "vscode-languageserver-protocol";
import type { TextEdit as TextEditType } from "vscode-languageserver-types";
import { sharedImport } from "../sharedImport.js";

const { TextEdit, Range, uinteger } = sharedImport("vscode-languageserver-types");

/**
 * Serializer formatter for Langium that uses the AstSerializer service.
 */
export class SerializerFormatter implements Formatter {
    private readonly astSerializer: AstSerializer;

    constructor(protected readonly services: LangiumCoreServices & AstSerializerAdditionalServices) {
        this.astSerializer = services.AstSerializer;
    }

    async formatDocument(
        document: LangiumDocument,
        params: DocumentFormattingParams,
        cancelToken?: CancellationToken
    ): Promise<TextEditType[]> {
        const parseResult = document.parseResult;
        if (parseResult.lexerErrors.length > 0 || parseResult.parserErrors.length > 0) {
            return [];
        }
        const formatted = await this.astSerializer.serializeNode(parseResult.value, document, params.options);
        return [TextEdit.replace(Range.create(0, 0, uinteger.MAX_VALUE, uinteger.MAX_VALUE), formatted)];
    }

    async formatDocumentRange(
        document: LangiumDocument,
        params: DocumentRangeFormattingParams,
        cancelToken?: CancellationToken
    ): Promise<TextEditType[]> {
        throw new Error("Not implemented.");
    }

    formatDocumentOnType(
        document: LangiumDocument,
        params: DocumentOnTypeFormattingParams,
        cancelToken?: CancellationToken
    ): MaybePromise<TextEditType[]> {
        throw new Error("Not supported.");
    }

    get formatOnTypeOptions(): DocumentOnTypeFormattingOptions | undefined {
        return undefined;
    }
}
