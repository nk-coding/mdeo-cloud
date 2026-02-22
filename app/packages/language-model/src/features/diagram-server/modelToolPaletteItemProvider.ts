import type { Args, PaletteItem } from "@eclipse-glsp/protocol";
import type { ModelState } from "@mdeo/language-shared";
import { BaseToolPaletteItemProvider, sharedImport } from "@mdeo/language-shared";
import type { PartialModel } from "../../grammar/modelPartialTypes.js";
import type { TriggerActionOperation } from "@mdeo/editor-protocol";

const { injectable, inject } = sharedImport("inversify");
const { ModelState: ModelStateKey } = sharedImport("@eclipse-glsp/server");

/**
 * Tool palette item provider for the model diagram.
 * Extends BaseToolPaletteItemProvider to inherit automatic handling of
 * CreateOperationHandlers and ToolboxItemProviders.
 *
 * The palette is organized into sections:
 * - Create: Automatically populated from CreateOperationHandlers
 *   (one item per non-abstract class from the metamodel)
 * - Setup: Shown when import is missing, allows user to set up the model file
 */
@injectable()
export class ModelToolPaletteItemProvider extends BaseToolPaletteItemProvider {
    @inject(ModelStateKey)
    protected modelState!: ModelState;

    /**
     * Counter to generate unique IDs for palette items.
     */
    private counter = 0;

    /**
     * Constructs the list of palette items for the model diagram.
     * Creates a top-level "Create" group containing object creation items.
     * If the import is missing, shows a "Setup" section instead.
     *
     * @param _args Optional arguments (unused)
     * @returns Array of palette items organized by section
     */
    override async getItems(_args?: Args): Promise<PaletteItem[]> {
        this.counter = 0;

        const sourceModel = this.modelState.sourceModel as PartialModel;
        const hasImport = sourceModel?.import != undefined;

        if (!hasImport) {
            return [
                {
                    id: "setup-group",
                    label: "Setup",
                    actions: [],
                    children: this.createSetupPaletteItems(),
                    icon: "settings",
                    sortString: "A"
                }
            ];
        }

        const items = await this.getGroupedItems();
        const createItems = items.get("create-group");

        if (createItems == undefined || createItems.length === 0) {
            return [];
        }

        return [
            {
                id: "create-group",
                label: "Create",
                actions: [],
                children: createItems,
                icon: "add",
                sortString: "A"
            }
        ];
    }
    /**
     * Creates palette items for setup operations.
     * Includes "Setup Model" item that triggers the new-file action.
     *
     * @returns Array of palette items for setup operations
     */
    protected createSetupPaletteItems(): PaletteItem[] {
        const languageId = this.modelState.languageServices.LanguageMetaData.languageId;

        const setupOperation: TriggerActionOperation = {
            kind: "triggerAction",
            actionType: "new-file",
            languageId,
            data: {
                uri: this.modelState.sourceUri ?? ""
            },
            isOperation: true
        };

        this.counter++;
        const setupItem: PaletteItem = {
            id: `palette-item-${this.counter}`,
            sortString: "A",
            label: "Setup Model",
            icon: "settings",
            actions: [setupOperation]
        };

        return [setupItem];
    }
}
