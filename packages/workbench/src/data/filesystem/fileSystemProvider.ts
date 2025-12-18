import type { IFileSystemProviderWithFileReadWriteCapability } from "@codingame/monaco-vscode-files-service-override";
import { FileSystemProviderCapabilities, FileType } from "@codingame/monaco-vscode-files-service-override";
import type { BackendApi } from "../api/backendApi";
import type {
    IFileDeleteOptions,
    IFileOverwriteOptions,
    IFileWriteOptions
} from "@codingame/monaco-vscode-api/vscode/vs/platform/files/common/files";
import type { IDisposable } from "@codingame/monaco-vscode-api/vscode/vs/base/common/lifecycle";
import type { Event } from "@codingame/monaco-vscode-api/vscode/vs/base/common/event";
import { Emitter } from "@codingame/monaco-vscode-api/vscode/vs/base/common/event";
import type { URI } from "@codingame/monaco-vscode-api/vscode/vs/base/common/uri";
import type { IFileChange } from "@codingame/monaco-vscode-api/vscode/vs/platform/files/common/files";
import type { IStat } from "@codingame/monaco-vscode-api/vscode/vs/platform/files/common/files";
import { FileChangeType } from "@codingame/monaco-vscode-api/vscode/vs/platform/files/common/files";
import {
    createFileSystemProviderError,
    FileSystemProviderErrorCode
} from "@codingame/monaco-vscode-api/vscode/vs/platform/files/common/files";
import type { FileSystemError, ProjectError } from "../api/apiResult";
import { FileSystemErrorCode, ProjectErrorCode, CommonErrorCode } from "../api/apiResult";

/**
 * Converts an API error to a VSCode FileSystemProvider error.
 */
function apiErrorToVSCodeError(error: FileSystemError | ProjectError): Error {
    let errorCode: FileSystemProviderErrorCode;

    switch (error.code) {
        case FileSystemErrorCode.FileNotFound:
        case ProjectErrorCode.ProjectNotFound:
            errorCode = FileSystemProviderErrorCode.FileNotFound;
            break;
        case FileSystemErrorCode.FileExists:
            errorCode = FileSystemProviderErrorCode.FileExists;
            break;
        case FileSystemErrorCode.FileIsADirectory:
            errorCode = FileSystemProviderErrorCode.FileIsADirectory;
            break;
        case FileSystemErrorCode.FileNotADirectory:
            errorCode = FileSystemProviderErrorCode.FileNotADirectory;
            break;
        case CommonErrorCode.Unavailable:
            errorCode = FileSystemProviderErrorCode.Unavailable;
            break;
        case FileSystemErrorCode.DirectoryNotEmpty:
        case CommonErrorCode.Unknown:
        default:
            errorCode = FileSystemProviderErrorCode.Unknown;
            break;
    }

    return createFileSystemProviderError(error.message, errorCode);
}

/**
 * File system provider that forwards operations to a backend API.
 *
 * URIs are expected to have the format: schema://projectId/path
 * This provider extracts the projectId and path from the URI and forwards
 * the operation to the backend API.
 */
export class BackendFileSystemProvider implements IFileSystemProviderWithFileReadWriteCapability {
    readonly capabilities = FileSystemProviderCapabilities.FileReadWrite;

    private readonly _onDidChangeFile = new Emitter<readonly IFileChange[]>();
    readonly onDidChangeFile: Event<readonly IFileChange[]> = this._onDidChangeFile.event;

    readonly onDidChangeCapabilities = new Emitter<void>().event;

    constructor(private readonly backendApi: BackendApi) {}

    /**
     * Parse a URI into projectId and path components.
     * Expected format: schema://projectId/path
     */
    private parseUri(uri: URI): { projectId: string; path: string } {
        const fullPath = uri.path;
        const parts = fullPath.substring(1).split("/");
        const projectId = parts[0];

        if (!projectId) {
            throw new Error("Invalid URI: missing project ID");
        }

        const path = "/" + parts.slice(1).join("/");

        return { projectId, path };
    }

