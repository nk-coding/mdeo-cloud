package com.mdeo.script.compiler.registry.function

import com.mdeo.script.ast.TypedAst
import com.mdeo.script.ast.TypedFunction
import com.mdeo.script.ast.TypedImport

/**
 * Reference to a function imported from another file.
 *
 * @param uri The URI of the source file containing the function.
 * @param ref The actual function name in the source file.
 */
data class ImportedFunctionRef(
    val uri: String,
    val ref: String
)

/**
 * File-specific function registry that handles local and imported function lookups.
 *
 * This registry implements a hierarchical lookup:
 * 1. Local functions defined in the current file
 * 2. Imported functions resolved through their source file registries
 * 3. Parent registry (typically the global registry)
 *
 * @param parent The parent registry (typically GlobalFunctionRegistry).
 * @param importedFileRegistries Map of file URIs to their registries for resolving imports.
 */
class FileFunctionRegistry(
    private val parent: FunctionRegistry,
    private val importedFileRegistries: Map<String, FileFunctionRegistry> = emptyMap()
) : FunctionRegistry {

    private val localFunctions: MutableMap<String, FunctionDefinition> = mutableMapOf()
    private val importedFunctions: MutableMap<String, ImportedFunctionRef> = mutableMapOf()

    /**
     * Registers a local function from the file.
     *
     * Extracts parameter types from the TypedFunction and stores them in the registry.
     * File-scope functions are registered with a single overload using the empty string key.
     *
     * @param func The typed function from the AST.
     * @param ast The TypedAst containing the types array.
     * @param ownerClass The JVM internal class name for this file.
     */
    fun registerLocalFunction(func: TypedFunction, ast: TypedAst, ownerClass: String) {
        val parameters = func.parameters.map { param ->
            FunctionParameter(
                name = param.name,
                type = ast.types[param.type]
            )
        }

        val definition = createSimpleFileFunction(
            name = func.name,
            parameters = parameters,
            returnType = ast.types[func.returnType],
            ownerClass = ownerClass
        )

        localFunctions[func.name] = definition
    }

    /**
     * Registers an imported function.
     *
     * Stores a reference that will be resolved through the imported file's registry.
     *
     * @param import The TypedImport declaration from the AST.
     */
    fun registerImport(import: TypedImport) {
        importedFunctions[import.name] = ImportedFunctionRef(
            uri = import.uri,
            ref = import.ref
        )
    }

    /**
     * Looks up a function by name following the hierarchy.
     *
     * The lookup order is:
     * 1. Local functions defined in this file
     * 2. Imported functions (resolved through imported file registries)
     * 3. Parent registry (global functions)
     *
     * @param name The function name to look up.
     * @return The function definition, or null if not found.
     */
    override fun lookupFunction(name: String): FunctionDefinition? {
        localFunctions[name]?.let { return it }

        importedFunctions[name]?.let { importRef ->
            val importedRegistry = importedFileRegistries[importRef.uri]
            return importedRegistry?.lookupFunction(importRef.ref)
        }

        return parent.lookupFunction(name)
    }

    /**
     * Gets the parent registry.
     *
     * For [FileFunctionRegistry], the parent is always non-null (typically the global registry).
     * The interface returns nullable to support the root registry case.
     *
     * @return The parent function registry (never null for file registries).
     */
    override fun getParent(): FunctionRegistry = parent

    /**
     * Gets the imported function reference for a given local name.
     *
     * This is useful for determining if a function call is to an imported function
     * and getting the actual function name in the source file.
     *
     * @param name The local name of the imported function.
     * @return The import reference, or null if not an imported function.
     */
    fun getImportRef(name: String): ImportedFunctionRef? = importedFunctions[name]

    companion object {
        /**
         * Creates FileFunctionRegistry instances for all files in a compilation.
         *
         * This is a two-phase process:
         * 1. Create registries for all files with empty import maps
         * 2. Link them together for cross-file import resolution
         *
         * @param files Map of file URIs to their TypedAst.
         * @param globalRegistry The global function registry to use as parent.
         * @param classNameResolver Function to convert a file URI to a JVM class name.
         * @return Map of file URIs to their FileFunctionRegistry instances.
         */
        fun createForCompilation(
            files: Map<String, TypedAst>,
            globalRegistry: FunctionRegistry,
            classNameResolver: (String) -> String
        ): Map<String, FileFunctionRegistry> {
            // Create all registries first with a placeholder empty map for imports.
            // We'll link them together after all are created.
            val registries = mutableMapOf<String, FileFunctionRegistry>()

            for ((uri, ast) in files) {
                val className = classNameResolver(uri)
                // Initially create with empty import map - will be updated below
                val registry = FileFunctionRegistry(globalRegistry, emptyMap())

                for (func in ast.functions) {
                    registry.registerLocalFunction(func, ast, className)
                }

                for (import in ast.imports) {
                    registry.registerImport(import)
                }

                registries[uri] = registry
            }

            // Now create the final linked registries that reference each other.
            // This allows transitive import resolution to work correctly.
            val linkedRegistries = mutableMapOf<String, FileFunctionRegistry>()
            
            // First pass: create all linked registries with the linkedRegistries map
            // Since we're building linkedRegistries iteratively, each registry will
            // see the final complete map due to reference semantics.
            for ((uri, ast) in files) {
                val className = classNameResolver(uri)
                // Pass linkedRegistries itself - it will be populated as we iterate
                val linkedRegistry = FileFunctionRegistry(globalRegistry, linkedRegistries)

                for (func in ast.functions) {
                    linkedRegistry.registerLocalFunction(func, ast, className)
                }

                for (import in ast.imports) {
                    linkedRegistry.registerImport(import)
                }

                linkedRegistries[uri] = linkedRegistry
            }

            return linkedRegistries
        }
    }
}
