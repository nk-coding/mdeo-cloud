import type {
    ActionSchema,
    ActionSchemaTypeForm,
    ActionSchemaEnumForm,
    ActionSchemaElementsForm,
    ActionSchemaPropertiesForm,
    ActionSchemaOptionalForm,
    ActionValidationError
} from "@mdeo/language-common";

/**
 * Type guard to check if schema is a type form (has primitive type)
 *
 * @param schema The schema to check
 * @returns True if schema is a type form
 */
export function isTypeForm(schema: ActionSchema): schema is ActionSchemaTypeForm {
    return "type" in schema && typeof (schema as ActionSchemaTypeForm).type === "string";
}

/**
 * Type guard to check if schema is an enum form (has enum array)
 *
 * @param schema The schema to check
 * @returns True if schema is an enum form
 */
export function isEnumForm(schema: ActionSchema): schema is ActionSchemaEnumForm {
    return "enum" in schema && Array.isArray((schema as ActionSchemaEnumForm).enum);
}

/**
 * Type guard to check if schema is an elements form (has elements for arrays)
 *
 * @param schema The schema to check
 * @returns True if schema is an elements form
 */
export function isElementsForm(schema: ActionSchema): schema is ActionSchemaElementsForm {
    return "elements" in schema && (schema as ActionSchemaElementsForm).elements !== undefined;
}

/**
 * Type guard to check if schema is a properties form (has properties)
 *
 * @param schema The schema to check
 * @returns True if schema is a properties form
 */
export function isPropertiesForm(schema: ActionSchema): schema is ActionSchemaPropertiesForm {
    return "properties" in schema;
}

/**
 * Type guard to check if schema is an optional form (has optional field)
 *
 * @param schema The schema to check
 * @returns True if schema is an optional form
 */
export function isOptionalForm(schema: ActionSchema): schema is ActionSchemaOptionalForm {
    return "optional" in schema && (schema as ActionSchemaOptionalForm).optional !== undefined;
}

/**
 * Generates a default value for a given schema
 *
 * @param schema The schema to generate default value for
 * @returns The default value appropriate for the schema type
 */
export function generateDefaultValue(schema: ActionSchema): unknown {
    if (isOptionalForm(schema)) {
        return undefined;
    }

    if (isElementsForm(schema)) {
        return [];
    }

    if (isPropertiesForm(schema)) {
        const result: Record<string, unknown> = {};
        for (const [key, propSchema] of Object.entries(schema.properties)) {
            result[key] = generateDefaultValue(propSchema);
        }
        return result;
    }

    if (isTypeForm(schema)) {
        switch (schema.type) {
            case "boolean":
                return false;
            case "string":
                return "";
            case "int8":
            case "int16":
            case "int32":
            case "uint8":
            case "uint16":
            case "uint32":
            case "float32":
            case "float64":
                return 0;
            default:
                return undefined;
        }
    }

    if (isEnumForm(schema)) {
        return undefined;
    }

    return undefined;
}

/**
 * Deep clones a value using the schema definition to properly handle nested structures.
 * This ensures no Vue proxies remain in the cloned object.
 *
 * @param value The value to clone
 * @param schema The schema defining the structure
 * @returns A deep clone of the value
 */
export function deepCloneWithSchema(value: unknown, schema: ActionSchema): unknown {
    if (value === undefined || value === null) {
        return value;
    }

    if (isOptionalForm(schema)) {
        if (value === undefined || value === null) {
            return undefined;
        }
        return deepCloneWithSchema(value, schema.optional);
    }

    if (isElementsForm(schema)) {
        if (!Array.isArray(value)) {
            return [];
        }
        return value.map((item) => deepCloneWithSchema(item, schema.elements));
    }

    if (isPropertiesForm(schema)) {
        const result: Record<string, unknown> = {};
        const obj = value as Record<string, unknown>;

        for (const [key, propSchema] of Object.entries(schema.properties)) {
            if (key in obj) {
                result[key] = deepCloneWithSchema(obj[key], propSchema);
            }
        }

        return result;
    }

    if (isTypeForm(schema)) {
        return value;
    }

    if (isEnumForm(schema)) {
        return value;
    }

    return value;
}

/**
 * Filters validation errors to only those matching a given path prefix
 *
 * @param errors The array of validation errors
 * @param path The path prefix to filter by
 * @returns Errors that match the path prefix
 */
export function filterErrorsByPath(errors: ActionValidationError[], path: string): ActionValidationError[] {
    if (!path || path === "") {
        return errors;
    }

    return errors.filter((error) => {
        return error.path === path || error.path.startsWith(path + "/");
    });
}

/**
 * Gets errors that exactly match a given path
 *
 * @param errors The array of validation errors
 * @param path The exact path to match
 * @returns Errors that exactly match the path
 */
export function getErrorsForPath(errors: ActionValidationError[], path: string): ActionValidationError[] {
    return errors.filter((error) => error.path === path);
}

/**
 * Verifies that a value matches a schema and fixes it if necessary.
 * Uses existing values where they fit the schema, otherwise generates defaults.
 *
 * @param value The value to verify and potentially fix
 * @param schema The schema to verify against
 * @returns A value that conforms to the schema
 */
export function verifyAndFix(value: unknown, schema: ActionSchema): unknown {
    if (isOptionalForm(schema)) {
        if (value === undefined || value === null) {
            return undefined;
        }
        return verifyAndFix(value, schema.optional);
    }

    if (isElementsForm(schema)) {
        if (!Array.isArray(value)) {
            return [];
        }
        return value.map((item) => verifyAndFix(item, schema.elements));
    }

    if (isPropertiesForm(schema)) {
        const result: Record<string, unknown> = {};
        const obj = typeof value === "object" && value !== null ? (value as Record<string, unknown>) : {};

        for (const [key, propSchema] of Object.entries(schema.properties)) {
            if (key in obj) {
                result[key] = verifyAndFix(obj[key], propSchema);
            } else {
                result[key] = generateDefaultValue(propSchema);
            }
        }

        return result;
    }

    if (isTypeForm(schema)) {
        switch (schema.type) {
            case "boolean":
                return typeof value === "boolean" ? value : false;
            case "string":
                return typeof value === "string" ? value : "";
            case "int8":
            case "int16":
            case "int32":
            case "uint8":
            case "uint16":
            case "uint32":
            case "float32":
            case "float64":
                return typeof value === "number" ? value : 0;
            default:
                return undefined;
        }
    }

    if (isEnumForm(schema)) {
        if (schema.enum.includes(value as never)) {
            return value;
        }
        return schema.enum.length > 0 ? schema.enum[0] : undefined;
    }

    return generateDefaultValue(schema);
}
