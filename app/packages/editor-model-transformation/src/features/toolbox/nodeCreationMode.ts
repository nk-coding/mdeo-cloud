/**
 * Modes for creating pattern instances in the model transformation editor.
 * Corresponds to the modifier keyword applied to a PatternObjectInstance.
 */
export enum NodeCreationMode {
    /**
     * No modifier — plain pattern matching (persist existing nodes).
     */
    PERSIST = "persist",

    /**
     * Create modifier — the element will be created during transformation.
     */
    CREATE = "create",

    /**
     * Delete modifier — the element will be deleted during transformation.
     */
    DELETE = "delete",

    /**
     * Require modifier — the element must exist (NAC positive).
     */
    REQUIRE = "require",

    /**
     * Forbid modifier — the element must not exist (NAC negative).
     */
    FORBID = "forbid"
}
