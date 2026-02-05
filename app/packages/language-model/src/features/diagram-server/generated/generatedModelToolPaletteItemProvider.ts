import type { Args, PaletteItem } from "@eclipse-glsp/protocol";
import { BaseToolPaletteItemProvider, sharedImport } from "@mdeo/language-shared";

const { injectable } = sharedImport("inversify");

/**
 * Tool palette item provider for generated model diagrams.
 * Returns an empty palette since generated models are read-only.
 */
@injectable()
export class GeneratedModelToolPaletteItemProvider extends BaseToolPaletteItemProvider {
    /**
     * Returns an empty list of palette items since this is a read-only diagram.
     *
     * @param _args Optional arguments (unused)
     * @returns Empty array
     */
    override async getItems(_args?: Args): Promise<PaletteItem[]> {
        return [];
    }
}
