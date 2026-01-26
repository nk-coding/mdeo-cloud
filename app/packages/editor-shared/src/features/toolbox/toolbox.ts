import type { VNode } from "snabbdom";
import { init, classModule, propsModule, styleModule, eventListenersModule, attributesModule } from "snabbdom";
import type { Action, IActionDispatcher, ICommand, GModelRoot, PaletteItem } from "@eclipse-glsp/sprotty";
import type MiniSearch from "minisearch";
import { sharedImport } from "../../sharedImport.js";
import { ToolState } from "./toolState.js";
import { ToolType, type ToolDefinition, type ToolboxEditEntry, type ToolboxErrorState } from "./toolboxTypes.js";
import { generateToolboxView } from "./views/toolboxView.js";
import { enableTool } from "./tools.js";
import { ScrollViewState } from "./views/scrollView.js";
import { PreviewRenderer } from "./previewRenderer.js";

const patcher = init([classModule, propsModule, styleModule, eventListenersModule, attributesModule]);

const { injectable, inject } = sharedImport("inversify");
const { html, TYPES } = sharedImport("@eclipse-glsp/sprotty");
const { ToolPalette, EnableDefaultToolsAction, EnableToolsAction } = sharedImport("@eclipse-glsp/client");
const MiniSearchLib = sharedImport("minisearch");

/**
 * Toolbox UI extension that provides tool selection and element palette functionality.
 * Extends the GLSP ToolPalette with Snabbdom-based rendering and enhanced search.
 */
@injectable()
export class Toolbox extends ToolPalette {
    static readonly TOOLBOX_ID = "toolbox";

    @inject(TYPES.IActionDispatcher)
    declare protected actionDispatcher: IActionDispatcher;

    @inject(ToolState)
    public toolState!: ToolState;

    @inject(PreviewRenderer)
    public previewRenderer!: PreviewRenderer;

    protected currentVNode?: VNode;
    protected searchIndex?: MiniSearch<ToolboxEditEntry>;
    protected toolboxEntries: ToolboxEditEntry[] = [];

    public detailsScrollState: ScrollViewState = new ScrollViewState();
    public errorScrollState: ScrollViewState = new ScrollViewState();

    public isOpen: boolean = true;
    public isBottomPanelOpen: boolean = true;
    public showPreviewFor?: string;
    public searchString: string = "";
    public errorState?: ToolboxErrorState = undefined;
    public selectedItemIndex: number = 0;
    public searchDebounceTimeout?: number;

    override id(): string {
        return Toolbox.ID;
    }

    override containerClass(): string {
        return "toolbox-wrapper";
    }

    /**
     * Initializes the toolbox contents with Snabbdom rendering.
     *
     * @param containerElement The container element to render into
     */
    protected override initializeContents(containerElement: HTMLElement): void {
        const toolbox = document.createElement("div");
        containerElement.appendChild(toolbox);
        containerElement.classList.add("mdeo-toolbox");
        this.currentVNode = this.patch(toolbox, html("div", null));
    }

    /**
     * Creates the header section. Overridden to prevent default DOM rendering.
     */
    protected override createHeader(): void {
        // Rendering is handled by Snabbdom
    }

    /**
     * Creates the body section. Overridden to prevent default DOM rendering.
     */
    protected override createBody(): void {
        // Rendering is handled by Snabbdom
    }

    /**
     * Adds the minimize button. Overridden to prevent default DOM rendering.
     */
    protected override addMinimizePaletteButton(): void {
        // Rendering is handled by Snabbdom
    }

    /**
     * Patches the current VNode with a new one.
     *
     * @param oldVNode The old VNode or element
     * @param newVNode The new VNode
     * @returns The patched VNode
     */
    protected patch(oldVNode: VNode | Element, newVNode: VNode): VNode {
        return patcher(oldVNode, newVNode);
    }

    /**
     * Updates the toolbox by re-rendering with Snabbdom.
     */
    update(): void {
        if (this.currentVNode) {
            this.currentVNode = this.patch(this.currentVNode, this.generateMainContent());
        }
    }

    /**
     * Generates the main content VNode for the toolbox.
     * Can be overridden to customize the rendering.
     *
     * @returns The main content VNode
     */
    protected generateMainContent(): VNode {
        return generateToolboxView(this);
    }

    /**
     * Handles actions from the action dispatcher.
     *
     * @param action The action to handle
     * @returns Command, Action, or void
     */
    override handle(action: Action): ICommand | Action | void {
        super.handle(action);
        this.update();
    }

    /**
     * Called before showing the toolbox.
     *
     * @param containerElement The container element
     * @param root The model root
     */
    protected override onBeforeShow(containerElement: HTMLElement, root: Readonly<GModelRoot>): void {
        super.onBeforeShow(containerElement, root);
        this.buildToolboxEntries();
        this.update();
    }

