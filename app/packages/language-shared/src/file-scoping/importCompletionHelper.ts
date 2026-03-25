import type { AstNode, CstNode, IndexManager, LangiumDocument, LangiumDocuments } from "langium";
import type { CompletionAcceptor, CompletionContext, CompletionValueItem } from "langium/lsp";
import type { AstSerializer } from "@mdeo/language-common";
import type { BaseType } from "@mdeo/language-common";
import type { TextEdit } from "vscode-languageserver-types";
import { resolveRelativePath } from "./util.js";
import { calculateRelativePath } from "../util/pathUtils.js";

/**
 * Structural type for a cross-reference on a file-import entity field.
 * Matches Langium's Reference<T> shape but allows the ref to be absent.
 */
export interface AstEntityImportRef {
    $refText?: string;
    ref?: { name?: string };
}

/**
 * Structural type for a single entity entry inside a file import statement.
 * E.g. the `Foo` or `Foo as Bar` part of `import { Foo as Bar } from "..."`.
 */
export interface AstEntityImport {
    entity: AstEntityImportRef;
    name?: string;
}

/**
 * Structural type for a file import statement node.
 * E.g. `import { Foo, Bar } from "./foo"`.
 */
export interface AstFileImportItem {
    file: string;
    $cstNode?: CstNode;
    imports: ReadonlyArray<AstEntityImport>;
}

/**
 * Abstract base class for providing file-import completion items.
 *
 * Subclasses configure the entity type, node type names, and services once in the constructor.
 * The language-specific filtering and completion item shape are defined by overriding
 * `shouldIncludeDocument` and `buildCompletionItem`.
 *
 * To produce completions, call `computeCompletions` with the document-specific context
 * (current document, its import list, and the anchor for new import insertion).
 *
 * @template E The entity type whose instances are searched in the workspace index.
 */
export abstract class FileImportCompletionHelper<E extends AstNode> {
    constructor(
        protected readonly entityType: BaseType<E>,
        protected readonly fileImportTypeName: string,
        protected readonly importTypeName: string,
        protected readonly astSerializer: AstSerializer,
        protected readonly indexManager: IndexManager,
        protected readonly langiumDocuments: LangiumDocuments
    ) {}

    /**
     * Whether to include a given target document when searching for entities to suggest.
     *
     * Override this for language-specific compatibility checks (e.g. metamodel compatibility).
     * The default implementation includes all documents.
     *
     * @param _targetDoc The document that would be imported from
     * @param _currentDoc The document currently being edited
     */
    protected shouldIncludeDocument(_targetDoc: LangiumDocument, _currentDoc: LangiumDocument): boolean {
        return true;
    }

    /**
     * Builds the completion item object for a given entity.
     *
     * Do NOT include `additionalTextEdits`; those are appended automatically.
     * Return `undefined` to skip this entity.
     *
     * @param entityName The name of the entity being suggested
     * @param relativePath The relative file path from the current document to the target document
     */
    protected abstract buildCompletionItem(entityName: string, relativePath: string): CompletionValueItem | undefined;

    /**
     * Computes and registers completion items for entities importable from other workspace documents.
     *
     * For each qualifying entity the method:
     * 1. Builds the completion item via `buildCompletionItem`.
     * 2. Builds an `additionalTextEdit` that inserts or updates the import statement using
     *    the `AstSerializer` for correctly formatted output.
     * 3. Passes the combined item to the LSP acceptor.
     *
     * @param context LSP completion context
     * @param acceptor LSP completion acceptor
     * @param document The document currently being edited
     * @param fileImports The container's current import statements (for deduplication and update)
     * @param getInsertAnchorCstNode Returns the CST node to insert a new import AFTER;
     *        returning `undefined` inserts at the start of the document
     * @param isEntityAlreadyAvailable Optional predicate: return `true` if the entity is already
     *        in scope by means other than the import from the target file
     */
    async computeCompletions(
        context: CompletionContext,
        acceptor: CompletionAcceptor,
        document: LangiumDocument,
        fileImports: ReadonlyArray<AstFileImportItem>,
        getInsertAnchorCstNode: () => CstNode | undefined,
        isEntityAlreadyAvailable?: (entityName: string) => boolean
    ): Promise<void> {
        const allDescriptions = this.indexManager.allElements(this.entityType.name).toArray();

        // Group by document URI so we load and filter each target document only once.
        const byDocument = new Map<string, typeof allDescriptions>();
        for (const desc of allDescriptions) {
            const key = desc.documentUri.toString();
            if (!byDocument.has(key)) byDocument.set(key, []);
            byDocument.get(key)!.push(desc);
        }

        const currentUriStr = document.uri.toString();
        const formattingOptions = this.astSerializer.guessFormattingOptions(document);

        for (const [docUriStr, descs] of byDocument) {
            if (docUriStr === currentUriStr) continue;

            const targetDoc = this.langiumDocuments.getDocument(descs[0].documentUri);
            if (targetDoc == undefined) continue;

            if (!this.shouldIncludeDocument(targetDoc, document)) continue;

            const relativePath = calculateRelativePath(document.uri.path, targetDoc.uri.path);
            const alreadyFromFile = getAlreadyImportedFromFile(fileImports, document, docUriStr);

            for (const desc of descs) {
                const entityName = desc.name;
                if (!entityName) continue;
                if (alreadyFromFile.has(entityName)) continue;
                if (isEntityAlreadyAvailable?.(entityName)) continue;

                const item = this.buildCompletionItem(entityName, relativePath);
                if (item == undefined) continue;

                try {
                    const textEdit = await _buildFileImportTextEdit(
                        document,
                        fileImports,
                        getInsertAnchorCstNode,
                        this.fileImportTypeName,
                        this.importTypeName,
                        this.astSerializer,
                        formattingOptions,
                        relativePath,
                        entityName,
                        docUriStr
                    );
                    acceptor(context, {
                        ...item,
                        additionalTextEdits: textEdit != undefined ? [textEdit] : []
                    });
                } catch {
                    // Ignore errors producing completions on incomplete documents
                }
            }
        }
    }
}

