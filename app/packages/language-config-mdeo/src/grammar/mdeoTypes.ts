import { createInterface, createType, createExternalInterface, Ref, Union, type ASTType } from "@mdeo/language-common";
import { FileScopingConfig, generateImportTypes } from "@mdeo/language-shared";
import type { ClassType, PropertyType } from "@mdeo/language-metamodel";
import type { ModelTransformationType } from "@mdeo/language-model-transformation";

/**
 * External reference to Class interface from language-metamodel.
 * Used for referencing classes in mutation operators.
 */
export const MetamodelClass = createExternalInterface<ClassType>("Class");

/**
 * External reference to Property interface from language-metamodel.
 * Used for referencing edge properties in mutation operators.
 */
export const Property = createExternalInterface<PropertyType>("Property");

/**
 * External reference to ModelTransformation interface from language-model-transformation.
 * Used for referencing model transformation files in the search section.
 */
export const ModelTransformation = createExternalInterface<ModelTransformationType>("ModelTransformation");

/**
 * Configuration for file-scoped model transformation imports in the MDEO config.
 * Uses the shared file-scoping pattern from language-shared.
 */
export const configMdeoFileScopingConfig = new FileScopingConfig<ModelTransformationType>(
    "ConfigMdeoTransformation",
    ModelTransformation
);

/**
 * Generated import types for model transformations using the file-scoping pattern.
 */
export const { importType: TransformationImport, fileImportType: TransformationFileImport } =
    generateImportTypes(configMdeoFileScopingConfig);

/**
 * Type representing a TransformationImport AST node.
 */
export type TransformationImportType = ASTType<typeof TransformationImport>;

/**
 * Type representing a TransformationFileImport AST node.
 */
export type TransformationFileImportType = ASTType<typeof TransformationFileImport>;

/**
 * A "using" path declaration for model transformation files.
 * Format: using "path/to/transformation.mt"
 */
export const UsingPath = createInterface("ConfigMdeoUsingPath").attrs({
    path: String
});

/**
 * Type representing a UsingPath AST node.
 */
export type UsingPathType = ASTType<typeof UsingPath>;

/**
 * A class-based mutation operator.
 * Supports: create ClassName, delete ClassName, mutate ClassName
 */
export const ClassMutation = createInterface("ConfigMdeoClassMutation").attrs({
    operator: Union("create", "delete", "mutate"),
    class: Ref(() => MetamodelClass)
});

/**
 * Type representing a ClassMutation AST node.
 */
export type ClassMutationType = ASTType<typeof ClassMutation>;

/**
 * An edge-based mutation operator.
 * Supports: add ClassName.edgeName, remove ClassName.edgeName, mutate ClassName.edgeName
 */
export const EdgeMutation = createInterface("ConfigMdeoEdgeMutation").attrs({
    operator: Union("add", "remove", "mutate"),
    class: Ref(() => MetamodelClass),
    edge: Ref(() => Property)
});

/**
 * Type representing an EdgeMutation AST node.
 */
export type EdgeMutationType = ASTType<typeof EdgeMutation>;

/**
 * Mutations block in the search section.
 * Contains using paths and mutation operators.
 */
export const MutationsBlock = createInterface("ConfigMdeoMutationsBlock").attrs({
    usingPaths: [UsingPath],
    classMutations: [ClassMutation],
    edgeMutations: [EdgeMutation]
});

/**
 * Type representing a MutationsBlock AST node.
 */
export type MutationsBlockType = ASTType<typeof MutationsBlock>;

/**
 * Search section interface.
 * Defines the mutations block for the search operators.
 */
export const SearchSection = createInterface("ConfigMdeoSearchSection").attrs({
    mutations: [MutationsBlock]
});

/**
 * Type representing a SearchSection AST node.
 */
export type SearchSectionType = ASTType<typeof SearchSection>;

/**
 * Bare integer step size.
 * Example: step = 3
 */
export const MutationStepNumeric = createInterface("ConfigMdeoMutationStepNumeric").attrs({
    value: Number
});
export type MutationStepNumericType = ASTType<typeof MutationStepNumeric>;