    /**
     * Reloads the palette body, refreshing items if dynamic and not readonly.
     */
    protected override async reloadPaletteBody(): Promise<void> {
        if (!this.editorContext.isReadonly && this.dynamic) {
            await this.setPaletteItems();
            this.buildToolboxEntries();
            this.update();
        }
    }

    /**
     * Builds toolbox entries from palette items.
     */
    protected buildToolboxEntries(): void {
        this.toolboxEntries = [];
        this.searchIndex = undefined;

        if (!this.paletteItems) {
            return;
        }

        for (const item of this.paletteItems) {
            if (item.children) {
                for (const child of item.children) {
                    this.toolboxEntries.push(this.createToolboxEntry(child, item.label));
                }
            } else {
                this.toolboxEntries.push(this.createToolboxEntry(item, ""));
            }
        }
    }

    /**
     * Creates a toolbox entry from a palette item.
     *
     * @param item The palette item
     * @param group The group name
     * @returns The toolbox entry
     */
    protected createToolboxEntry(item: PaletteItem, group: string): ToolboxEditEntry {
        return {
            id: item.id,
            name: item.label,
            group: group || "Other",
            keywords: [item.label.toLowerCase()],
            paletteItem: item
        };
    }

    /**
     * Gets the search index, building it if necessary.
     *
     * @returns The MiniSearch index
     */
    protected getSearchIndex(): MiniSearch<ToolboxEditEntry> {
        if (!this.searchIndex) {
            this.searchIndex = new MiniSearchLib.default<ToolboxEditEntry>({
                fields: ["name", "keywords"],
                storeFields: ["id", "name", "group", "keywords", "paletteItem"],
                idField: "id"
            });
            this.searchIndex.addAll(this.toolboxEntries);
        }
        return this.searchIndex;
    }

    /**
     * Gets filtered items based on the current search string.
     *
     * @returns Array of filtered toolbox entries
     */
    getFilteredItems(): ToolboxEditEntry[] {
        if (this.searchString.length === 0) {
            return this.toolboxEntries;
        }

        const searchIndex = this.getSearchIndex();
        const results = searchIndex.search(this.searchString, { prefix: true, fuzzy: 0.2 });
        const entryMap = new Map(this.toolboxEntries.map((e) => [e.id, e]));
        return results
            .map((result: { id: string }) => entryMap.get(result.id))
            .filter((entry): entry is ToolboxEditEntry => entry !== undefined);
    }

    /**
     * Toggles the toolbox open/closed state.
     */
    toggleToolbox(): void {
        this.isOpen = !this.isOpen;
        this.update();
    }

    /**
     * Toggles the bottom panel open/closed state.
     */
    toggleBottomPanel(): void {
        this.isBottomPanelOpen = !this.isBottomPanelOpen;
        this.update();
    }

    /**
     * Updates the current tool.
     *
     * @param tool The tool type to set
     */
    updateTool(tool: ToolType): void {
        if (this.toolState.toolType === tool) {
            return;
        }

        this.toolState.toolType = tool;

        const action = this.getToolAction(tool);
        if (action) {
            this.actionDispatcher.dispatch(action);
        }

        this.update();
    }

    /**
     * Gets the action to dispatch for a tool.
     *
     * @param tool The tool type
     * @returns The action to dispatch
     */
    protected getToolAction(tool: ToolType): Action | undefined {
        switch (tool) {
            case ToolType.POINTER:
                return EnableDefaultToolsAction.create();
            case ToolType.HAND:
                return EnableToolsAction.create([]);
            case ToolType.DELETE:
                return EnableToolsAction.create(["glsp.delete-mouse-tool"]);
            case ToolType.MARQUEE:
                return EnableToolsAction.create(["glsp.marquee-selection-tool"]);
            default:
                return undefined;
        }
    }

    /**
     * Handles tool button clicks.
     *
     * @param tool The tool definition that was clicked
     */
    onToolClick(tool: ToolDefinition): void {
        if (tool.id === ToolType.BOTTOM_PANEL_TOGGLE) {
            this.toggleBottomPanel();
        } else {
            enableTool(this, tool.id);
        }
    }

    /**
     * Handles palette item clicks.
     *
     * @param item The toolbox entry that was clicked
     */
    onPaletteItemClick(item: ToolboxEditEntry): void {
        if (this.editorContext.isReadonly) {
            return;
        }

        this.actionDispatcher.dispatchAll(item.paletteItem.actions);
    }

    /**
     * Handles keydown events in the toolbox.
     *
     * @param event The keyboard event
     */
    handleKeyDown(event: KeyboardEvent): void {
        if (event.key === "Escape") {
            this.searchString = "";
            this.update();
        }
    }

    /**
     * Handles keyup events in the toolbox.
     *
     * @param _event The keyboard event
     */
    handleKeyUp(_event: KeyboardEvent): void {
        // Placeholder for key state tracking
    }
}
