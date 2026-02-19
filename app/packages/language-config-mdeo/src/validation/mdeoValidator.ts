import type { ValidationAcceptor, ValidationChecks, AstNode, Properties } from "langium";
import type { ExtendedLangiumServices } from "@mdeo/language-common";
import { sharedImport, resolveRelativePath } from "@mdeo/language-shared";
import {
    type SearchSectionType,
    type SolverSectionType,
    type MutationsBlockType,
    type UsingPathType,
    type MutationBlockType,
    type ArchiveBlockType
} from "../grammar/mdeoTypes.js";
import { ModelTransformation } from "@mdeo/language-model-transformation";
import type { Range } from "vscode-languageserver-types";

const { AstUtils, CstUtils, isLeafCstNode, GrammarUtils } = sharedImport("langium");

/**
 * Valid provider values for the solver section.
 */
const VALID_PROVIDERS = new Set(["moea", "ecj"]);

/**
 * Valid algorithm names for the solver section.
 */
const VALID_ALGORITHMS = new Set(["NSGAII", "IBEA", "SPEA2", "SMSMOEA", "VEGA", "PESA2", "PAES", "RANDOM"]);

/**
 * Valid variation values for mutation configuration.
 */
const VALID_VARIATIONS = new Set(["mutation", "genetic", "probabilistic"]);

/**
 * Valid mutation strategy values.
 */
const VALID_MUTATION_STRATEGIES = new Set(["random", "repetitive"]);

/**
 * Valid mutation selection values.
 */
const VALID_MUTATION_SELECTIONS = new Set(["random"]);

/**
 * Parameters that are required for specific algorithms.
 */
const PESA2_PAES_ALGORITHMS = new Set(["PESA2", "PAES"]);

/**
 * Interface mapping for MDEO AST types used in validation checks.
 */
interface MdeoAstTypes {
    ConfigMdeoSearchSection: SearchSectionType;
    ConfigMdeoSolverSection: SolverSectionType;
}

/**
 * Registers validation checks for the config-mdeo language.
 *
 * @param services The extended Langium services
 */
export function registerMdeoValidationChecks(services: ExtendedLangiumServices): void {
    const registry = services.validation.ValidationRegistry;
    const validator = new MdeoValidator(services);

    const checks: ValidationChecks<MdeoAstTypes> = {
        ConfigMdeoSearchSection: validator.validateSearchSection.bind(validator),
        ConfigMdeoSolverSection: validator.validateSolverSection.bind(validator)
    };

    registry.register(checks, validator);
}

/**
 * Validator for config-mdeo language constructs.
 * Checks path validity, parameter values, and required fields.
 */
export class MdeoValidator {
    private readonly services: ExtendedLangiumServices;

    /**
     * Constructs a new MdeoValidator.
     *
     * @param services The extended Langium services
     */
    constructor(services: ExtendedLangiumServices) {
        this.services = services;
    }

    /**
     * Validates a search section.
     * Checks that mutations block exists and using paths resolve to valid transformation files.
     *
     * @param search The search section node to validate
     * @param accept The validation acceptor for reporting diagnostics
     */
    validateSearchSection(search: SearchSectionType, accept: ValidationAcceptor): void {
        this.checkNoDuplicateNodes(search.mutations, "mutations", accept);
        const mutations = search.mutations[0];
        if (mutations != undefined) {
            this.validateMutationsBlock(mutations, accept);
        }
    }

    /**
     * Validates a mutations block.
     * Checks that using paths reference valid model transformation files.
     *
     * @param mutations The mutations block to validate
     * @param accept The validation acceptor
     */
    private validateMutationsBlock(mutations: MutationsBlockType, accept: ValidationAcceptor): void {
        for (const usingPath of mutations.usingPaths ?? []) {
            this.validateUsingPath(usingPath, accept);
        }

        if (
            (mutations.classMutations?.length ?? 0) === 0 &&
            (mutations.edgeMutations?.length ?? 0) === 0 &&
            (mutations.usingPaths?.length ?? 0) === 0
        ) {
            accept("warning", "Mutations block has no mutation operators defined.", {
                node: mutations
            });
        }
    }

    /**
     * Validates a using path.
     * Checks that the path resolves to a valid model transformation file.
     *
     * @param usingPath The using path to validate
     * @param accept The validation acceptor
     */
    private validateUsingPath(usingPath: UsingPathType, accept: ValidationAcceptor): void {
        const document = AstUtils.getDocument(usingPath);
        const targetUri = resolveRelativePath(document, usingPath.path);
        const documents = this.services.shared.workspace.LangiumDocuments;
        const targetDoc = documents.getDocument(targetUri);

        if (targetDoc == undefined) {
            accept("error", `Cannot resolve path '${usingPath.path}'. The file does not exist or is not loaded.`, {
                node: usingPath,
                property: "path"
            });
            return;
        }

        const root = targetDoc.parseResult?.value;
        if (root == undefined || root.$type !== ModelTransformation.name) {
            accept("error", `The path '${usingPath.path}' does not point to a model transformation file.`, {
                node: usingPath,
                property: "path"
            });
        }
    }

