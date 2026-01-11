import type { DefaultSharedCoreModuleContext } from "langium";
import type { Connection } from "vscode-languageserver";
import { URI } from "vscode-uri";
import type { FileSystemNode } from "langium";
import type { MetadataFileSystemProvider } from "@mdeo/language-common";
import {
    ReadFileRequest,
    StatRequest,
    ReadDirectoryRequest,
    ReadMetadataRequest,
    WriteMetadataRequest
} from "./protocol";

/**
 * Creates a file system provider that communicates with the client via LSP connection.
 *
 * This function provides a Langium-compatible file system implementation that delegates
 * file system operations (readFile, stat, readDirectory) to the LSP client through
 * custom protocol requests. Other file system methods throw "Not implemented" errors.
 *
 * @param connection The LSP connection to use for communicating with the client
 * @returns A DefaultSharedCoreModuleContext with the file system provider
 */
export function lspFileSystem(connection: Connection): DefaultSharedCoreModuleContext {
    return {
        fileSystemProvider: (): MetadataFileSystemProvider => ({
            readFile: async (uri: URI): Promise<string> => {
                const result = await connection.sendRequest(ReadFileRequest.type, { uri: uri.toString() });
                return result;
            },

            readFileSync: (): string => {
                throw new Error("Not implemented");
            },

            readBinary: async (): Promise<Uint8Array> => {
                throw new Error("Not implemented");
            },

            readBinarySync: (): Uint8Array => {
                throw new Error("Not implemented");
            },

            stat: async (uri: URI): Promise<FileSystemNode> => {
                const result = await connection.sendRequest(StatRequest.type, { uri: uri.toString() });
                return {
                    isFile: result.isFile,
                    isDirectory: result.isDirectory,
                    uri: URI.parse(result.uri)
                };
            },

            statSync: (): FileSystemNode => {
                throw new Error("Not implemented");
            },

            readDirectory: async (uri: URI): Promise<FileSystemNode[]> => {
                const result = await connection.sendRequest(ReadDirectoryRequest.type, { uri: uri.toString() });
                return result.map((node) => ({
                    isFile: node.isFile,
                    isDirectory: node.isDirectory,
                    uri: URI.parse(node.uri)
                }));
            },

            readDirectorySync: (): FileSystemNode[] => {
                throw new Error("Not implemented");
            },

            exists: async (): Promise<boolean> => {
                throw new Error("Not implemented");
            },

            existsSync: (): boolean => {
                throw new Error("Not implemented");
            },

            readMetadata: async (uri: URI): Promise<object> => {
                const result = await connection.sendRequest(ReadMetadataRequest.type, { uri: uri.toString() });
                return result;
            },

            writeMetadata: async (uri: URI, metadata: object): Promise<void> => {
                await connection.sendRequest(WriteMetadataRequest.type, { uri: uri.toString(), metadata });
            }
        })
    };
}
