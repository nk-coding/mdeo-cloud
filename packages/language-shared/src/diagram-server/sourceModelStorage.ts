import type {
    ActionDispatcher,
    SourceModelStorage as BaseSourceModelStorage,
    RequestModelAction
} from "@eclipse-glsp/server";
import { sharedImport } from "../sharedImport.js";
import type { ModelState } from "./modelState.js";
import { LanguageServicesKey } from "./langiumServices.js";
import type { LanguageServices } from "@mdeo/language-common";
import type { GraphMetadata } from "./metadata.js";
import { UpdateClientOperation } from "./handler/updateClientHandler.js";

const { injectable, inject } = sharedImport("inversify");
const {
    SOURCE_URI_ARG,
    ModelState: ModelStateKey,
    ActionDispatcher: ActionDispatcherKey
} = sharedImport("@eclipse-glsp/server");
const { URI, DocumentState } = sharedImport("langium");

/**
 * Storage implementation for loading and saving Langium source models in GLSP.
 * Handles the conversion between URIs and Langium documents.
 */
@injectable()
export class SourceModelStorage implements BaseSourceModelStorage {
    @inject(LanguageServicesKey) languageServices!: LanguageServices;
    @inject(ModelStateKey) protected readonly modelState!: ModelState;
    @inject(ActionDispatcherKey) protected actionDispatcher!: ActionDispatcher;

    /**
     * Tracks whether the update listener has been initialized.
     */
    private updateListenerInitialized = false;

    async loadSourceModel(action: RequestModelAction): Promise<void> {
        const sourceUri = action.options?.[SOURCE_URI_ARG];
        if (typeof sourceUri !== "string") {
            throw new Error("Source URI is missing in the request action.");
        }
        this.initializeUpdateListener();
        const uri = URI.parse(sourceUri);
        const langiumDocument = await this.languageServices.shared.workspace.LangiumDocumentFactory.fromUri(uri);
        const root = langiumDocument.parseResult.value;
        const metadata = (await this.languageServices.shared.workspace.FileSystemProvider.readMetadata(
            uri
        )) as Partial<GraphMetadata>;

        await this.modelState.updateSourceModel(uri, root, {
            nodes: typeof metadata.nodes === "object" ? metadata.nodes : {},
            edges: typeof metadata.edges === "object" ? metadata.edges : {}
        });
    }

    /**
     * Initializes the update listener to track document changes.
     * This is done only once to avoid multiple registrations.
     */
    protected initializeUpdateListener() {
        if (this.updateListenerInitialized) {
            return;
        }
        this.updateListenerInitialized = true;
        this.languageServices.shared.workspace.DocumentBuilder.onBuildPhase(DocumentState.Validated, async (built) => {
            for (const doc of built) {
                if (this.modelState.sourceUri != doc.uri.toString()) {
                    continue;
                }
                const root = doc.parseResult.value;
                this.modelState.sourceModel = root;
                await this.actionDispatcher.dispatch(UpdateClientOperation.create());
            }
        });
    }

    saveSourceModel(): void {
        // No-op: saving is handled via text edits in the operation handler
    }
}
