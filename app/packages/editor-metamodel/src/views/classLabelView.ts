import type { GLabel } from "@mdeo/editor-shared";
import { GLabelView, sharedImport } from "@mdeo/editor-shared";

const { injectable } = sharedImport("inversify");

/**
 * View for rendering Class name labels in metamodel diagrams.
 * Extends HtmlLabelView to provide bold and center-aligned styling.
 */
@injectable()
export class GClassLabelView extends GLabelView {
    protected override getClasses(model: Readonly<GLabel>): Record<string, boolean> {
        return {
            ...super.getClasses(model),
            "font-bold": true,
            "text-center": true
        };
    }
}
