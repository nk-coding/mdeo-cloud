import type { FileDataHandler } from "@mdeo/service-common";
import { astHandler } from "./astHandler.js";

/**
 * Handlers for different file data keys.
 * Maps data keys to their corresponding handler functions.
 */
export const handlers: Record<string, FileDataHandler> = {
    ast: astHandler
};
