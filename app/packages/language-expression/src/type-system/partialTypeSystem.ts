import type { TypirLangiumSpecifics } from "typir-langium";
import type { CustomClassType } from "../typir-extensions/kinds/custom-class/custom-class-type.js";
import type { ExpressionTypirServices } from "./services.js";
import type { Interface, AstReflection } from "@mdeo/language-common";
import { sharedImport } from "@mdeo/language-shared";
import { type AstNode } from "langium";
import {
    type TypeAssignability,
    type TypeInferenceCollector,
    type TypeInferenceRuleWithoutInferringChildren,
    type ValidationCollector,
    type ValidationRuleFunctional,
    type ValidationProblem,
    type InferenceProblem as InferenceProblemType
} from "typir";
import type { TypeSystemConfig } from "./typeSystemConfig.js";

const {
    ValidationProblem: ValidationProblemConstant,
    InferenceProblem: InferenceProblemConstant,
    InferenceRuleNotApplicable
} = sharedImport("typir");

/**
 * Represents a mapping of primitive type names to their corresponding CustomClassType instances.
 * Excludes special type configuration keys like "additionalTypes", "lambdaSuperTypes", and "void".
 */
export type PrimitiveTypes = Record<
    Exclude<keyof TypeSystemConfig, "additionalTypes" | "lambdaSuperTypes" | "Iterable">,
    CustomClassType
>;

/**
 * Abstract base class for partial type system implementations.
 * This class provides common functionality for registering type inference and validation rules,
 * and manages primitive types and commonly used services.
 *
 * @template Specifics The language-specific type system configuration extending TypirLangiumSpecifics
 * @template T The type configuration specific to the partial type system
 */
export abstract class PartialTypeSystem<Specifics extends TypirLangiumSpecifics, T> {
    /**
     * AST reflection service for type checking AST nodes.
     */
    protected readonly astReflection: AstReflection;

    /**
     * Type inference service for inferring types of AST nodes.
     */
    protected readonly inference: TypeInferenceCollector<Specifics>;

    /**
     * Validation problem type for creating validation errors.
     */
    protected readonly validationProblem: typeof ValidationProblem;

    /**
     * Inference problem type for creating type inference errors.
     */
    protected readonly inferenceProblem: typeof InferenceProblemType;

    /**
     * Assignability service for checking type assignability.
     */
    protected readonly assignability: TypeAssignability;

    /**
     * Validation collector for adding validation rules.
     */
    protected readonly validationCollector: ValidationCollector<Specifics>;

    /**
     * Creates a new PartialTypeSystem instance.
     *
     * @param typir The extended Typir services with expression-specific functionality
     * @param types The type configuration specific to this partial type system
     */
    constructor(
        protected readonly typir: ExpressionTypirServices<Specifics>,
        protected readonly types: T
    ) {
        this.astReflection = typir.langium.LangiumServices.AstReflection;
        this.inference = typir.Inference;
        this.validationProblem = ValidationProblemConstant;
        this.inferenceProblem = InferenceProblemConstant;
        this.assignability = typir.Assignability;
        this.validationCollector = typir.validation.Collector;
    }

    /**
     * Registers all type inference and validation rules for this partial type system.
     * This method must be implemented by concrete subclasses.
     */
    abstract registerRules(): void;

    /**
     * Registers a type inference rule for a specific AST node type.
     *
     * @template T The AST node type extending AstNode
     * @param type The interface/type definition for the AST node
     * @param rule The inference rule function to apply
     */
    protected registerInferenceRule<T extends AstNode>(
        type: Interface<T>,
        rule: TypeInferenceRuleWithoutInferringChildren<Specifics, T>
    ): void {
        this.inference.addInferenceRule((node) => {
            if (!this.astReflection.isInstance(node, type)) {
                return InferenceRuleNotApplicable;
            }
            return rule(node as T, this.typir);
        });
    }

    /**
     * Registers a validation rule for a specific AST node type.
     *
     * @template T The AST node type extending AstNode
     * @param type The interface/type definition for the AST node
     * @param rule The validation rule function to apply
     */
    protected registerValidationRule<T extends AstNode>(
        type: Interface<T>,
        rule: ValidationRuleFunctional<Specifics, T>
    ): void {
        this.validationCollector.addValidationRule(
            (node, accept) => {
                if (!this.astReflection.isInstance(node, type)) {
                    return;
                }
                return rule(node as T, accept, this.typir);
            },
            {
                languageKey: type.name
            }
        );
    }
}
