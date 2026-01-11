import type { IconNode } from "lucide";
import type { IconNode as VueIconNode } from "lucide-vue-next";

/**
 * Converts a Lucide IconNode to a lucide-vue-next IconNode by ensuring all attribute values are strings.
 * Most likely not necessary, but safer this way.
 *
 * @param icon The Lucide IconNode to convert
 * @returns The converted lucide-vue-next IconNode
 */
export function convertIcon(icon: IconNode): VueIconNode {
    return icon.map((entry) => {
        return [
            entry[0],
            Object.fromEntries(
                Object.entries(entry[1]).map(([key, value]) => {
                    return [key, value.toString()];
                })
            )
        ];
    });
}
