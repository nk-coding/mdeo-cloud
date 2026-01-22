import type { MonacoApi } from "@/lib/monacoPlugin";
import { reactive, watch, markRaw } from "vue";
import type { Folder, FileSystemNode, File } from "./file";
import type { Project } from "../project/project";
import type { WorkbenchState } from "../workbenchState";
import { FileType } from "@codingame/monaco-vscode-files-service-override";
import type { FileOperationEvent } from "@codingame/monaco-vscode-api/vscode/vs/platform/files/common/files";
import {
    FileOperation,
    type IFileStatWithMetadata
} from "@codingame/monaco-vscode-api/vscode/vs/platform/files/common/files";
import { Uri } from "vscode";
import { getFileExtension } from "./util";

/**
 * Creates a reactive folder tree for a project that automatically updates
 * when file system operations occur.
 *
 * @param monacoApi Monaco API instance providing access to file service
 * @param workbenchState The workbench state manager
 * @returns A reactive folder representing the project's root directory
 */
export function useFileTree(monacoApi: MonacoApi, workbenchState: WorkbenchState): Folder {
    const project = workbenchState.project;
    const root = reactive<Folder>({
        id: "/",
        name: "",
        type: FileType.Directory,
        uri: markRaw(Uri.file("/")),
        children: [],
        parent: null
    });

    const loadTree = async (currentProject: Project) => {
        const projectUri = markRaw(Uri.file(`/${currentProject.id}/files`));
        root.name = currentProject.name;
        root.uri = projectUri;
        const children = await loadChildren(monacoApi, projectUri, root);
        root.children = children;
    };

    watch(
        project,
        async (newProject) => {
            if (newProject != undefined) {
                await loadTree(newProject);
            }
        },
        { immediate: true }
    );

    monacoApi.fileService.onDidRunOperation((event) => {
        handleFileOperation(monacoApi, root, event, project.value, workbenchState);
    });

    return root;
}

/**
 * Recursively loads all children of a directory from the file system.
 *
 * @param monacoApi Monaco API instance providing access to file service
 * @param uri Uri of the directory to load children from
 * @param parent Parent folder of the children being loaded
 * @returns Promise resolving to an array of child file system nodes
 */
async function loadChildren(monacoApi: MonacoApi, uri: Uri, parent: Folder): Promise<FileSystemNode[]> {
    const entries = await monacoApi.fileService.resolve(uri);
    if (entries.children == undefined) {
        return [];
    }

    const children: FileSystemNode[] = [];

    for (const entry of entries.children) {
        const childUri = markRaw(entry.resource);

        if (entry.isDirectory) {
            const folder = reactive<Folder>({
                id: childUri.path,
                name: entry.name,
                type: FileType.Directory,
                uri: childUri,
                children: [],
                parent
            });
            const subChildren = await loadChildren(monacoApi, childUri, folder);
            folder.children = subChildren;
            children.push(folder);
        } else {
            children.push(
                reactive<File>({
                    id: childUri.path,
                    name: entry.name,
                    type: FileType.File,
                    uri: childUri,
                    parent,
                    extension: getFileExtension(entry.name)
                })
            );
        }
    }

    return children;
}

/**
 * Handles file system operation events and updates the tree accordingly.
 *
 * @param monacoApi Monaco API instance providing access to file service
 * @param root Root folder of the file tree
 * @param event File operation event to handle
 * @param currentProject Currently active project
 * @param workbenchState The workbench state manager
 */
async function handleFileOperation(
    monacoApi: MonacoApi,
    root: Folder,
    event: FileOperationEvent,
    currentProject: Project | undefined,
    workbenchState: WorkbenchState
): Promise<void> {
    if (currentProject == undefined) {
        return;
    }

    const projectPrefix = `/${currentProject.id}/files`;

    if (event.operation === FileOperation.CREATE && event.target != undefined) {
        handleCreate(monacoApi, root, event.target, projectPrefix);
    } else if (event.operation === FileOperation.DELETE) {
        handleDelete(root, event.resource, projectPrefix, workbenchState);
    } else if (event.operation === FileOperation.MOVE && event.target != undefined) {
        handleMove(root, event.resource, event.target.resource, projectPrefix);
    } else if (event.operation === FileOperation.COPY && event.target != undefined) {
        handleCreate(monacoApi, root, event.target, projectPrefix);
    }
}

/**
 * Handles file/folder creation by adding the node to the tree.
 *
 * @param monacoApi Monaco API instance providing access to file service
 * @param root Root folder of the file tree
 * @param target The created file/folder
 * @param projectPrefix Uri prefix for the current project
 */
async function handleCreate(
    monacoApi: MonacoApi,
    root: Folder,
    target: IFileStatWithMetadata,
    projectPrefix: string
): Promise<void> {
    const resource = target.resource;
    const resourcePath = resource.path;
    if (!resourcePath.startsWith(projectPrefix)) {
        return;
    }

    const parent = findParentFolder(root, resource);
    if (parent == undefined) {
        return;
    }

    const name = getResourceName(resource);
    if (parent.children.some((child) => child.name === name)) {
        return;
    }

    const childUri = markRaw(resource);

    if (target.isDirectory) {
        const folder = reactive<Folder>({
            id: childUri.path,
            name,
            type: FileType.Directory,
            uri: childUri,
            children: [],
            parent
        });
        const children = await loadChildren(monacoApi, childUri, folder);
        folder.children = children;
        parent.children.push(folder);
    } else {
        parent.children.push(
            reactive<File>({
                id: childUri.path,
                name,
                type: FileType.File,
                uri: childUri,
                parent,
                extension: getFileExtension(name)
            })
        );
    }
}

