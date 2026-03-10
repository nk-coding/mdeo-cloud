import { GLabelView, sharedImport } from "@mdeo/editor-shared";
import type { GLabel } from "@mdeo/editor-shared";

const { injectable } = sharedImport("inversify");

/**
 * View for rendering pattern instance name labels in model transformation diagrams.
 * Renders the combined instance name and optional type with underline styling
 * (UML convention for instances). Uses HTML since the pattern instance node is
 * now rendered as a foreign-object HTML box.
 */
@injectable()
export class GPatternInstanceLabelView extends GLabelView {
    protected override getClasses(model: Readonly<GLabel>): Record<string, boolean> {
        return {
            ...super.getClasses(model),
            "text-sm": true,
            underline: true,
            "text-center": true,
            block: true
        };
    }
}
