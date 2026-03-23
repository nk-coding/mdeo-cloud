package com.mdeo.optimizer.provider

/**
 * Listener for optimization progress notifications.
 *
 * Implementations receive a callback after each generation of the evolutionary search completes,
 * allowing callers to report progress, collect metrics, or request cancellation by throwing
 * [kotlinx.coroutines.CancellationException].
 */
fun interface OptimizationProgressListener {

    /**
     * Called after a generation of the evolutionary search has completed.
     *
     * @param generation The 1-based index of the completed generation.
     */
    suspend fun onGenerationComplete(generation: Int)
}
