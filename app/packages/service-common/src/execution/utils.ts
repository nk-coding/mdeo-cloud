import { ExecutionState, type ExecutionEvent, type ExecutionEventType } from "./types.js";

/**
 * Checks if an execution state is terminal (completed, cancelled, or failed).
 *
 * @param state The execution state to check
 * @returns True if the state is terminal
 */
export function isTerminalState(state: ExecutionState): boolean {
    return (
        state === ExecutionState.COMPLETED ||
        state === ExecutionState.CANCELLED ||
        state === ExecutionState.FAILED
    );
}

/**
 * Checks if an execution state is active (submitted, initializing, or running).
 *
 * @param state The execution state to check
 * @returns True if the state is active
 */
export function isActiveState(state: ExecutionState): boolean {
    return (
        state === ExecutionState.SUBMITTED ||
        state === ExecutionState.INITIALIZING ||
        state === ExecutionState.RUNNING
    );
}

/**
 * Validates if a state transition is valid.
 * Prevents transitions from terminal states and enforces logical progression.
 *
 * @param fromState The current state
 * @param toState The target state
 * @returns True if the transition is valid
 */
export function isValidStateTransition(fromState: ExecutionState, toState: ExecutionState): boolean {
    // Cannot transition from terminal states
    if (isTerminalState(fromState)) {
        return false;
    }

    // Can always transition to terminal states
    if (isTerminalState(toState)) {
        return true;
    }

    // Define valid non-terminal transitions
    const validTransitions: Record<string, ExecutionState[]> = {
        submitted: [ExecutionState.INITIALIZING, ExecutionState.RUNNING],
        initializing: [ExecutionState.RUNNING],
        running: [],
        completed: [],
        cancelled: [],
        failed: []
    };

    return validTransitions[fromState]?.includes(toState) ?? false;
}

/**
 * Creates an execution event with the current timestamp.
 *
 * @param type The event type
 * @param executionId The execution ID
 * @param options Optional event data
 * @returns The execution event
 */
export function createExecutionEvent(
    type: ExecutionEventType,
    executionId: string,
    options?: {
        state?: ExecutionState;
        progressText?: string;
        error?: string;
        timestamp?: string;
    }
): ExecutionEvent {
    return {
        type,
        executionId,
        state: options?.state,
        progressText: options?.progressText,
        error: options?.error,
        timestamp: options?.timestamp ?? new Date().toISOString()
    };
}

/**
 * Parses an execution state string into the ExecutionState enum.
 * Returns undefined for invalid states.
 *
 * @param state The state string to parse
 * @returns The ExecutionState if valid, undefined otherwise
 */
export function parseExecutionState(state: string): ExecutionState | undefined {
    const validStates: ExecutionState[] = [
        ExecutionState.SUBMITTED,
        ExecutionState.INITIALIZING,
        ExecutionState.RUNNING,
        ExecutionState.COMPLETED,
        ExecutionState.CANCELLED,
        ExecutionState.FAILED
    ];
    return validStates.includes(state as ExecutionState) ? (state as ExecutionState) : undefined;
}

/**
 * Calculates the duration of an execution in milliseconds.
 * Returns undefined if the execution hasn't started or finished.
 *
 * @param startedAt ISO 8601 timestamp when execution started
 * @param finishedAt ISO 8601 timestamp when execution finished
 * @returns Duration in milliseconds, or undefined if timestamps are invalid
 */
export function calculateExecutionDuration(startedAt?: string, finishedAt?: string): number | undefined {
    if (!startedAt || !finishedAt) {
        return undefined;
    }

    try {
        const start = new Date(startedAt);
        const end = new Date(finishedAt);
        return end.getTime() - start.getTime();
    } catch {
        return undefined;
    }
}

/**
 * Formats an execution duration in a human-readable format.
 *
 * @param durationMs Duration in milliseconds
 * @returns Formatted duration string (e.g., "1m 30s", "45s", "2.5s")
 */
export function formatExecutionDuration(durationMs: number): string {
    const seconds = durationMs / 1000;
    
    if (seconds < 1) {
        return `${Math.round(durationMs)}ms`;
    }
    
    if (seconds < 60) {
        return `${seconds.toFixed(1)}s`;
    }
    
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = Math.floor(seconds % 60);
    
    if (minutes < 60) {
        return `${minutes}m ${remainingSeconds}s`;
    }
    
    const hours = Math.floor(minutes / 60);
    const remainingMinutes = minutes % 60;
    return `${hours}h ${remainingMinutes}m`;
}