    /**
     * Validates a solver section.
     * Checks provider, algorithm, parameters block, and termination block.
     *
     * @param solver The solver section node to validate
     * @param accept The validation acceptor for reporting diagnostics
     */
    validateSolverSection(solver: SolverSectionType, accept: ValidationAcceptor): void {
        this.checkNoDuplicateKeys(solver, "provider", accept);
        this.checkNoDuplicateKeys(solver, "algorithm", accept);
        this.checkNoDuplicateNodes(solver.parameters, "parameters", accept);
        this.checkNoDuplicateNodes(solver.termination, "termination", accept);
        this.checkNoDuplicateKeys(solver, "batches", accept);
        this.validateProvider(solver, accept);
        this.validateAlgorithm(solver, accept);
        this.validateParameters(solver, accept);
        this.validateTermination(solver, accept);
    }

    /**
     * Validates the provider value.
     *
     * @param solver The solver section
     * @param accept The validation acceptor
     */
    private validateProvider(solver: SolverSectionType, accept: ValidationAcceptor): void {
        const provider = solver.provider[0];
        if (provider != undefined && !VALID_PROVIDERS.has(provider)) {
            accept(
                "error",
                `Invalid provider '${provider}'. Must be one of: ${Array.from(VALID_PROVIDERS).join(", ")}.`,
                { node: solver, property: "provider", index: 0 }
            );
        }
    }

    /**
     * Validates the algorithm value.
     *
     * @param solver The solver section
     * @param accept The validation acceptor
     */
    private validateAlgorithm(solver: SolverSectionType, accept: ValidationAcceptor): void {
        const algorithm = solver.algorithm[0];
        if (algorithm == undefined) {
            accept("error", "An algorithm must be specified in the solver section.", {
                node: solver,
                range: this.findSectionRange(solver)
            });
            return;
        }
        if (!VALID_ALGORITHMS.has(algorithm)) {
            accept(
                "error",
                `Invalid algorithm '${algorithm}'. Must be one of: ${Array.from(VALID_ALGORITHMS).join(", ")}.`,
                { node: solver, property: "algorithm", index: 0 }
            );
        }
    }

    /**
     * Validates the parameters block.
     * Checks required parameters (population, variation) and algorithm-specific ones
     * (bisections, archive.size for PESA2/PAES).
     *
     * @param solver The solver section
     * @param accept The validation acceptor
     */
    private validateParameters(solver: SolverSectionType, accept: ValidationAcceptor): void {
        const params = solver.parameters[0];
        if (params == undefined) {
            accept("error", "A parameters block is required in the solver section.", {
                node: solver,
                range: this.findSectionRange(solver)
            });
            return;
        }

        this.checkNoDuplicateKeys(params, "population", accept);
        this.checkNoDuplicateKeys(params, "variation", accept);
        this.checkNoDuplicateNodes(params.mutation, "mutation", accept);
        this.checkNoDuplicateKeys(params, "bisections", accept);
        this.checkNoDuplicateNodes(params.archive, "archive", accept);

        const population = params.population[0];
        if (population == undefined) {
            accept("error", "Required parameter 'population' must be specified in the parameters block.", {
                node: params,
                keyword: "parameters"
            });
        }

        const variation = params.variation[0];
        if (variation == undefined) {
            accept("error", "Required parameter 'variation' must be specified in the parameters block.", {
                node: params,
                keyword: "parameters"
            });
        } else if (!VALID_VARIATIONS.has(variation)) {
            accept(
                "error",
                `Invalid variation '${variation}'. Must be one of: ${Array.from(VALID_VARIATIONS).join(", ")}.`,
                { node: params, property: "variation", index: 0 }
            );
        }

        const mutation = params.mutation[0];
        if (mutation != undefined) {
            this.validateMutationBlock(mutation, accept);
        }

        const algorithm = solver.algorithm[0];
        if (algorithm != undefined && PESA2_PAES_ALGORITHMS.has(algorithm)) {
            const bisections = params.bisections[0];
            if (bisections == undefined) {
                accept("error", `Required parameter 'bisections' must be specified for algorithm '${algorithm}'.`, {
                    node: params,
                    keyword: "parameters"
                });
            }
            const archive = params.archive[0];
            if (archive == undefined) {
                accept("error", `An archive block with 'size' is required for algorithm '${algorithm}'.`, {
                    node: params,
                    keyword: "parameters"
                });
            } else {
                this.validateArchiveBlock(archive, algorithm, accept);
            }
        }
    }