/**
 * Handles file/folder deletion by removing the node from the tree.
 *
 * @param root Root folder of the file tree
 * @param resource Uri of the deleted resource
 * @param projectPrefix Uri prefix for the current project
 * @param workbenchState The workbench state manager
 */
function handleDelete(root: Folder, resource: Uri, projectPrefix: string, workbenchState: WorkbenchState): void {
    const resourcePath = resource.path;
    if (!resourcePath.startsWith(projectPrefix)) {
        return;
    }

    const parent = findParentFolder(root, resource);
    if (parent == null) {
        return;
    }

    const name = getResourceName(resource);
    const index = parent.children.findIndex((child) => child.name === name);
    if (index !== -1) {
        const deletedNode = parent.children[index];

        if (deletedNode) {
            closeTabsForNode(deletedNode, workbenchState);
        }

        parent.children.splice(index, 1);
    }
}

/**
 * Handles file/folder move by updating the node's parent and Uris.
 *
 * @param root Root folder of the file tree
 * @param oldResource Old Uri of the resource
 * @param newResource New Uri of the resource
 * @param projectPrefix Uri prefix for the current project
 */
function handleMove(root: Folder, oldResource: Uri, newResource: Uri, projectPrefix: string): void {
    const oldResourcePath = oldResource.path;
    if (!oldResourcePath.startsWith(projectPrefix)) {
        return;
    }

    const newResourcePath = newResource.path;
    if (!newResourcePath.startsWith(projectPrefix)) {
        return;
    }

    const oldParent = findParentFolder(root, oldResource);
    if (oldParent == undefined) {
        return;
    }

    const oldName = getResourceName(oldResource);
    const nodeIndex = oldParent.children.findIndex((child) => child.name === oldName);
    if (nodeIndex === -1) {
        return;
    }

    const node = oldParent.children[nodeIndex];
    if (node == undefined) {
        return;
    }

    const newParent = findParentFolder(root, newResource);
    if (newParent == undefined) {
        return;
    }

    const newName = getResourceName(newResource);

    oldParent.children.splice(nodeIndex, 1);

    node.name = newName;
    node.uri = markRaw(newResource);
    node.parent = newParent;

    if (node.type === FileType.Directory) {
        updateDescendantUris(node, oldResource, newResource);
    }

    newParent.children.push(node);
}

/**
 * Recursively updates Uris for all descendants of a moved folder.
 *
 * @param folder The folder whose descendants to update
 * @param oldBaseUri The old base Uri
 * @param newBaseUri The new base Uri
 */
function updateDescendantUris(folder: Folder, oldBaseUri: Uri, newBaseUri: Uri): void {
    for (const child of folder.children) {
        const oldPath = child.uri.path;
        const oldBasePath = oldBaseUri.path;
        const newBasePath = newBaseUri.path;

        const newPath = oldPath.replace(oldBasePath, newBasePath);
        child.uri = markRaw(Uri.file(newPath));

        if (child.type === FileType.Directory) {
            updateDescendantUris(child, oldBaseUri, newBaseUri);
        }
    }
}

/**
 * Closes all tabs associated with a file or folder (recursively).
 *
 * @param node The file or folder node
 * @param workbenchState The workbench state manager
 */
function closeTabsForNode(node: FileSystemNode, workbenchState: WorkbenchState): void {
    const tabsToClose: number[] = [];
    const nodePath = node.uri.path;

    workbenchState.tabs.value.forEach((tab, index) => {
        const tabPath = tab.fileUri.path;
        if (tabPath === nodePath || tabPath.startsWith(nodePath + "/")) {
            tabsToClose.push(index);
        }
    });

    for (let i = tabsToClose.length - 1; i >= 0; i--) {
        const tabIndex = tabsToClose[i];
        if (tabIndex === undefined) {
            continue;
        }

        const closedTab = workbenchState.tabs.value[tabIndex];
        if (!closedTab) {
            continue;
        }

        if (workbenchState.activeTab.value === closedTab) {
            const newActiveIndex =
                tabIndex > 0 ? tabIndex - 1 : tabIndex < workbenchState.tabs.value.length - 1 ? tabIndex : -1;
            if (newActiveIndex >= 0) {
                workbenchState.activeTab.value = workbenchState.tabs.value[newActiveIndex];
            } else {
                workbenchState.activeTab.value = undefined;
            }
        }

        workbenchState.tabs.value.splice(tabIndex, 1);
    }
}

/**
 * Finds the parent folder of a given resource in the tree.
 *
 * @param root Root folder of the file tree
 * @param resource Uri of the resource whose parent to find
 * @returns The parent folder, or null if not found
 */
function findParentFolder(root: Folder, resource: Uri): Folder | null {
    const path = resource.path;
    const segments = path.split("/").filter((s) => s.length > 0);

    if (segments.length <= 2) {
        return null;
    }

    let current: FileSystemNode = root;

    for (let i = 2; i < segments.length - 1; i++) {
        if (current.type !== FileType.Directory) {
            return null;
        }

        const child: FileSystemNode | undefined = current.children.find((c) => c.name === segments[i]);
        if (!child) {
            return null;
        }

        current = child;
    }

    return current.type === FileType.Directory ? current : null;
}

/**
 * Extracts the name of a resource from its Uri.
 *
 * @param resource Uri of the resource
 * @returns The name of the resource
 */
function getResourceName(resource: Uri): string {
    const path = resource.path;
    const segments = path.split("/").filter((s) => s.length > 0);
    return segments[segments.length - 1] || "";
}
