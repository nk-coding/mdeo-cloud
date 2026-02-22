/**
 * Captures enum information extracted from a metamodel.
 */

export interface MetamodelEnumInfo {
    /**
     * The simple name of the enum
     */
    name: string;
    /**
     * The package in which the enum is defined (e.g. `"metamodel./path/to/file.mm"`)
     */
    package: string;
    /**
     * The names of all enum entries
     */
    entries: string[];
}
