import type { ValidationAcceptor, ValidationChecks } from "langium";
import type { ExtendedLangiumServices } from "@mdeo/language-common";
import { sharedImport, resolveRelativePath } from "@mdeo/language-shared";
import { type ScriptType } from "../grammar/scriptTypes.js";

const { AstUtils } = sharedImport("langium");

/**
 * Interface mapping for Script AST types used in validation checks.
 */
interface ScriptAstTypes {
    Script: ScriptType;
}

/**
 * Registers validation checks for the Script language.
 *
 * @param services The extended Langium services
 */
export function registerScriptValidationChecks(services: ExtendedLangiumServices): void {
    const registry = services.validation.ValidationRegistry;
    const validator = new ScriptValidator(services);

    const checks: ValidationChecks<ScriptAstTypes> = {
        Script: validator.validateScript.bind(validator)
    };

    registry.register(checks, validator);
}

/**
 * Validator for Script language constructs.
 */
export class ScriptValidator {
    private readonly services: ExtendedLangiumServices;

    /**
     * Constructs a new ScriptValidator.
     *
     * @param services The extended Langium services
     */
    constructor(services: ExtendedLangiumServices) {
        this.services = services;
    }

    /**
     * Validates the root Script node.
     * Checks that if a `using` metamodel import is declared, the referenced file
     * actually exists and is loaded as a document.
     *
     * @param script The Script AST root node to validate
     * @param accept The validation acceptor for reporting diagnostics
     */
    validateScript(script: ScriptType, accept: ValidationAcceptor): void {
        this.validateMetamodelImport(script, accept);
    }

    /**
     * Checks that the metamodel path in a `using` declaration resolves to an existing document.
     *
     * @param script The script node containing the metamodel import
     * @param accept The validation acceptor
     */
    private validateMetamodelImport(script: ScriptType, accept: ValidationAcceptor): void {
        const metamodelImport = script.metamodelImport;
        if (metamodelImport == undefined) {
            return;
        }

        const file = metamodelImport.file;
        if (file == undefined || file.trim() === "") {
            return;
        }

        const document = AstUtils.getDocument(script);
        const targetUri = resolveRelativePath(document, file);
        const targetDoc = this.services.shared.workspace.LangiumDocuments.getDocument(targetUri);

        if (targetDoc == undefined) {
            accept("error", `Cannot resolve metamodel path '${file}'. The file does not exist or is not loaded.`, {
                node: metamodelImport,
                property: "file"
            });
        }
    }
}
