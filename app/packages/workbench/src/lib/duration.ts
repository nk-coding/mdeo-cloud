/**
 * Formats a duration in milliseconds to a human-readable string.
 *
 * The format is chosen based on the duration:
 * - Less than 1 second: shows milliseconds (e.g., "450ms")
 * - Less than 1 minute: shows seconds with one decimal (e.g., "23.5s")
 * - Less than 1 hour: shows minutes and seconds (e.g., "5m 30s")
 * - 1 hour or more: shows hours and minutes (e.g., "2h 15m")
 *
 * @param ms The duration in milliseconds
 * @returns A formatted string representing the duration
 */
export function formatDuration(ms: number): string {
    if (ms < 0) {
        return "0ms";
    }

    const seconds = ms / 1000;
    const minutes = seconds / 60;
    const hours = minutes / 60;

    if (ms < 1000) {
        return `${Math.round(ms)}ms`;
    }

    if (seconds < 60) {
        return `${seconds.toFixed(1)}s`;
    }

    if (minutes < 60) {
        const wholeMinutes = Math.floor(minutes);
        const remainingSeconds = Math.round(seconds % 60);
        return remainingSeconds > 0 ? `${wholeMinutes}m ${remainingSeconds}s` : `${wholeMinutes}m`;
    }

    const wholeHours = Math.floor(hours);
    const remainingMinutes = Math.round(minutes % 60);
    return remainingMinutes > 0 ? `${wholeHours}h ${remainingMinutes}m` : `${wholeHours}h`;
}

/**
 * Calculates the duration between two ISO 8601 timestamps.
 *
 * @param startedAt The start timestamp in ISO 8601 format
 * @param finishedAt The end timestamp in ISO 8601 format
 * @returns The duration in milliseconds, or null if either timestamp is invalid
 */
export function calculateDuration(startedAt: string | null, finishedAt: string | null): number | null {
    if (startedAt == null || finishedAt == null) {
        return null;
    }

    const start = new Date(startedAt).getTime();
    const end = new Date(finishedAt).getTime();

    if (isNaN(start) || isNaN(end)) {
        return null;
    }

    return end - start;
}
