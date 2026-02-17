import { createRule, or, many, ref, createExternalTerminalRule, unorderedGroup, group } from "@mdeo/language-common";
import { generateImportRules } from "@mdeo/language-shared";
import {
    ProblemSection,
    GoalSection,
    ConstraintReference,
    Objective,
    Refinement,
    SingleMultiplicity,
    RangeMultiplicity,
    Multiplicity,
    ScriptFunction,
    MetamodelClass,
    Property,
    configOptimizationFileScopingConfig,
    FunctionImport,
    FunctionFileImport
} from "./optimizationTypes.js";

/**
 * External terminal rules from the base language-common grammar.
 * These must be defined using external references since config-optimization
 * should not directly import standard terminals.
 */
const ID = createExternalTerminalRule<string>("ID");
const INT = createExternalTerminalRule<number>("INT");
const STRING = createExternalTerminalRule<string>("STRING");
const NEWLINE = createExternalTerminalRule<string>("NEWLINE");

/**
 * Single multiplicity rule.
 * Matches either a numeric value or one of the special multiplicity symbols: *, +, ?
 */
export const SingleMultiplicityRule = createRule("ConfigSingleMultiplicityRule")
    .returns(SingleMultiplicity)
    .as(({ set }) => [or(set("numericValue", INT), set("value", "*", "+", "?"))]);

/**
 * Range multiplicity rule.
 * Matches a range specification like "1..*" or "0..5".
 */
export const RangeMultiplicityRule = createRule("ConfigRangeMultiplicityRule")
    .returns(RangeMultiplicity)
    .as(({ set }) => [set("lower", INT), "..", or(set("upper", "*"), set("upperNumeric", INT))]);

/**
 * Multiplicity rule.
 * Matches multiplicity specifications in brackets, either single or range.
 */
export const MultiplicityRule = createRule("ConfigMultiplicityRule")
    .returns(Multiplicity)
    .as(() => ["[", or(SingleMultiplicityRule, RangeMultiplicityRule), "]"]);

/**
 * Problem section content rule (without keyword).
 * Parses the problem section content with metamodel and model file paths.
 * The metamodel and model properties can appear in any order.
 * The keyword will be added by the wrapper rule in language-config composition.
 * Syntax:
 *   {
 *       metamodel = "./path/to/metamodel"
 *       model = "./path/to/model"
 *   }
 */
export const ProblemSectionContentRule = createRule("ProblemSectionContentRule")
    .returns(ProblemSection)
    .as(({ set }) => [
        "{",
        many(NEWLINE),
        unorderedGroup(
            group("metamodel", "=", set("metamodel", STRING), many(NEWLINE)),
            group("model", "=", set("model", STRING), many(NEWLINE))
        ),
        "}"
    ]);

/**
 * Generated import rules for function imports.
 * Uses the file-scoping pattern from language-shared.
 */
export const { importRule: FunctionImportRule, fileImportRule: FunctionFileImportRule } = generateImportRules(
    configOptimizationFileScopingConfig,
    FunctionImport,
    FunctionFileImport,
    ID,
    ID,
    STRING,
    NEWLINE
);

/**
 * Constraint reference rule.
 * Syntax: constraint <functionRef>
 */
export const ConstraintReferenceRule = createRule("ConfigConstraintReferenceRule")
    .returns(ConstraintReference)
    .as(({ set }) => ["constraint", set("constraint", ref(ScriptFunction, ID))]);

/**
 * Objective rule.
 * Syntax: maximize <functionRef> | minimize <functionRef>
 */
export const ObjectiveRule = createRule("ConfigObjectiveRule")
    .returns(Objective)
    .as(({ set }) => [set("type", "maximize", "minimize"), set("objective", ref(ScriptFunction, ID))]);

/**
 * Refinement rule.
 * Syntax: refine ClassRef.fieldRef[multiplicity]
 */
export const RefinementRule = createRule("ConfigRefinementRule")
    .returns(Refinement)
    .as(({ set }) => [
        "refine",
        set("class", ref(MetamodelClass, ID)),
        ".",
        set("field", ref(Property, ID)),
        set("multiplicity", MultiplicityRule)
    ]);

/**
 * Goal section content rule (without keyword).
 * Parses the goal section content with imports, constraints, objectives, and refinements.
 * The keyword will be added by the wrapper rule in language-config composition.
 * Syntax:
 *   {
 *       import { func1, func2 } from "./script.fn"
 *       constraint constraint1
 *       maximize goal1
 *       minimize goal2
 *       refine MyClass.myField[0..*]
 *   }
 */
export const GoalSectionContentRule = createRule("GoalSectionContentRule")
    .returns(GoalSection)
    .as(({ add }) => [
        "{",
        many(NEWLINE),
        many(
            or(
                add("imports", FunctionFileImportRule),
                add("constraints", ConstraintReferenceRule),
                add("objectives", ObjectiveRule),
                add("refinements", RefinementRule)
            ),
            many(NEWLINE)
        ),
        "}"
    ]);
