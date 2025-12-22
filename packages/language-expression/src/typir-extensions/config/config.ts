import type { BaseClassTypeRef, ClassType, FunctionType } from "./type.js";

/**
 * Configuration for the type system.
 * Defines all classes, lambda super types, and global functions available in the type system.
 */
export interface TypeSystemConfig {
    /**
     * Array of all class type definitions in the type system
     */
    classes: ClassType[];

    /**
     * Super types that all lambda types extend
     */
    lambdaSuperTypes: BaseClassTypeRef[];

    /**
     * Global scope configuration
     */
    globalScope: {
        /**
         * Record of globally available functions mapped by their names
         */
        functions: Record<string, FunctionType>;
    };
}
