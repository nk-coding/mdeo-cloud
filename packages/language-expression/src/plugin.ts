import type { ServiceProvider } from "@mdeo/language-common";

/**
 * The service provider for the expression language part.
 * Used for the plugin architecture.
 */
export type ExpressionServiceProvider<T> = ServiceProvider<object, T>;
