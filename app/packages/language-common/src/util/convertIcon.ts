import type { IconNode } from "lucide";
import type { ActionIconNode } from "../protocol/action.js";

/**
 * Converts a Lucide IconNode to an ActionIconNode.
 * ActionIconNode is a simplified representation compatible with serialization.
 * This ensures all attribute values are strings, making the icon data
 * safe for JSON serialization and compatible with various icon rendering implementations.
 *
 * @param icon The Lucide IconNode to convert
 * @returns The converted ActionIconNode with all attributes as strings
 */
export function convertIcon(icon: IconNode): ActionIconNode {
    return icon.map((entry) => {
        return [
            entry[0],
            Object.fromEntries(
                Object.entries(entry[1])
                    .filter(([_key, value]) => value != undefined)
                    .map(([key, value]) => {
                        return [key, value!.toString()];
                    })
            )
        ];
    });
}
