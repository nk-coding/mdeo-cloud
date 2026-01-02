/********************************************************************************
 * Copyright (c) 2020-2023 EclipseSource and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License v. 2.0 are satisfied: GNU General Public License, version 2
 * with the GNU Classpath Exception which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 ********************************************************************************/
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
