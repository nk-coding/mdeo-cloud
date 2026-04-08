import { getFileExtension } from "@/data/filesystem/util";
import type { ResolvedWorkbenchLanguagePlugin } from "@/data/plugin/plugin";
import type { EditorTab } from "@/data/tab/editorTab";
import { type IReference, type ITextFileEditorModel, createModelReference } from "@codingame/monaco-vscode-api/monaco";
import type { ICodeEditorViewState } from "@codingame/monaco-vscode-api/vscode/vs/editor/common/editorCommon";
import type { IStandaloneCodeEditor } from "@codingame/monaco-vscode-api/vscode/vs/editor/standalone/browser/standaloneCodeEditor";
import { FileCategory, parseUri, type ParsedUri } from "@mdeo/language-common";
import type { IDisposable, IRange } from "monaco-editor";
import type { Uri } from "vscode";
import { computed, watch, type ComputedRef, type ShallowRef } from "vue";

/**
 * An editor state representing the state of an open editor tab.
 * Manages reactive state of both the tab and loaded plugins.
 */
export class EditorState implements IDisposable {
    /**
     * The state of the textual editor associated with this editor state.
     */
    private textualEditorState: TextualEditorState | undefined;

    /**
     * Indicates whether this editor tab is currently active.
     */
    private isActive: boolean = false;

    /**
     * A selection range to reveal once the Monaco model is loaded into the editor.
     * Set by {@link setPendingSelection} and consumed (and cleared) by
     * {@link updateModelIfActive} when the model is actually applied.
     */
    private pendingSelection: IRange | undefined;

    /**
     * The parsed URI of the file opened in this editor tab.
     */
    parsedUri: ComputedRef<ParsedUri>;

    /**
     * The language plugin resolved for the file opened in this editor tab.
     */
    languagePlugin: ComputedRef<ResolvedWorkbenchLanguagePlugin | undefined>;

    /**
     * Indicates whether the file opened in this editor tab can be edited using a graphical editor.
     */
    hasGraphicalEditor: ComputedRef<boolean>;

    /**
     * Indicates whether the file opened in this editor tab can be edited using a textual editor.
     */
    hasTextualEditor: ComputedRef<boolean>;

    /**
     * Indicates whether the file opened in this editor tab can be viewed as Markdown.
     */
    hasMarkdownViewer: ComputedRef<boolean>;

    /**
     * Creates a new EditorState instance.
     *
     * @param tab The editor tab associated with this state
     * @param languagePluginByExtension A computed map of file extensions to language plugins
     * @param editor The Monaco standalone code editor instance
     */
    constructor(
        readonly tab: EditorTab,
        languagePluginByExtension: ComputedRef<Map<string, ResolvedWorkbenchLanguagePlugin>>,
        private readonly editor: ShallowRef<IStandaloneCodeEditor | undefined>
    ) {
        this.parsedUri = computed(() => parseUri(tab.fileUri));
        this.languagePlugin = computed(() => {
            const parsed = this.parsedUri.value;
            if (parsed.category === FileCategory.ExecutionSummary) {
                return undefined;
            }
            return languagePluginByExtension.value.get(getFileExtension(parsed.path));
        });
        this.hasGraphicalEditor = computed(() => {
            const languagePlugin = this.languagePlugin.value;
            return languagePlugin?.graphicalEditorPlugin != undefined;
        });
        this.hasTextualEditor = computed(() => {
            const languagePlugin = this.languagePlugin.value;
            return languagePlugin?.textualEditorPlugin != undefined;
        });
        this.hasMarkdownViewer = computed(() => {
            const parsed = this.parsedUri.value;
            return parsed.category === FileCategory.ExecutionSummary;
        });

        watch(
            this.languagePlugin,
            async (newPlugin) => {
                if (this.textualEditorState != undefined) {
                    if (newPlugin?.textualEditorPlugin == undefined) {
                        this.textualEditorState.dispose();
                        this.textualEditorState = undefined;
                    } else {
                        await this.textualEditorState.updateUri(tab.fileUri, newPlugin.id);
                        this.updateModelIfActive();
                    }
                } else {
                    if (newPlugin?.textualEditorPlugin != undefined) {
                        this.textualEditorState = new TextualEditorState(tab.fileUri);
                        await this.textualEditorState.updateUri(tab.fileUri, newPlugin.id);
                        this.updateModelIfActive();
                    }
                }
            },
            { immediate: true }
        );
    }

    /**
     * Activates the editor state, restoring the editor model and view state if applicable.
     */
    activate(): void {
        this.isActive = true;
        if (this.hasTextualEditor.value) {
            this.updateModelIfActive();
        }
    }

    /**
     * Requests that the given range be selected and revealed the next time the Monaco
     * model is applied to the editor.  If the model is already active and loaded this
     * will take effect immediately via the next {@link updateModelIfActive} call
     * triggered by {@link activate}.
     *
     * @param selection The range to select and reveal (1-based Monaco coordinates).
     */
    setPendingSelection(selection: IRange): void {
        this.pendingSelection = selection;
    }

    /**
     * Deactivates the editor state, saving the current view state if applicable.
     */
    deactivate(): void {
        this.isActive = false;
        if (this.textualEditorState != undefined && this.editor.value != undefined) {
            this.textualEditorState.viewState = this.editor.value.saveViewState() || undefined;
        }
    }

    /**
     * Updates the editor's model and view state if this editor state is active.
     */
    private updateModelIfActive(): void {
        if (!this.isActive || this.textualEditorState?.modelReference == undefined || this.editor.value == undefined) {
            return;
        }
        this.editor.value.setModel(this.textualEditorState.modelReference.object.textEditorModel);
        if (this.textualEditorState.viewState != undefined) {
            this.editor.value.restoreViewState(this.textualEditorState.viewState);
        }
        if (this.pendingSelection != undefined) {
            const sel = this.pendingSelection;
            this.pendingSelection = undefined;
            this.editor.value.setSelection(sel);
            this.editor.value.revealRangeNearTop(sel);
        }
    }

    /**
     * Disposes of the editor state and releases associated resources.
     *
     * This method should be called when the editor state is no longer needed
     * to prevent memory leaks.
     */
    dispose(): void {
        this.textualEditorState?.dispose();
    }
}

/**
 * Manages the state of a Monaco editor instance for a specific file.
 *
 * This class encapsulates the editor's model reference and view state,
 * providing lifecycle management and URI updates for editor instances.
 * It implements IDisposable to ensure proper cleanup of resources.
 */
export class TextualEditorState implements IDisposable {
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
     * @param uri The URI of the file this editor state represents
     */
    constructor(public uri: Uri) {}

    /**
     * Updates the URI of the editor and recreates the model reference with a new language.
     *
     * This method disposes of the existing model reference and creates a new one
     * for the updated URI, setting the appropriate language ID for syntax highlighting.
     *
     * @param newUri The new URI to associate with this editor state
     * @param language The language ID to set for the new model (e.g., 'typescript', 'javascript')
     */
    async updateUri(newUri: Uri, language: string) {
        this.uri = newUri;
        this.modelReference?.dispose();
        this.modelReference = undefined;
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