    async stat(resource: URI): Promise<IStat> {
        const fullPath = resource.path;

        if (fullPath === "/" || fullPath === "") {
            return {
                type: FileType.Directory,
                mtime: Date.now(),
                ctime: Date.now(),
                size: 0
            };
        }

        const { projectId, path } = this.parseUri(resource);

        if (path === "/" || path === "") {
            return {
                type: FileType.Directory,
                mtime: Date.now(),
                ctime: Date.now(),
                size: 0
            };
        }

        const fileTypeResult = await this.backendApi.stat(projectId, path);
        if (!fileTypeResult.success) {
            throw apiErrorToVSCodeError(fileTypeResult.error);
        }

        if (fileTypeResult.value === FileType.File) {
            const contentResult = await this.backendApi.readFile(projectId, path);
            if (!contentResult.success) {
                throw apiErrorToVSCodeError(contentResult.error);
            }
            return {
                type: FileType.File,
                mtime: Date.now(),
                ctime: Date.now(),
                size: contentResult.value.byteLength
            };
        } else {
            return {
                type: FileType.Directory,
                mtime: Date.now(),
                ctime: Date.now(),
                size: 0
            };
        }
    }

    async readdir(resource: URI): Promise<[string, FileType][]> {
        const fullPath = resource.path;

        if (fullPath === "/" || fullPath === "") {
            const projectsResult = await this.backendApi.getProjects();
            if (!projectsResult.success) {
                throw apiErrorToVSCodeError(projectsResult.error);
            }
            return projectsResult.value.map((project) => [project.id, FileType.Directory]);
        }

        const { projectId, path } = this.parseUri(resource);
        const result = await this.backendApi.readdir(projectId, path);
        if (!result.success) {
            throw apiErrorToVSCodeError(result.error);
        }
        return result.value;
    }

    async readFile(resource: URI): Promise<Uint8Array> {
        const { projectId, path } = this.parseUri(resource);
        const result = await this.backendApi.readFile(projectId, path);
        if (!result.success) {
            throw apiErrorToVSCodeError(result.error);
        }
        return result.value;
    }

    async writeFile(resource: URI, content: Uint8Array, opts: IFileWriteOptions): Promise<void> {
        const { projectId, path } = this.parseUri(resource);

        let exists: boolean;
        try {
            const statResult = await this.backendApi.stat(projectId, path);
            exists = statResult.success;
        } catch {
            exists = false;
        }

        const result = await this.backendApi.writeFile(projectId, path, content, opts);
        if (!result.success) {
            throw apiErrorToVSCodeError(result.error);
        }

        if (!exists && opts.create) {
            this._onDidChangeFile.fire([{ type: FileChangeType.ADDED, resource }]);
        } else {
            this._onDidChangeFile.fire([{ type: FileChangeType.UPDATED, resource }]);
        }
    }

    async mkdir(resource: URI): Promise<void> {
        const { projectId, path } = this.parseUri(resource);
        const result = await this.backendApi.mkdir(projectId, path);
        if (!result.success) {
            throw apiErrorToVSCodeError(result.error);
        }

        this._onDidChangeFile.fire([{ type: FileChangeType.ADDED, resource }]);
    }

    async delete(resource: URI, opts: IFileDeleteOptions): Promise<void> {
        const { projectId, path } = this.parseUri(resource);
        const result = await this.backendApi.delete(projectId, path, opts);
        if (!result.success) {
            throw apiErrorToVSCodeError(result.error);
        }

        this._onDidChangeFile.fire([{ type: FileChangeType.DELETED, resource }]);
    }

    async rename(from: URI, to: URI, opts: IFileOverwriteOptions): Promise<void> {
        const fromParsed = this.parseUri(from);
        const toParsed = this.parseUri(to);

        if (fromParsed.projectId !== toParsed.projectId) {
            throw new Error("Cannot rename across projects");
        }

        const result = await this.backendApi.rename(fromParsed.projectId, fromParsed.path, toParsed.path, opts);
        if (!result.success) {
            throw apiErrorToVSCodeError(result.error);
        }

        this._onDidChangeFile.fire([
            { type: FileChangeType.DELETED, resource: from },
            { type: FileChangeType.ADDED, resource: to }
        ]);
    }

    watch(_resource: URI, _opts: { recursive: boolean; excludes: readonly string[] }): IDisposable {
        return {
            dispose: () => {}
        };
    }

    dispose(): void {
        this._onDidChangeFile.dispose();
    }
}
