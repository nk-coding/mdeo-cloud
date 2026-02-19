/**
 * Data extracted from the optimization problem section.
 */
export interface ProblemSectionData {
    /**
     * Absolute path to the .metamodel file.
     */
    metamodel: string;
    /**
     * Absolute path to the .model file.
     */
    model: string;
}

/**
 * A multiplicity with explicit lower and upper bounds.
 * Use -1 for upper to indicate an unbounded (∞) upper limit.
 */
export interface MultiplicityData {
    lower: number;
    /**
     * -1 means unbounded (∞).
     */
    upper: number;
}

/**
 * A constraint reference in the goal section (e.g., `constraint myFn`).
 */
export interface ConstraintData {
    /**
     * Absolute path to the script file that contains the constraint function.
     */
    path: string;
    /**
     * The name of the constraint function as declared in the script file.
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
     * Absolute path to the script file that contains the objective function.
     */
    path: string;
    /**
     * The name of the objective function as declared in the script file.
     */
    functionName: string;
}

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
     * Constraint function references with their absolute file paths.
     */
    constraints: ConstraintData[];
    /**
     * Objective function references with direction and absolute file paths.
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
