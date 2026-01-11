import type { Uri } from "vscode";
import type { SearchRangeSetPairing } from "@codingame/monaco-vscode-api/vscode/vs/workbench/services/search/common/search";
import type { TreeItem as TreeItemType } from "@/components/tree/util";

export interface SearchMatch extends TreeItemType {
    id: string;
    previewText?: string;
    range?: SearchRangeSetPairing;
    fileResult: FileSearchResult;
    index: number;
}

export interface FileSearchResult extends TreeItemType {
    id: string;
    resource: Uri;
    results?: SearchMatch[];
}
