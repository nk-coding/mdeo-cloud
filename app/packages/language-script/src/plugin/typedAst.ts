import type { ReturnType } from "@mdeo/language-expression";
import { type TypedExpression, type TypedCallableBody } from "@mdeo/language-expression";

export type { TypedCallableBody } from "@mdeo/language-expression";

/**
 * Root of the TypedAST containing all program information.
 */
export interface TypedAst {
    /**
     * Array of all types used in the program.
     * Generics are replaced by Any? due to type erasure.
     * Indexed by typeIndex in expressions.
     */
    types: ReturnType[];

    /**
     * Optional absolute path of the metamodel file referenced by this script.
     * Undefined if the script has no metamodel import.
     */
    metamodelPath?: string;

    /**
     * All imports in the program.
     */
    imports: TypedImport[];

    /**
     * All top-level functions in the program.
     */
    functions: TypedFunction[];
}

/**
 * Import declaration.
 */
export interface TypedImport {
    /**
     * Name under which it is registered in the global scope.
     */
    name: string;

    /**
     * The name of the function in its source file.
     */
    ref: string;

    /**
     * URI from where it is imported.
     */
    uri: string;
}

/**
 * Function declaration.
 */
export interface TypedFunction {
    /**
     * Name of the function.
     */
    name: string;

    /**
     * Parameters of the function.
     */
    parameters: TypedParameter[];

    /**
     * Index into the types array for the return type.
     */
    returnType: number;

    /**
     * Body of the function.
     */
    body: TypedCallableBody;
}

/**
 * Function or lambda parameter.
 */
export interface TypedParameter {
    /**
     * Name of the parameter.
     */
    name: string;

    /**
     * Index into the types array for the parameter type.
     */
    type: number;
}

/**
 * Lambda expression.
 */
export interface TypedLambdaExpression extends TypedExpression {
    kind: "lambda";
    /**
     * Parameters of the lambda (names only, types are in the evalType).
     */
    parameters: string[];
    /**
     * Body of the lambda as statements.
     */
    body: TypedCallableBody;
}