    /**
     * Validates the archive sub-block.
     *
     * @param archive The archive block to validate
     * @param algorithm The current algorithm name (for error messages)
     * @param accept The validation acceptor
     */
    private validateArchiveBlock(archive: ArchiveBlockType, algorithm: string, accept: ValidationAcceptor): void {
        this.checkNoDuplicateKeys(archive, "size", accept);
        const size = archive.size[0];
        if (size == undefined) {
            accept("error", `Required parameter 'archive.size' must be specified for algorithm '${algorithm}'.`, {
                node: archive,
                keyword: "archive"
            });
        }
    }

    /**
     * Validates the mutation sub-block.
     * Checks strategy and selection values against allowed identifiers.
     *
     * @param mutation The mutation block to validate
     * @param accept The validation acceptor
     */
    private validateMutationBlock(mutation: MutationBlockType, accept: ValidationAcceptor): void {
        this.checkNoDuplicateKeys(mutation, "step", accept);
        this.checkNoDuplicateKeys(mutation, "strategy", accept);
        this.checkNoDuplicateKeys(mutation, "selection", accept);
        this.checkNoDuplicateKeys(mutation, "application", accept);
        this.checkNoDuplicateKeys(mutation, "credit", accept);
        this.checkNoDuplicateKeys(mutation, "repair", accept);

        const strategy = mutation.strategy[0];
        if (strategy != undefined && !VALID_MUTATION_STRATEGIES.has(strategy)) {
            accept(
                "error",
                `Invalid mutation strategy '${strategy}'. Must be one of: ${Array.from(VALID_MUTATION_STRATEGIES).join(", ")}.`,
                { node: mutation, property: "strategy", index: 0 }
            );
        }
        const selection = mutation.selection[0];
        if (selection != undefined && !VALID_MUTATION_SELECTIONS.has(selection)) {
            accept(
                "error",
                `Invalid mutation selection '${selection}'. Must be one of: ${Array.from(VALID_MUTATION_SELECTIONS).join(", ")}.`,
                { node: mutation, property: "selection", index: 0 }
            );
        }
    }

    /**
     * Validates the termination block.
     * Checks that at least one termination condition is present.
     *
     * @param solver The solver section
     * @param accept The validation acceptor
     */
    private validateTermination(solver: SolverSectionType, accept: ValidationAcceptor): void {
        const termination = solver.termination[0];
        if (termination == undefined) {
            accept("error", "A termination block is required in the solver section.", {
                node: solver,
                range: this.findSectionRange(solver)
            });
            return;
        }

        this.checkNoDuplicateKeys(termination, "evolutions", accept);
        this.checkNoDuplicateKeys(termination, "time", accept);
        this.checkNoDuplicateKeys(termination, "delta", accept);
        this.checkNoDuplicateKeys(termination, "iterations", accept);

        const hasCondition =
            termination.evolutions.length > 0 || termination.time.length > 0 || termination.delta.length > 0;

        if (!hasCondition) {
            accept("error", "Termination block must contain at least one condition: evolutions, time, or delta.", {
                node: termination,
                keyword: "termination"
            });
        }
    }

    /**
     * Reports an error for every duplicate entry after the first in a scalar-array field
     * whose elements are primitive values (strings, numbers).
     * Index 0 is accepted; indices >= 1 are reported as duplicates.
     *
     * @param node The parent AST node
     * @param property The name of the array property holding primitive values
     * @param accept The validation acceptor
     */
    private checkNoDuplicateKeys<T extends AstNode>(
        node: T,
        property: Properties<T> & keyof T,
        accept: ValidationAcceptor
    ): void {
        const arr = node[property];
        if (!Array.isArray(arr)) {
            return;
        }
        for (let i = 1; i < arr.length; i++) {
            const cstNode = GrammarUtils.findNodeForKeyword(node.$cstNode, property, i);
            accept("error", `Duplicate '${property}' key.`, { node, range: cstNode?.range });
        }
    }

    /**
     * Reports an error for every duplicate entry after the first in an array field
     * whose elements are AST nodes. Reports the error directly at the duplicate node.
     *
     * @param arr The array of AST nodes
     * @param label The field label used in the error message
     * @param accept The validation acceptor
     */
    private checkNoDuplicateNodes(arr: AstNode[], label: string, accept: ValidationAcceptor): void {
        for (let i = 1; i < arr.length; i++) {
            accept("error", `Duplicate '${label}' block.`, { node: arr[i] });
        }
    }

    /**
     * Finds the range of the section keyword for error reporting
     *
     * @param section the section node to find the keyword range for
     * @returns the range of the section keyword, or undefined if it cannot be determined
     */
    private findSectionRange(section: AstNode): Range | undefined {
        const cstNode = section.$container?.$cstNode;
        if (cstNode == undefined) {
            return undefined;
        }
        const keywordNode = CstUtils.streamCst(cstNode).find((node) => isLeafCstNode(node) && !node.hidden);
        if (keywordNode == undefined) {
            return undefined;
        }
        return keywordNode.range;
    }
}
