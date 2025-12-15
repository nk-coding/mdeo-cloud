import type { IFileSystemProviderWithFileReadWriteCapability } from "@codingame/monaco-vscode-files-service-override";
import { FileSystemProviderCapabilities, FileType } from "@codingame/monaco-vscode-files-service-override";
import type { Ref } from "vue";
import type { Project } from "../project/project";
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

    constructor(
        private readonly backendApi: BackendApi,
        private readonly project: Ref<Project | undefined>
    ) {}

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

        const fileType = await this.backendApi.stat(projectId, path);

        if (fileType === FileType.File) {
            const content = await this.backendApi.readFile(projectId, path);
            return {
                type: FileType.File,
                mtime: Date.now(),
                ctime: Date.now(),
                size: content.byteLength
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
            const projects = await this.backendApi.getProjects();
            return projects.map((project) => [project.id, FileType.Directory]);
        }

        const { projectId, path } = this.parseUri(resource);
        return this.backendApi.readdir(projectId, path);
    }

    async readFile(resource: URI): Promise<Uint8Array> {
        const { projectId, path } = this.parseUri(resource);
        return this.backendApi.readFile(projectId, path);
    }

    async writeFile(resource: URI, content: Uint8Array, opts: IFileWriteOptions): Promise<void> {
        const { projectId, path } = this.parseUri(resource);
        await this.backendApi.writeFile(projectId, path, content, opts);
    }

    async mkdir(resource: URI): Promise<void> {
        const { projectId, path } = this.parseUri(resource);
        await this.backendApi.mkdir(projectId, path);
    }

    async delete(resource: URI, opts: IFileDeleteOptions): Promise<void> {
        const { projectId, path } = this.parseUri(resource);
        await this.backendApi.delete(projectId, path, opts);
    }

    async rename(from: URI, to: URI, opts: IFileOverwriteOptions): Promise<void> {
        const fromParsed = this.parseUri(from);
        const toParsed = this.parseUri(to);

        if (fromParsed.projectId !== toParsed.projectId) {
            throw new Error("Cannot rename across projects");
        }

        await this.backendApi.rename(fromParsed.projectId, fromParsed.path, toParsed.path, opts);
    }

    watch(resource: URI, opts: { recursive: boolean; excludes: readonly string[] }): IDisposable {
        return {
            dispose: () => {}
        };
    }

    dispose(): void {
        this._onDidChangeFile.dispose();
    }
}
