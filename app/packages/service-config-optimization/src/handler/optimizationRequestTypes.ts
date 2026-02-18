/**
 * Data extracted from the optimization problem section.
 */
export interface ProblemSectionData {
    /**
     * Relative path to the .metamodel file.
     */
    metamodel: string;
    /**
     * Relative path to the .model file.
     */
    model: string;
}

/**
 * A single import entry from a function file import statement.
 */
export interface FunctionImportData {
    /**
     * The function name as referenced in the source.
     */
    entity: string;
    /**
     * Optional alias name.
     */
    alias?: string;
}

/**
 * A file import block (e.g. `import { fn1, fn2 } from "./script.fn"`).
 */
export interface FunctionFileImportData {
    /**
     * The relative path of the imported file.
     */
    file: string;
    /**
     * All named imports from this file.
     */
    imports: FunctionImportData[];
}

/**
 * A constraint reference in the goal section (e.g., `constraint myFn`).
 */
export interface ConstraintData {
    /**
     * The referenced constraint function name.
     */
    functionName: string;
}

/**
 * An objective in the goal section (e.g., `maximize myFn`).
 */
export interface ObjectiveData {
    /**
     * Whether the objective is maximized or minimized.
     */
    type: "maximize" | "minimize";
    /**
     * The referenced objective function name.
     */
    functionName: string;
}

/**
 * A single multiplicity (numeric or symbolic: *, +, ?).
 */
export type SingleMultiplicityData = { kind: "single"; value: string | number };

/**
 * A range multiplicity (e.g., 0..*, 1..5).
 */
export type RangeMultiplicityData = { kind: "range"; lower: number; upper: "*" | number };

/**
 * Union of single and range multiplicity data.
 */
export type MultiplicityData = SingleMultiplicityData | RangeMultiplicityData;

/**
 * A refinement specification in the goal section (e.g., `refine Class.field[0..*]`).
 */
export interface RefinementData {
    /**
     * The name of the refined class.
     */
    className: string;
    /**
     * The name of the refined field.
     */
    fieldName: string;
    /**
     * The multiplicity constraint.
     */
    multiplicity: MultiplicityData;
}

/**
 * Data extracted from the optimization goal section.
 */
export interface GoalSectionData {
    /**
     * Function file imports declared in this goal section.
     */
    imports: FunctionFileImportData[];
    /**
     * Constraint function references.
     */
    constraints: ConstraintData[];
    /**
     * Objective function references with direction.
     */
    objectives: ObjectiveData[];
    /**
     * Class field refinements with multiplicities.
     */
    refinements: RefinementData[];
}

/**
 * The overall response from the optimization plugin request handler.
 * Keys are section names ("problem", "goal"); values are the corresponding section data.
 */
export interface OptimizationRequestResponse {
    problem?: ProblemSectionData;
    goal?: GoalSectionData;
}
