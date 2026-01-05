import type { BindingTarget } from "@eclipse-glsp/server";
import { sharedImport } from "../sharedImport.js";
import { ModelState } from "./modelState.js";
import { GModelIndex } from "./modelIndex.js";
import { SourceModelStorage } from "./sourceModelStorage.js";
import type { LanguageServices } from "@mdeo/language-common";
import type { interfaces } from "inversify";
import { LanguageServicesKey } from "./langiumServices.js";

const { injectable } = sharedImport("inversify");
const { DiagramModule, bindOrRebind } = sharedImport("@eclipse-glsp/server");

/**
 * Abstract base class for diagram modules that integrate Langium services.
 * Extends the GLSP DiagramModule with bindings for Langium language services.
 */
@injectable()
export abstract class BaseDiagramModule extends DiagramModule {

    /**
     * Creates a new diagram module with the given language services.
     *
     * @param services The language services to bind into the GLSP container
     */
    constructor(private readonly services: LanguageServices) {
        super()
    }

    protected override configure(bind: interfaces.Bind, unbind: interfaces.Unbind, isBound: interfaces.IsBound, rebind: interfaces.Rebind): void {
        super.configure(bind, unbind, isBound, rebind);
        const context = { bind, unbind, isBound, rebind };
        bindOrRebind(context, LanguageServicesKey).toConstantValue(this.services);
    }

    protected override bindModelState(): BindingTarget<ModelState> {
        return ModelState;
    }

    protected override bindGModelIndex(): BindingTarget<GModelIndex> {
        return GModelIndex;
    }

    protected override bindSourceModelStorage(): BindingTarget<SourceModelStorage> {
        return SourceModelStorage;
    }
}
