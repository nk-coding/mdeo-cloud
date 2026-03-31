import type { IconNode } from "lucide";
import { sharedImport, DefaultIconRegistry } from "@mdeo/editor-shared";
import { VariablePlus } from "./customIcons.js";

const { injectable } = sharedImport("inversify");

/**
 * Icon registry for the model transformation editor.
 *
 * Extends the default lucide-backed registry with the custom SVG icons
 * used by the model transformation diagram.
 *
 * Icon names are resolved in the following order:
 * 1. Built-in model transformation icons matched by name.
 * 2. Standard lucide icons via the parent implementation.
 *
 * The registered custom icon names are:
 * - `"variable-plus"` — plus sign, used to indicate a variable that can hold multiple values (e.g., a collection).
 */
@injectable()
export class ModelTransformationIconRegistry extends DefaultIconRegistry {
    protected override getIconNode(iconName: string): IconNode | undefined {
        switch (iconName) {
            case "variable-plus":
                return VariablePlus;
            default:
                return super.getIconNode(iconName);
        }
    }
}
