import type { LanguageServices } from "@mdeo/language-common";
import type { URI } from "vscode-uri";
import type { LangiumInstance } from "../langium/langiumInstance.js";
import type { ServerApi } from "../service/serverApi.js";
import type { ServiceAdditionalServices } from "../langium/types.js";

/**
 * Handler for file data computation requests
 *
 * @template T The return type of the handler
 * @template S The type of LanguageServices used
 */
export type FileDataHandler<T = unknown, S = object> = (context: FileDataContext<S>) => Promise<FileDataResult<T>>;

/**
 * Context provided to file data handlers
 */
export interface FileDataContext<T = object> {
    /**
     * The URI of the file, already registered in the Langium workspace
     */
    uri: URI;

    /**
     * The version of the file
     */
    version: number;

    /**
     * The Langium instance to use for processing
     */
    instance: LangiumInstance<T>;

    /**
     * The Langium services for this language
     */
    services: LanguageServices & ServiceAdditionalServices & T;

    /**
     * Server API for accessing backend endpoints
     */
    serverApi: ServerApi;
}

/**
 * Result of a file data computation
 * @template T The type of the computed data
 */
export interface FileDataResult<T = unknown> {
    /**
     * The computed data (JSON value)
     */
    data: T;

    /**
     * File dependencies (paths and versions)
     */
    fileDependencies: FileDependency[];

    /**
     * Data dependencies (other file data computations)
     */
    dataDependencies: DataDependency[];

    /**
     * Additional file data computed as part of this request
     */
    additionalFileData?: AdditionalFileData<T>[];
}

/**
 * File dependency information
 */
export interface FileDependency {
    /**
     * Path to the file
     */
    path: string;
    /**
     * Version of the file
     */
    version: number;
}
/**
 * Data dependency information
 */

export interface DataDependency {
    /**
     * Path to the file
     */
    path: string;
    /**
     * Key for the file-data request
     */
    key: string;
    /**
     * Version of the file the data was computed for
     */
    version: number;
}

/**
 * Additional file data computed as part of another computation
 * Eagerly cached in the backend for efficiency.
 */
export interface AdditionalFileData<T = unknown> extends Omit<FileDataResult<T>, "additionalFileData"> {
    /**
     * Path to the file
     */
    path: string;
    /**
     * Key for the file-data request
     */
    key: string;
    /**
     * Version of the file the data was computed for
     */
    sourceVersion: number;
}
