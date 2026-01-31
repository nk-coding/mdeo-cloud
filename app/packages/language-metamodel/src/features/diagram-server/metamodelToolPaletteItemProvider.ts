import type { Args, PaletteItem } from "@eclipse-glsp/protocol";
import type { TriggerActionOperation } from "@mdeo/editor-protocol";
import type { ModelState } from "@mdeo/language-shared";
import { BaseToolPaletteItemProvider, sharedImport } from "@mdeo/language-shared";

const { injectable, inject } = sharedImport("inversify");
const { ModelState: ModelStateKey } = sharedImport("@eclipse-glsp/server");

/**
 * Custom tool palette item provider for the metamodel diagram.
 * Extends BaseToolPaletteItemProvider to inherit automatic handling of CreateOperationHandlers
 * and ToolboxItemProviders, while adding custom import functionality.
 *
 * The palette is organized into sections:
 * - Create: Automatically populated from CreateOperationHandlers (Class and Enum nodes)
 * - Import: Custom import actions for importing classes and enums from other files
 */
@injectable()
export class MetamodelToolPaletteItemProvider extends BaseToolPaletteItemProvider {
    @inject(ModelStateKey)
    protected modelState!: ModelState;

    /**
     * Counter to generate unique IDs for palette items.
     */
    private counter = 0;

    /**
     * Constructs the list of palette items for the metamodel diagram.
     * Creates two top-level groups: Create and Import.
     *
     * @param _args Optional arguments (unused)
     * @returns Array of palette items organized by section
     */
    override async getItems(_args?: Args): Promise<PaletteItem[]> {
        this.counter = 0;

        const items = await this.getGroupedItems();
        const importItems = this.createImportPaletteItems();

        return [
            {
                id: "create-group",
                label: "Create",
                actions: [],
                children: items.get("create-group"),
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
     * Creates the Import palette group with import actions.
     *
     * @returns A palette item representing the Import group
     */
    protected createImportGroup(): PaletteItem {
        const importItems = this.createImportPaletteItems();

        return {
            id: "import-group",
            label: "Import",
            actions: [],
            children: importItems,
            icon: "cloud-download",
            sortString: "B"
        };
    }

    /**
     * Creates palette items for import operations.
     * Includes a single "Import File" item that triggers the file import action.
     *
     * @returns Array of palette items for import operations
     */
    protected createImportPaletteItems(): PaletteItem[] {
        const languageId = this.modelState.languageServices.LanguageMetaData.languageId;

        const importFileOperation: TriggerActionOperation = {
            kind: "triggerAction",
            actionType: "import-file",
            languageId,
            data: {
                uri: this.modelState.sourceUri ?? ""
            },
            isOperation: true
        };

        this.counter++;
        const importFileItem: PaletteItem = {
            id: `palette-item-${this.counter}`,
            sortString: "A",
            label: "Import File",
            icon: "cloud-download",
            actions: [importFileOperation]
        };

        return [importFileItem];
    }
}
