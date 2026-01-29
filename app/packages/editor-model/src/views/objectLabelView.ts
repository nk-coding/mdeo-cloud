import type { GLabel } from "@mdeo/editor-shared";
import { GLabelView, sharedImport } from "@mdeo/editor-shared";

const { injectable } = sharedImport("inversify");

/**
 * View for rendering Object labels in model diagrams.
 * Renders the combined object name and type with underline styling (UML convention for instances).
 * Format: "name : type"
 */
@injectable()
export class GObjectLabelView extends GLabelView {
    /**
     * Gets the CSS classes to apply to the object label.
     *
     * @param model - The label model being rendered
     * @returns A record of CSS class names to boolean values
     */
    protected override getClasses(model: Readonly<GLabel>): Record<string, boolean> {
        return {
            ...super.getClasses(model),
            "text-center": true,
            underline: true
        };
    }
}
