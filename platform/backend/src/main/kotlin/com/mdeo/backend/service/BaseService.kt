package com.mdeo.backend.service

/**
 * Base service class.
 */
abstract class BaseService {
    /**
     * Normalizes a file or directory path by removing leading and trailing slashes.
     *
     * @param path The original path
     * @return The normalized path
     */
    protected fun normalizePath(path: String): String {
        var p = path
        if (p.startsWith("/")) {
            p = p.substring(1)
        }
        if (p.endsWith("/") && p.length > 1) {
            p = p.substring(0, p.length - 1)
        }
        return p
    }
}