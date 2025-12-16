import { type IReference, type ITextFileEditorModel, createModelReference } from "@codingame/monaco-vscode-api/monaco";
import type { ICodeEditorViewState } from "@codingame/monaco-vscode-api/vscode/vs/editor/common/editorCommon";
import type { IDisposable } from "monaco-editor";
import type { Uri } from "vscode";

/**
 * Manages the state of a Monaco editor instance for a specific file.
 *
 * This class encapsulates the editor's model reference and view state,
 * providing lifecycle management and URI updates for editor instances.
 * It implements IDisposable to ensure proper cleanup of resources.
 */
export class EditorState implements IDisposable {
    /**
     * Reference to the Monaco text file editor model.
     * This holds the actual document content and manages file operations.
     */
    modelReference: IReference<ITextFileEditorModel> | undefined;

    /**
     * The current view state of the editor (cursor position, scroll position, etc.).
     * Used to restore the editor state when switching between files.
     */
    viewState: ICodeEditorViewState | undefined;

    /**
     * Creates a new editor state for a given file URI.
     *
     * @param uri - The URI of the file this editor state represents
     */
    constructor(public uri: Uri) {}

    /**
     * Updates the URI of the editor and recreates the model reference with a new language.
     *
     * This method disposes of the existing model reference and creates a new one
     * for the updated URI, setting the appropriate language ID for syntax highlighting.
     *
     * @param newUri - The new URI to associate with this editor state
     * @param language - The language ID to set for the new model (e.g., 'typescript', 'javascript')
     */
    async updateUri(newUri: Uri, language: string) {
        this.uri = newUri;
        this.modelReference?.dispose();
        const modelReference = await createModelReference(newUri);
        modelReference.object.onDidChangeContent(() => {
            modelReference.object.save();
        });
        modelReference.object.setLanguageId(language);
        this.modelReference = modelReference;
    }

    /**
     * Disposes of the editor state and releases associated resources.
     *
     * This method should be called when the editor state is no longer needed
     * to prevent memory leaks.
     */
    dispose(): void {
        this.modelReference?.dispose();
    }
}
