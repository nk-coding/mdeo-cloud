@file:Suppress("unused")
package com.mdeo.scriptexecution.config

import io.ktor.server.application.Application
import com.mdeo.execution.common.config.configureSerialization as commonConfigureSerialization

/**
 * Re-exports serialization configuration from execution-common.
 * This file maintains backward compatibility for existing imports.
 *
 * @see com.mdeo.execution.common.config.configureSerialization
 */

/**
 * Configures JSON serialization for the application.
 * Delegates to execution-common implementation.
 */
fun Application.configureSerialization() = commonConfigureSerialization()
