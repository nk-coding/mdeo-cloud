import type {
    BindingContext,
    BindingTarget,
    ContextEditValidatorRegistry,
    InstanceMultiBinding,
    OperationHandlerConstructor,
    ActionHandlerConstructor
} from "@eclipse-glsp/server";
import { sharedImport } from "../sharedImport.js";
import { ModelState } from "./modelState.js";
import { GModelIndex } from "./modelIndex.js";
import { SourceModelStorage } from "./sourceModelStorage.js";
import type { LanguageServices } from "@mdeo/language-common";
import type { interfaces } from "inversify";
import { AstReflectionKey, LanguageServicesKey } from "./langiumServices.js";
import { ModelIdProvider } from "./modelIdProvider.js";
import { ChangeBoundsOperationHandler } from "./handler/changeBoundsOperationHandler.js";
import { PartialChangeBoundsOperationHandler } from "./handler/partialChangeBoundsOperationHandler.js";
import { TriggerActionOperationHandler } from "./handler/triggerActionOperationHandler.js";
import { UpdateRoutingInformationOperationHandler } from "./handler/updateRoutingInformationOperationHandler.js";
import { MetadataManager } from "./metadataManager.js";
import { UpdateClientOperationHandler } from "./handler/updateClientHandler.js";
import { ExtendedContextEditValidatorRegistry } from "./contextEditValidationRegistry.js";
import { ModelSubmissionHandler } from "./modelSubmissionHandler.js";
import { BaseLayoutEngine } from "./layoutEngine.js";
import { LayoutOperationHandler } from "./handler/layoutOperationHandler.js";
import { RequestCreateEdgeSchemaActionHandler } from "./handler/requestCreateEdgeSchemaActionHandler.js";
import { CreateEdgeSchemaResolver } from "./createEdgeSchemaResolver.js";

const { injectable } = sharedImport("inversify");
const { DiagramModule, bindOrRebind, applyBindingTarget, CompoundOperationHandler } =
    sharedImport("@eclipse-glsp/server");

/**
 * Abstract base class for diagram modules that integrate Langium services.
 * Extends the GLSP DiagramModule with bindings for Langium language services.
 */
@injectable()
export abstract class BaseDiagramModule extends DiagramModule {
    override get diagramType() {
        return this.services.LanguageMetaData.languageId;
    }

    /**
     * Creates a new diagram module with the given language services.
     *
     * @param services The language services to bind into the GLSP container
     */
    constructor(protected readonly services: LanguageServices) {
        super();
    }

    protected override configure(
        bind: interfaces.Bind,
        unbind: interfaces.Unbind,
        isBound: interfaces.IsBound,
        rebind: interfaces.Rebind
    ): void {
        super.configure(bind, unbind, isBound, rebind);
        const context = { bind, unbind, isBound, rebind };
        bindOrRebind(context, LanguageServicesKey).toConstantValue(this.services);
        bindOrRebind(context, AstReflectionKey).toConstantValue(this.services.shared.AstReflection);
        applyBindingTarget(context, ModelIdProvider, this.bindModelIdProvider()).inSingletonScope();
        applyBindingTarget(context, MetadataManager, this.bindMetadataManager()).inSingletonScope();
        applyBindingTarget(context, BaseLayoutEngine, this.bindCustomLayoutEngine()).inSingletonScope();
        applyBindingTarget(context, CreateEdgeSchemaResolver, this.bindCreateEdgeSchemaResolver()).inSingletonScope();
        this.configureAdditional(context);
    }

    protected override configureActionHandlers(binding: InstanceMultiBinding<ActionHandlerConstructor>): void {
        super.configureActionHandlers(binding);
        binding.add(RequestCreateEdgeSchemaActionHandler);
    }

    /**
     * Configures additional bindings in the GLSP container.
     *
     * @param _context The binding context
     */
    protected configureAdditional(_context: BindingContext): void {}

    protected override bindModelState(): BindingTarget<ModelState> {
        return ModelState;
    }

    protected override bindGModelIndex(): BindingTarget<GModelIndex> {
        return GModelIndex;
    }

    protected override bindSourceModelStorage(): BindingTarget<SourceModelStorage> {
        return SourceModelStorage;
    }

    protected override bindContextEditValidatorRegistry(): BindingTarget<ContextEditValidatorRegistry> {
        return ExtendedContextEditValidatorRegistry;
    }

    protected override bindModelSubmissionHandler(): BindingTarget<ModelSubmissionHandler> {
        return ModelSubmissionHandler;
    }

    protected override configureOperationHandlers(binding: InstanceMultiBinding<OperationHandlerConstructor>): void {
        binding.add(CompoundOperationHandler);
        binding.add(UpdateClientOperationHandler);
        binding.add(ChangeBoundsOperationHandler);
        binding.add(PartialChangeBoundsOperationHandler);
        binding.add(TriggerActionOperationHandler);
        binding.add(UpdateRoutingInformationOperationHandler);
        binding.add(LayoutOperationHandler);
    }

    /**
     * Binds the model ID provider for generating unique IDs in the diagram.
     *
     * @returns The binding target for the ModelIdProvider
     */
    protected abstract bindModelIdProvider(): BindingTarget<ModelIdProvider>;

    /**
     * Binds the metadata manager for handling graph metadata.
     *
     * @returns The binding target for the MetadataManager
     */
    protected abstract bindMetadataManager(): BindingTarget<MetadataManager>;

    /**
     * Binds the layout engine for performing diagram layouting.
     *
     * @returns The binding target for the BaseLayoutEngine
     */
    protected abstract bindCustomLayoutEngine(): BindingTarget<BaseLayoutEngine>;

    /**
     * Binds the backend create-edge schema resolver.
     */
    protected abstract bindCreateEdgeSchemaResolver(): BindingTarget<CreateEdgeSchemaResolver>;
}
