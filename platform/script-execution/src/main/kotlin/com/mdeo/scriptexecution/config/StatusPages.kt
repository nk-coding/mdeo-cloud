@file:Suppress("unused")
package com.mdeo.scriptexecution.config

/**
 * Re-exports status pages configuration from execution-common.
 * This file maintains backward compatibility for existing imports.
 *
 * @see com.mdeo.execution.common.config.configureStatusPages
 */

import io.ktor.server.application.Application
import com.mdeo.execution.common.config.configureStatusPages as commonConfigureStatusPages

/**
 * Configures status pages for error handling.
 * Delegates to execution-common implementation.
 */
fun Application.configureStatusPages() = commonConfigureStatusPages()