/**
 * Fixed step size with no argument (defaults to 1 at runtime).
 * Example: step = fixed
 */
export const MutationStepFixed = createInterface("ConfigMdeoMutationStepFixed").attrs({});
export type MutationStepFixedType = ASTType<typeof MutationStepFixed>;

/**
 * Fixed step size with an explicit integer argument.
 * Example: step = fixed(3)
 */
export const MutationStepFixedN = createInterface("ConfigMdeoMutationStepFixedN").attrs({
    n: Number
});
export type MutationStepFixedNType = ASTType<typeof MutationStepFixedN>;

/**
 * Random step size drawn uniformly from [lower, upper) each call.
 * Example: step = interval(1, 5)
 */
export const MutationStepInterval = createInterface("ConfigMdeoMutationStepInterval").attrs({
    lower: Number,
    upper: Number
});
export type MutationStepIntervalType = ASTType<typeof MutationStepInterval>;

/**
 * Union of all valid mutation step value forms.
 */
export const MutationStepValue = createType("ConfigMdeoMutationStepValue").types(
    MutationStepNumeric,
    MutationStepFixed,
    MutationStepFixedN,
    MutationStepInterval
);
export type MutationStepValueType = ASTType<typeof MutationStepValue>;

/**
 * The `mutation { }` nested block inside the `parameters` block.
 * Collects all mutation.* configuration keys as direct named fields.
 */
export const MutationBlock = createInterface("ConfigMdeoMutationBlock").attrs({
    step: [MutationStepValue],
    strategy: [Union("random", "repetitive")],
    selection: [Union("random")],
    application: [Union("random")],
    credit: [Union("random")],
    repair: [Union("default")]
});
export type MutationBlockType = ASTType<typeof MutationBlock>;

/**
 * The `archive { }` nested block inside the `parameters` block.
 * Used only by PESA2 and PAES algorithms.
 */
export const ArchiveBlock = createInterface("ConfigMdeoArchiveBlock").attrs({
    size: [Number]
});
export type ArchiveBlockType = ASTType<typeof ArchiveBlock>;

/**
 * The `parameters { }` block in the solver section.
 * Contains population, variation, and optional sub-blocks.
 */
export const AlgorithmParameters = createInterface("ConfigMdeoAlgorithmParameters").attrs({
    population: [Number],
    variation: [Union("mutation", "genetic", "probabilistic")],
    mutation: [MutationBlock],
    bisections: [Number],
    archive: [ArchiveBlock]
});
export type AlgorithmParametersType = ASTType<typeof AlgorithmParameters>;

/**
 * The `termination { }` block in the solver section.
 * Multiple conditions may be specified; they are combined as OR (first hit stops the run).
 *
 * - `evolutions` — number of generations (actual evaluations = evolutions × population)
 * - `time`       — wall-clock time limit in seconds
 * - `delta`      — convergence threshold (percentage)
 * - `iterations` — consecutive steps below delta before terminating (default 3)
 */
export const TerminationBlock = createInterface("ConfigMdeoTerminationBlock").attrs({
    evolutions: [Number],
    time: [Number],
    delta: [Number],
    iterations: [Number]
});
export type TerminationBlockType = ASTType<typeof TerminationBlock>;

/**
 * The solver `{ }` section content.
 *
 * - `provider`    — algorithm provider (`moea` | `ecj`)
 * - `algorithm`   — algorithm name (`NSGAII` | `IBEA` | … | `RANDOM`)
 * - `parameters`  — algorithm parameters block
 * - `termination` — termination condition block
 * - `batches`     — number of independent runs
 */
export const SolverSection = createInterface("ConfigMdeoSolverSection").attrs({
    provider: [Union("moea", "ecj")],
    algorithm: [Union("NSGAII", "IBEA", "SPEA2", "SMSMOEA", "VEGA", "PESA2", "PAES", "RANDOM")],
    parameters: [AlgorithmParameters],
    termination: [TerminationBlock],
    batches: [Number]
});
export type SolverSectionType = ASTType<typeof SolverSection>;
