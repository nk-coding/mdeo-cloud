import type { LangiumCoreServices, LangiumDocument, MaybePromise } from "langium";
import type { PluginContext } from "../../plugin/pluginContext.js";
import type { AstSerializer, AstSerializerAdditionalServices } from "../serialization/astSerializer.js";
import type { Formatter } from "langium/lsp";
import type { CancellationToken } from "vscode-jsonrpc";
import {
    type DocumentFormattingParams,
    type DocumentRangeFormattingParams,
    type DocumentOnTypeFormattingParams,
    type DocumentOnTypeFormattingOptions,
    type FormattingOptions,
    Range,
    uinteger
} from "vscode-languageserver-protocol";
import { TextEdit } from "vscode-languageserver-types";

/**
 * Generates a serializer formatter for Langium that uses the AstSerializer service.
 *
 * @param context The plugin context
 * @returns An provider for the Formatter service
 */
export function generateSerializerFormatter(context: PluginContext): {
    Formatter: (services: LangiumCoreServices & AstSerializerAdditionalServices) => Formatter;
} {
    class SerializerFormatter implements Formatter {
        private readonly astSerializer: AstSerializer;

        constructor(protected readonly services: LangiumCoreServices & AstSerializerAdditionalServices) {
            this.astSerializer = services.AstSerializer;
        }

        async formatDocument(
            document: LangiumDocument,
            params: DocumentFormattingParams,
            cancelToken?: CancellationToken
        ): Promise<TextEdit[]> {
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
        ): Promise<TextEdit[]> {
            throw new Error("Not implemented.");
        }

        formatDocumentOnType(
            document: LangiumDocument,
            params: DocumentOnTypeFormattingParams,
            cancelToken?: CancellationToken
        ): MaybePromise<TextEdit[]> {
            throw new Error("Not supported.");
        }

        get formatOnTypeOptions(): DocumentOnTypeFormattingOptions | undefined {
            return undefined;
        }
    }

    return {
        Formatter: (services: LangiumCoreServices & AstSerializerAdditionalServices) =>
            new SerializerFormatter(services)
    };
}