/**
 * Returns the set of entity names already imported from a specific target file.
 *
 * @param fileImports The import statements from the current document's container node
 * @param document The current document (used to resolve relative paths)
 * @param targetDocUriString The string form of the target document's URI
 */
export function getAlreadyImportedFromFile(
    fileImports: ReadonlyArray<AstFileImportItem>,
    document: LangiumDocument,
    targetDocUriString: string
): Set<string> {
    const names = new Set<string>();
    for (const fileImport of fileImports) {
        const resolvedUri = resolveRelativePath(document, fileImport.file).toString();
        if (resolvedUri === targetDocUriString) {
            for (const imp of fileImport.imports) {
                const name = imp.entity.$refText ?? imp.entity.ref?.name;
                if (name) names.add(name);
            }
        }
    }
    return names;
}

/**
 * Builds a `TextEdit` for inserting or updating an import statement.
 *
 * - If an import from `targetDocUriString` already exists in `fileImports`, the existing
 *   import node is serialized with the new entity appended and replaces the old CST node.
 * - Otherwise a new import node is serialized and inserted after `getInsertAnchorCstNode()`.
 *
 * Uses the `AstSerializer` to produce consistently formatted text.
 */
export async function buildFileImportTextEdit(
    document: LangiumDocument,
    fileImports: ReadonlyArray<AstFileImportItem>,
    getInsertAnchorCstNode: () => CstNode | undefined,
    fileImportTypeName: string,
    importTypeName: string,
    astSerializer: AstSerializer,
    relativePath: string,
    entityName: string,
    targetDocUriString: string
): Promise<TextEdit | undefined> {
    const formattingOptions = astSerializer.guessFormattingOptions(document);
    return _buildFileImportTextEdit(
        document,
        fileImports,
        getInsertAnchorCstNode,
        fileImportTypeName,
        importTypeName,
        astSerializer,
        formattingOptions,
        relativePath,
        entityName,
        targetDocUriString
    );
}

/** Internal implementation reusing a pre-computed formattingOptions instance. */
async function _buildFileImportTextEdit(
    document: LangiumDocument,
    fileImports: ReadonlyArray<AstFileImportItem>,
    getInsertAnchorCstNode: () => CstNode | undefined,
    fileImportTypeName: string,
    importTypeName: string,
    astSerializer: AstSerializer,
    formattingOptions: ReturnType<AstSerializer["guessFormattingOptions"]>,
    relativePath: string,
    entityName: string,
    targetDocUriString: string
): Promise<TextEdit | undefined> {
    const existingImport = fileImports.find(
        (imp) => resolveRelativePath(document, imp.file).toString() === targetDocUriString
    );

    if (existingImport != undefined) {
        return buildUpdateImportEdit(
            existingImport,
            fileImportTypeName,
            importTypeName,
            astSerializer,
            formattingOptions,
            document,
            entityName
        );
    } else {
        return buildNewImportEdit(
            getInsertAnchorCstNode(),
            fileImportTypeName,
            importTypeName,
            astSerializer,
            formattingOptions,
            document,
            relativePath,
            entityName
        );
    }
}

/**
 * Builds a TextEdit that replaces an existing file-import CST node with a new serialized
 * version that includes all existing imports plus the new entity appended at the end.
 */
async function buildUpdateImportEdit(
    existingImport: AstFileImportItem,
    fileImportTypeName: string,
    importTypeName: string,
    astSerializer: AstSerializer,
    formattingOptions: ReturnType<AstSerializer["guessFormattingOptions"]>,
    document: LangiumDocument,
    entityName: string
): Promise<TextEdit | undefined> {
    const cst = existingImport.$cstNode;
    if (cst == undefined) return undefined;

    const syntheticNode = {
        $type: fileImportTypeName,
        file: existingImport.file,
        imports: [
            ...existingImport.imports.map((imp) => ({
                $type: importTypeName,
                entity: { $refText: imp.entity.$refText ?? imp.entity.ref?.name ?? "" },
                ...(imp.name != undefined ? { name: imp.name } : {})
            })),
            { $type: importTypeName, entity: { $refText: entityName } }
        ]
    };

    const serialized = await astSerializer.serializeNode(
        syntheticNode as unknown as AstNode,
        document,
        formattingOptions
    );

    return { range: cst.range, newText: serialized };
}

/**
 * Builds a TextEdit that inserts a new file-import statement after the anchor CST node.
 * If no anchor is given, the statement is inserted at the very beginning of the document.
 */
async function buildNewImportEdit(
    anchorCstNode: CstNode | undefined,
    fileImportTypeName: string,
    importTypeName: string,
    astSerializer: AstSerializer,
    formattingOptions: ReturnType<AstSerializer["guessFormattingOptions"]>,
    document: LangiumDocument,
    relativePath: string,
    entityName: string
): Promise<TextEdit | undefined> {
    const syntheticNode = {
        $type: fileImportTypeName,
        file: relativePath,
        imports: [{ $type: importTypeName, entity: { $refText: entityName } }]
    };

    const serialized = await astSerializer.serializeNode(
        syntheticNode as unknown as AstNode,
        document,
        formattingOptions
    );

    const insertPosition = anchorCstNode != undefined ? anchorCstNode.range.end : { line: 0, character: 0 };

    return {
        range: { start: insertPosition, end: insertPosition },
        newText: "\n" + serialized
    };
}
