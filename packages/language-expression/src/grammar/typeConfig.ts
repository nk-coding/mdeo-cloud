/**
 * Configuration for type annotations
 */
export class TypeConfig {
    /**
     * The name for the BaseType type.
     */
    readonly baseTypeName: string;

    /**
     * The name for the Type rule.
     */
    readonly typeRuleName: string;

    /**
     * The name for the ClassType type.
     */
    readonly classTypeTypeName: string;

    /**
     * The name for the ClassType rule.
     */
    readonly classTypeRuleName: string;

    /**
     * The name for the LambdaType type.
     */
    readonly lambdaTypeTypeName: string;

    /**
     * The name for the LambdaType rule.
     */
    readonly lambdaTypeRuleName: string;

    /**
     * The name for the VoidType type.
     */
    readonly voidTypeTypeName: string;

    /**
     * The name for the VoidType rule.
     */
    readonly voidTypeRuleName: string;

    /**
     * The name for the ReturnType type.
     */
    readonly returnTypeTypeName: string;

    /**
     * The name for the ReturnType rule.
     */
    readonly returnTypeRuleName: string;

    /**
     * Creates a new TypeConfig.
     *
     * @param prefix Prefix for naming generated rules and types.
     */
    constructor(readonly prefix: string) {
        this.baseTypeName = prefix + "BaseType";
        this.typeRuleName = prefix + "TypeRule";
        this.classTypeTypeName = prefix + "ClassType";
        this.classTypeRuleName = prefix + "ClassTypeRule";
        this.lambdaTypeTypeName = prefix + "LambdaType";
        this.lambdaTypeRuleName = prefix + "LambdaTypeRule";
        this.voidTypeTypeName = prefix + "VoidType";
        this.voidTypeRuleName = prefix + "VoidTypeRule";
        this.returnTypeTypeName = prefix + "ReturnType";
        this.returnTypeRuleName = prefix + "ReturnTypeRule";
    }
}
