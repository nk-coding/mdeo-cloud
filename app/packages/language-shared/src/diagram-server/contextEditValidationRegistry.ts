import type {
    ContextEditValidator,
    ContextEditValidatorRegistry,
    LabelEditValidator as LabelEditValidatorType,
    RequestEditValidationAction,
    ValidationStatus as ValidationStatusType
} from "@eclipse-glsp/server";
import { sharedImport } from "../sharedImport.js";
import type { ModelState } from "./modelState.js";

const { inject, injectable, multiInject, optional } = sharedImport("inversify");
const {
    Registry,
    LabelEditValidator,
    ModelState: ModelStateKey,
    ContextEditValidators: ContextEditValidatorsKey,
    ValidateLabelEditAdapter,
    ValidationStatus,
    GModelElement
} = sharedImport("@eclipse-glsp/server");

/**
 * Extended registry for context edit validators that uses a lenient label edit validator adapter.
 */
@injectable()
export class ExtendedContextEditValidatorRegistry
    extends Registry<string, ContextEditValidator>
    implements ContextEditValidatorRegistry
{
    constructor(
        @inject(ModelStateKey) modelState: ModelState,
        @multiInject(ContextEditValidatorsKey) @optional() contextEditValidators: ContextEditValidator[] = [],
        @inject(LabelEditValidator) @optional() labelEditValidator?: LabelEditValidatorType
    ) {
        super();
        contextEditValidators.forEach((provider) => this.register(provider.contextId, provider));
        if (labelEditValidator) {
            this.register(
                LabelEditValidator.CONTEXT_ID,
                new LenientValidateLabelEditAdapter(modelState, labelEditValidator)
            );
        }
    }
}

/**
 * A lenient version of the ValidateLabelEditAdapter that does not error
 * when the model element is not found, but instead returns an ERROR status.
 */
class LenientValidateLabelEditAdapter extends ValidateLabelEditAdapter {
    override validate(action: RequestEditValidationAction): ValidationStatusType {
        const element = this.modelState.index.find(action.modelElementId);
        if (element == undefined) {
            return {
                severity: ValidationStatus.Severity.ERROR,
                message: `Model element with ID ${action.modelElementId} not found.`
            };
        }
        if (element instanceof GModelElement) {
            return this.labelEditValidator.validate(action.text, element);
        }
        return { severity: ValidationStatus.Severity.OK };
    }
}
