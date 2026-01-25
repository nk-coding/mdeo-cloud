import type { GLabel } from "@mdeo/editor-shared";
import { sharedImport, GLabelView } from "@mdeo/editor-shared";

const { injectable } = sharedImport("inversify");

/**
 * View for rendering enum name labels.
 * Displays the enum name with a stereotype indicator.
 */
@injectable()
export class GEnumLabelView extends GLabelView {
    protected override getClasses(model: Readonly<GLabel>): Record<string, boolean> {
        return {
            ...super.getClasses(model),
            "font-bold": true,
            "text-center": true
        };
    }
}
