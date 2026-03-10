/**
 * Data for a class mutation (create/delete/mutate).
 * Informational only; not forwarded to the optimizer backend.
 */
export interface ClassMutationData {
    operator: "create" | "delete" | "mutate";
    className: string;
}

/**
 * Data for an edge mutation (add/remove/mutate).
 * Informational only; not forwarded to the optimizer backend.
 */
export interface EdgeMutationData {
    operator: "add" | "remove" | "mutate";
    className: string;
    edgeName: string;
}

/**
 * Data for the mutations block.
 * `usingPaths` is a flat list of absolute file-system paths, matching
 * Kotlin MutationsConfig.usingPaths (List<String>).
 */
export interface MutationsBlockData {
    /**
     * Absolute paths to the model transformation files.
     */
    usingPaths: string[];
    classMutations: ClassMutationData[];
    edgeMutations: EdgeMutationData[];
}

/**
 * Data extracted from the search section.
 */
export interface SearchSectionData {
    mutations?: MutationsBlockData;
}

/**
 * Mutation step configuration.
 * `type` discriminator matches Kotlin @SerialName values ("Fixed" / "Interval").
 */
export type MutationStepConfig = { type: "Fixed"; n: number } | { type: "Interval"; lower: number; upper: number };

/**
 * Mutation parameters block.
 * Maps to Kotlin MutationParameters.
 * `strategy` matches Kotlin MutationStrategy enum names (uppercase).
 */
export interface MutationBlockData {
    step?: MutationStepConfig;
    strategy?: "RANDOM" | "REPETITIVE";
}

/**
 * Algorithm parameters block.
 * Maps to Kotlin AlgorithmParameters.
 * `variation` matches Kotlin VariationType enum names (uppercase).
 * `archiveSize` matches Kotlin AlgorithmParameters.archiveSize directly.
 */
export interface AlgorithmParametersData {
    population?: number;
    variation?: "MUTATION" | "GENETIC" | "PROBABILISTIC";
    mutation?: MutationBlockData;
    bisections?: number;
    archiveSize?: number;
}

/**
 * Termination block.
 * Maps to Kotlin TerminationConfig.
 */
export interface TerminationBlockData {
    evolutions?: number;
    time?: number;
    delta?: number;
    iterations?: number;
}

/**
 * Solver section data.
 * Maps to Kotlin SolverConfig.
 * `provider` and `algorithm` match Kotlin enum names (uppercase).
 */
export interface SolverSectionData {
    algorithm?: "NSGAII" | "IBEA" | "SPEA2" | "SMSMOEA" | "VEGA" | "PESA2" | "PAES" | "RANDOM";
    parameters?: AlgorithmParametersData;
    termination?: TerminationBlockData;
    batches?: number;
    /**
     * Per-evaluation timeout for constraint and objective scripts, in seconds.
     * Same unit as `termination.time`. Optional; defaults to 30 s on the backend.
     */
    scriptTimeout?: number;
}

/**
 * Combined response from the MDEO request handler.
 * Keys match Kotlin SearchConfig / SolverConfig directly so no conversion
 * is needed in the execution handler.
 */
export interface MdeoRequestResponse {
    search?: SearchSectionData;
    solver?: SolverSectionData;
}
