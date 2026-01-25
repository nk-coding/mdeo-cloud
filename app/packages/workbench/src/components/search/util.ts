import { Uri } from "vscode";
import {
    QueryType,
    type ITextQuery,
    type ITextSearchMatch
} from "@codingame/monaco-vscode-api/vscode/vs/workbench/services/search/common/search";
import type { SearchMatch, FileSearchResult } from "./types";

/**
 * Creates a search query for the specified project
 *
 * @param project the id of the project to search in
 * @param searchText the text to search for
 * @param isRegex whether the search text is a regex
 * @param isCaseSensitive whether the search is case sensitive
 * @param isWholeWord whether to match whole words only
 * @param includePattern an optional include pattern for filtering
 * @returns a text search query object
 */
export function createSearchQuery(
    project: string,
    searchText: string,
    isRegex: boolean,
    isCaseSensitive: boolean,
    isWholeWord: boolean,
    includePattern?: string
): ITextQuery {
    return {
        type: QueryType.Text as QueryType.Text,
        contentPattern: {
            pattern: searchText,
            isRegExp: isRegex,
            isCaseSensitive: isCaseSensitive,
            isWordMatch: isWholeWord
        },
        folderQueries: [
            {
                folder: Uri.file(`${project}/files`)
            }
        ],
        ...(includePattern && {
            includePattern: {
                [includePattern]: true
            }
        })
    };
}

export function createFileSearchResult(resource: Uri, results: ITextSearchMatch<Uri>[] | undefined): FileSearchResult {
    const fileResult: FileSearchResult = {
        id: resource.path,
        resource: resource,
        results: undefined
    };

    fileResult.results =
        results?.map((match, index) => ({
            id: `${resource.path}-${index}`,
            previewText: match.previewText,
            range: match.rangeLocations[0],
            fileResult,
            index
        })) ?? [];

    return fileResult;
}

export function getFileName(uri: Uri): string {
    const pathSegments = uri.path.split("/").filter((s) => s.length > 0);
    return pathSegments[pathSegments.length - 1] ?? "";
}

export function getRelativePath(uri: Uri): string {
    const pathSegments = uri.path.split("/").filter((s) => s.length > 0);

    if (pathSegments.length <= 2) {
        return "";
    }
    if (pathSegments[1] === "files") {
        if (pathSegments.length <= 3) {
            return "";
        }
        return pathSegments.slice(2, -1).join("/");
    }
    return pathSegments.slice(1, -1).join("/");
}

export function getPreviewBefore(match: SearchMatch): string {
    if (match.previewText == undefined || !match.range) {
        return "";
    }
    const startOffset = match.range.preview.startColumn;
    return match.previewText.substring(0, startOffset);
}

export function getPreviewHighlight(match: SearchMatch): string {
    if (match.previewText == undefined || !match.range) {
        return "";
    }
    const startOffset = match.range.preview.startColumn;
    const endOffset = match.range.preview.endColumn;
    return match.previewText.substring(startOffset, endOffset);
}

export function getPreviewAfter(match: SearchMatch): string {
    if (match.previewText == undefined || !match.range) {
        return "";
    }
    const endOffset = match.range.preview.endColumn;
    return match.previewText.substring(endOffset);
}
