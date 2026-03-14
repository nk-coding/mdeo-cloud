import type { ValidationStatus } from "@eclipse-glsp/protocol";
import { sharedImport } from "../sharedImport.js";
import type { ModelState } from "./modelState.js";
import type { AstReflection } from "@mdeo/language-common";

const { injectable, inject } = sharedImport("inversify");
const {
    LabelEditValidator,
    ValidationStatus: { Severity },
    ModelState: ModelStateKey
} = sharedImport("@eclipse-glsp/server");

/**
 * Base class for label edit validators with useful helper methods
 */
@injectable()
export abstract class BaseLabelEditValidator extends LabelEditValidator {
    /**
     * The model state instance
     */
    @inject(ModelStateKey) protected readonly modelState!: ModelState;

    /**
     * The AST reflection utility
     */
    protected get reflection(): AstReflection {
        return this.modelState.languageServices.shared.AstReflection;
    }

    /**
     * Validates a label edit for an unknown model element.
     * This method is called when the model element being edited cannot be found in the model state.
     * This can happen when a new label is inserted into the client model and its temporary ID does not yet exist on the server side.
     *
     * @param text the new label text
     * @param modelElementId the ID of the model element being edited
     */
    abstract validateUnknown(text: string, modelElementId: string): ValidationStatus;

    /**
     * Creates an error validation status with the given message
     *
     * @param message the error message
     * @returns the validation status
     */
    protected error(message: string): ValidationStatus {
        return {
            severity: Severity.ERROR,
            message
        };
    }

    /**
     * Validates the given identifier string.
     *
     * @param identifier the identifier to validate
     * @param name the name of the identifier (for error messages)
     * @returns a validation status if the identifier is invalid, undefined otherwise
     */
    protected validateIdentifier(identifier: string, name: string): ValidationStatus | undefined {
        if (identifier.trim().length === 0) {
            return this.error(`${name} cannot be empty.`);
        } else if (identifier.includes("\n")) {
            return this.error(`${name} cannot contain line breaks.`);
        } else if (identifier.includes("\r")) {
            return this.error(`${name} cannot contain carriage returns.`);
        } else if (identifier.includes("`")) {
            return this.error(`${name} cannot contain backticks (\`).`);
        }
        return undefined;
    }

    /**
     * Validates the raw identifier string.
     * The identifier can be a regular identifier or a backtick-quoted identifier.
     *
     * @param identifier the identifier to validate
     * @param name the name of the identifier (for error messages)
     * @returns a validation status if the identifier is invalid, undefined otherwise
     */
    protected validateRawIdentifier(identifier: string, name: string): ValidationStatus | undefined {
        if (identifier.trim().length === 0) {
            return this.error(`${name} cannot be empty.`);
        }

        const regularIdPattern = /^[\p{ID_Start}][\p{ID_Continue}]*$/u;
        const backtickIdPattern = /^`[^`\n\r]+`$/;

        if (!regularIdPattern.test(identifier) && !backtickIdPattern.test(identifier)) {
            return this.error(
                `${name} must be a valid identifier. Use backticks (\`) for identifiers with special characters.`
            );
        }

        if (identifier.includes("\n")) {
            return this.error(`${name} cannot contain line breaks.`);
        }
        if (identifier.includes("\r")) {
            return this.error(`${name} cannot contain carriage returns.`);
        }

        return undefined;
    }
}
