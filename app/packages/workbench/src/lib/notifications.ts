import { toast } from "vue-sonner";

/**
 * Duration in milliseconds for toast notifications
 */
const DEFAULT_DURATION = 4000;

/**
 * Options for toast notifications
 */
export interface NotificationOptions {
    /**
     * Optional description to show below the title
     */
    description?: string;
    /**
     * Duration in milliseconds before auto-dismissal
     */
    duration?: number;
}

/**
 * Shows a success notification toast
 *
 * @param message The main message to display
 * @param options Optional configuration for the notification
 */
export function showSuccess(message: string, options?: NotificationOptions): void {
    toast.success(message, {
        description: options?.description,
        duration: options?.duration ?? DEFAULT_DURATION
    });
}

/**
 * Shows an error notification toast
 *
 * @param message The main message to display
 * @param options Optional configuration for the notification
 */
export function showError(message: string, options?: NotificationOptions): void {
    toast.error(message, {
        description: options?.description,
        duration: options?.duration ?? DEFAULT_DURATION
    });
}

/**
 * Shows a warning notification toast
 *
 * @param message The main message to display
 * @param options Optional configuration for the notification
 */
export function showWarning(message: string, options?: NotificationOptions): void {
    toast.warning(message, {
        description: options?.description,
        duration: options?.duration ?? DEFAULT_DURATION
    });
}

/**
 * Shows an info notification toast
 *
 * @param message The main message to display
 * @param options Optional configuration for the notification
 */
export function showInfo(message: string, options?: NotificationOptions): void {
    toast.info(message, {
        description: options?.description,
        duration: options?.duration ?? DEFAULT_DURATION
    });
}

/**
 * Shows an error notification for a failed API operation
 *
 * @param operation Description of the operation that failed
 * @param errorMessage The error message from the API
 */
export function showApiError(operation: string, errorMessage?: string): void {
    showError(`Failed to ${operation}`, {
        description: errorMessage
    });
}
