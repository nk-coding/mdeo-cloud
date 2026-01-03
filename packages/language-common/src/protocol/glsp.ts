import type {
    ActionMessage,
    DisposeClientSessionParameters,
    InitializeClientSessionParameters,
    InitializeParameters,
    InitializeResult
} from "@eclipse-glsp/protocol";
import { NotificationType, RequestType } from "vscode-jsonrpc";
import type { TextDocumentIdentifier } from "vscode-languageserver-types";

export namespace JsonrpcGLSPClient {
    export const ActionMessageNotification = new NotificationType<
        ActionMessage & { textDocument: TextDocumentIdentifier }
    >("process");
    export const InitializeRequest = new RequestType<
        InitializeParameters & { textDocument: TextDocumentIdentifier },
        InitializeResult,
        void
    >("initialize");
    export const InitializeClientSessionRequest = new RequestType<
        InitializeClientSessionParameters & { textDocument: TextDocumentIdentifier },
        void,
        void
    >("initializeClientSession");
    export const DisposeClientSessionRequest = new RequestType<
        DisposeClientSessionParameters & { textDocument: TextDocumentIdentifier },
        void,
        void
    >("disposeClientSession");
}
