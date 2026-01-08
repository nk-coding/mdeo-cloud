import type {
    SourceModelStorage as BaseSourceModelStorage,
    MaybePromise,
    RequestModelAction,
    SaveModelAction
} from "@eclipse-glsp/server";
import { sharedImport } from "../sharedImport.js";
import type { ModelState } from "./modelState.js";
import { LanguageServicesKey } from "./langiumServices.js";
import type { LanguageServices } from "@mdeo/language-common";
import type { GraphMetadata } from "./metadata.js";

const { injectable, inject } = sharedImport("inversify");
const { SOURCE_URI_ARG, ModelState: ModelStateKey } = sharedImport("@eclipse-glsp/server");
const { URI } = sharedImport("langium");

/**
 * Storage implementation for loading and saving Langium source models in GLSP.
 * Handles the conversion between URIs and Langium documents.
 */
@injectable()
export class SourceModelStorage implements BaseSourceModelStorage {
    @inject(LanguageServicesKey) languageServices!: LanguageServices;
    @inject(ModelStateKey) protected readonly modelState!: ModelState;

    async loadSourceModel(action: RequestModelAction): Promise<void> {
        const sourceUri = action.options?.[SOURCE_URI_ARG];
        if (typeof sourceUri !== "string") {
            throw new Error("Source URI is missing in the request action.");
        }
        const uri = URI.parse(sourceUri);
        const langiumDocument = await this.languageServices.shared.workspace.LangiumDocumentFactory.fromUri(uri);
        const root = langiumDocument.parseResult.value;
        const metadata = (await this.languageServices.shared.workspace.FileSystemProvider.readMetadata(
            uri
        )) as Partial<GraphMetadata>;

        await this.modelState.updateSourceModel(root, {
            nodes: typeof metadata.nodes === "object" ? metadata.nodes : {},
            edges: typeof metadata.edges === "object" ? metadata.edges : {}
        });
    }

    saveSourceModel(action: SaveModelAction): MaybePromise<void> {
        console.log("Saving source model is not implemented yet.");
    }
}
