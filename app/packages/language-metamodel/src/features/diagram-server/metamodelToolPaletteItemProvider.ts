import type { Args, MaybePromise, PaletteItem } from "@eclipse-glsp/protocol";
import type { CreateOperationHandler, OperationHandlerRegistry, ToolPaletteItemProvider } from "@eclipse-glsp/server";
import type { TriggerNodeCreationAction as TriggerNodeCreationActionType } from "@eclipse-glsp/protocol";
import type { TriggerActionOperation } from "@mdeo/editor-protocol";
import type { ModelState } from "@mdeo/language-shared";
import { sharedImport } from "@mdeo/language-shared";

const { injectable, inject } = sharedImport("inversify");
const { CreateNodeOperation: CreateNodeOperationKind } = sharedImport("@eclipse-glsp/protocol");
const { OperationHandlerRegistry: OperationHandlerRegistrySymbol, ModelState: ModelStateKey } =
    sharedImport("@eclipse-glsp/server");

/**
 * Custom tool palette item provider for the metamodel diagram.
 * Provides palette items organized into two sections:
 * - Create: Contains class creation items (with and without property)
 * - Import: Contains import class and import enum actions that trigger the workbench
 */
@injectable()
export class MetamodelToolPaletteItemProvider implements ToolPaletteItemProvider {
    @inject(OperationHandlerRegistrySymbol)
    protected operationHandlerRegistry!: OperationHandlerRegistry;

    @inject(ModelStateKey)
    protected modelState!: ModelState;

    /**
     * Counter for generating unique palette item IDs.
     */
    protected counter: number = 0;

    /**
     * Returns the context ID for this provider.
     *
     * @returns The tool palette context identifier
     */
    get contextId(): string {
        return "tool-palette";
    }

    /**
     * Returns a list of labeled actions for the given editor context.
     *
     * @param _editorContext The editor context (unused)
     * @returns A promise resolving to the palette items
     */
    async getActions(_editorContext: unknown): Promise<PaletteItem[]> {
        return this.getItems();
    }

    /**
     * Constructs the list of palette items for the metamodel diagram.
     * Creates two top-level groups: Create and Import.
     *
     * @param _args Optional arguments (unused)
     * @returns Array of palette items organized by section
     */
    getItems(_args?: Args): MaybePromise<PaletteItem[]> {
        this.counter = 0;

        const createItems = this.createNodePaletteItems();
        const importItems = this.createImportPaletteItems();

        return [
            {
                id: "create-group",
                label: "Create",
                actions: [],
                children: createItems,
                icon: "add",
                sortString: "A"
            },
            {
                id: "import-group",
                label: "Import",
                actions: [],
                children: importItems,
                icon: "cloud-download",
                sortString: "B"
            }
        ];
    }

    /**
     * Creates palette items for node creation operations.
     * Includes items for creating classes with and without properties.
     *
     * @returns Array of palette items for class creation
     */
    protected createNodePaletteItems(): PaletteItem[] {
        const handlers = this.operationHandlerRegistry
            .getAll()
            .filter(this.isCreateOperationHandler) as CreateOperationHandler[];

        return handlers
            .filter((handler) => handler.operationType === CreateNodeOperationKind.KIND)
            .flatMap((handler) =>
                handler
                    .getTriggerActions()
                    .map((action) => this.createPaletteItem(action, this.getLabelForAction(action, handler.label)))
            )
            .sort((a, b) => a.sortString.localeCompare(b.sortString));
    }

    /**
     * Type guard to check if a handler is a CreateOperationHandler.
     *
     * @param handler The operation handler to check
     * @returns True if the handler is a CreateOperationHandler
     */
    protected isCreateOperationHandler(handler: unknown): handler is CreateOperationHandler {
        return (
            typeof handler === "object" &&
            handler !== null &&
            "getTriggerActions" in handler &&
            typeof (handler as CreateOperationHandler).getTriggerActions === "function"
        );
    }

    /**
     * Gets a descriptive label for a trigger action.
     * Appends context-specific suffixes based on action arguments.
     *
     * @param action The trigger node creation action
     * @param baseLabel The base label from the handler
     * @returns The formatted label for the palette item
     */
    protected getLabelForAction(action: TriggerNodeCreationActionType, baseLabel: string): string {
        if (action.args?.includeProperty === true) {
            return `${baseLabel} (with Property)`;
        }
        if (action.args?.includeEntry === true) {
            return `${baseLabel} (with Entry)`;
        }
        return baseLabel;
    }

    /**
     * Creates a single palette item from a trigger action.
     *
     * @param action The trigger element creation action
     * @param label The label for the palette item
     * @returns A palette item configured with the action
     */
    protected createPaletteItem(action: TriggerNodeCreationActionType, label: string): PaletteItem {
        this.counter++;
        return {
            id: `palette-item-${this.counter}`,
            sortString: label.charAt(0),
            label,
            actions: [action]
        };
    }

    /**
     * Creates palette items for import operations.
     * Includes "Import Class" and "Import Enum" items that trigger actions on the workbench.
     *
     * @returns Array of palette items for import operations
     */
    protected createImportPaletteItems(): PaletteItem[] {
        const languageId = this.modelState.languageServices.LanguageMetaData.languageId;

        const importClassOperation: TriggerActionOperation = {
            kind: "triggerAction",
            actionType: "import-class",
            languageId,
            data: {
                uri: this.modelState.sourceUri ?? ""
            },
            isOperation: true
        };

        const importEnumOperation: TriggerActionOperation = {
            kind: "triggerAction",
            actionType: "import-enum",
            languageId,
            data: {
                uri: this.modelState.sourceUri ?? ""
            },
            isOperation: true
        };

        this.counter++;
        const importClassItem = {
            id: `palette-item-${this.counter}`,
            sortString: "A",
            label: "Import Class",
            icon: "cloud-download",
            actions: [importClassOperation]
        };

        this.counter++;
        const importEnumItem = {
            id: `palette-item-${this.counter}`,
            sortString: "B",
            label: "Import Enum",
            icon: "cloud-download",
            actions: [importEnumOperation]
        };

        return [importClassItem, importEnumItem];
    }
}
