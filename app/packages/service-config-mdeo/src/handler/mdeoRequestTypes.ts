/**
 * Data for a "using" path in the search section.
 */
export interface UsingPathData {
    /**
     * The path to the model transformation file.
     */
    path: string;
}

/**
 * Data for a class mutation (create/delete/mutate).
 */
export interface ClassMutationData {
    /**
     * The mutation operator type.
     */
    operator: "create" | "delete" | "mutate";
    /**
     * The name of the class being mutated.
     */
    className: string;
}

/**
 * Data for an edge mutation (add/remove/mutate).
 */
export interface EdgeMutationData {
    /**
     * The mutation operator type.
     */
    operator: "add" | "remove" | "mutate";
    /**
     * The name of the class owning the edge.
     */
    className: string;
    /**
     * The name of the edge property.
     */
    edgeName: string;
}

/**
 * Data for the mutations block.
 */
export interface MutationsBlockData {
    /**
     * The model transformation file paths.
     */
    usingPaths: UsingPathData[];
    /**
     * The class mutations.
     */
    classMutations: ClassMutationData[];
    /**
     * The edge mutations.
     */
    edgeMutations: EdgeMutationData[];
}

/**
 * Data extracted from the search section.
 */
export interface SearchSectionData {
    /**
     * The mutations block data.
     */
    mutations?: MutationsBlockData;
}

/** Bare integer step size. Example: step = 3 */
export interface MutationStepNumericData {
    kind: "numeric";
    value: number;
}

/** Fixed step size with no argument (runtime default = 1). Example: step = fixed */
export interface MutationStepFixedData {
    kind: "fixed";
}

/** Fixed step size with an explicit integer argument. Example: step = fixed(3) */
export interface MutationStepFixedNData {
    kind: "fixedN";
    n: number;
}

/** Interval step size drawn from [lower, upper). Example: step = interval(1, 5) */
export interface MutationStepIntervalData {
    kind: "interval";
    lower: number;
    upper: number;
}

/** Discriminated union of all mutation step value forms. */
export type MutationStepValueData =
    | MutationStepNumericData
    | MutationStepFixedData
    | MutationStepFixedNData
    | MutationStepIntervalData;

/** Data for the mutation { } nested block. */
export interface MutationBlockData {
    step?: MutationStepValueData;
    strategy?: "random" | "repetitive";
    selection?: "random";
    application?: "random";
    credit?: "random";
    repair?: "default";
}

/** Data for the archive { } nested block. */
export interface ArchiveBlockData {
    size?: number;
}

/** Data for the parameters { } block. */
export interface AlgorithmParametersData {
    population?: number;
    variation?: "mutation" | "genetic" | "probabilistic";
    mutation?: MutationBlockData;
    bisections?: number;
    archive?: ArchiveBlockData;
}

/** Data for the termination { } block. */
export interface TerminationBlockData {
    evolutions?: number;
    time?: number;
    delta?: number;
    iterations?: number;
}

/** Data extracted from the solver section. */
export interface SolverSectionData {
    provider?: "moea" | "ecj";
    algorithm?: "NSGAII" | "IBEA" | "SPEA2" | "SMSMOEA" | "VEGA" | "PESA2" | "PAES" | "RANDOM";
    parameters?: AlgorithmParametersData;
    termination?: TerminationBlockData;
    batches?: number;
}

/**
 * Combined response from the MDEO request handler.
 */
export interface MdeoRequestResponse {
    /**
     * The search section data.
     */
    search?: SearchSectionData;
    /**
     * The solver section data.
     */
    solver?: SolverSectionData;
}
