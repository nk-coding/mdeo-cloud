/**
 * Captures enum information extracted from a metamodel.
 */

export interface MetamodelEnumInfo {
    /**
     * The simple name of the enum
     */
    name: string;
    /**
     * The package in which the enum is defined (e.g. `"enum/path/to/file.mm"`)
     */
    package: string;
    /**
     * The container package for this enum (e.g. `"enum-container/path/to/file.mm"`).
     * Derived directly from {@link package} by replacing the `enum/` prefix with `enum-container`.
     */
    containerPackage: string;
    /**
     * The names of all enum entries
     */
    entries: string[];
}
