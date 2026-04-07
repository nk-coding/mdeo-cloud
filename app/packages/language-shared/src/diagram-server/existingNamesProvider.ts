import { sharedImport } from "../sharedImport.js";
import type { ModelState } from "./modelState.js";

const { injectable, inject } = sharedImport("inversify");
const { ModelState: ModelStateKey } = sharedImport("@eclipse-glsp/server");

/**
 * Injection key and interface for providing the set of already-used names in the
 * target document.
 *
 * <p>The default implementation ({@link DefaultExistingNamesProvider}) collects
 * names via {@code ScopeComputation.collectExportedSymbols}. Language-specific
 * diagram modules can rebind this to include additional names (e.g. names from
 * imported files).
 */
export const ExistingNamesProvider = Symbol("ExistingNamesProvider");

/**
 * Interface for providing the set of names already present in the target document.
 * Used to detect naming conflicts when creating or pasting nodes.
 */
export interface ExistingNamesProvider {
    /**
     * Collects all names already present in the target document.
     *
     * @returns A set of existing names in the target document.
     */
    getExistingNames(): Promise<Set<string>>;
}

/**
 * Default implementation of {@link ExistingNamesProvider}.
 *
 * <p>Collects names by querying {@code ScopeComputation.collectExportedSymbols}
 * on the current source document.
 */
@injectable()
export class DefaultExistingNamesProvider implements ExistingNamesProvider {
    @inject(ModelStateKey)
    protected readonly modelState!: ModelState;

    async getExistingNames(): Promise<Set<string>> {
        const document = this.modelState.sourceModel?.$document;
        if (!document) {
            return new Set();
        }
        const exported =
            await this.modelState.languageServices.references.ScopeComputation.collectExportedSymbols(document);
        return new Set(exported.map((description) => description.name));
    }
}
