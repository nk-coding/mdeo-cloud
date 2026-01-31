package com.mdeo.execution.common.service

/**
 * Common execution scopes used for authorization.
 * Centralizes scope definitions to ensure consistency across services.
 */
object ExecutionScopes {
    const val EXECUTION_WRITE = "execution:write"
    const val EXECUTION_READ = "plugin:execution:read"
    const val EXECUTION_CANCEL = "plugin:execution:cancel"
    const val EXECUTION_DELETE = "plugin:execution:delete"
    const val EXECUTION_FILE_TREE = "plugin:execution:file-tree"
    const val EXECUTION_FILE_CONTENTS = "plugin:execution:file-contents"
}
