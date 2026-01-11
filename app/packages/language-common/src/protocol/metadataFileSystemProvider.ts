import type { FileSystemProvider, URI } from "langium";

/**
 * Langium file system provider that supports reading and writing metadata for documents.
 * Metadata is a file-specific JSON object that can store additional information about the document
 */
export interface MetadataFileSystemProvider extends FileSystemProvider {
    /**
     * Reads a document's metadata asynchronously from a given URI.
     * @returns The metadata object of the file with the specified URI.
     */
    readMetadata(uri: URI): Promise<object>;

    /**
     * Writes a document's metadata asynchronously to a given URI.
     * @param uri The URI of the file to write metadata to.
     * @param metadata The metadata object to write.
     */
    writeMetadata(uri: URI, metadata: object): Promise<void>;
}

/**
 * Additional services for the MetadataFileSystemProvider.
 */
export interface MetadataFileSystemProviderAdditionalServices {
    shared: {
        workspace: {
            FileSystemProvider: MetadataFileSystemProvider;
        };
    };
}
