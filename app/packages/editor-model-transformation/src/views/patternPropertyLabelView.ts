import { GLabelView, sharedImport } from "@mdeo/editor-shared";
import type { GLabel } from "@mdeo/editor-shared";

const { injectable } = sharedImport("inversify");

/**
 * View for rendering pattern property labels in model transformation diagrams.
 * Uses HTML since the pattern instance node is now rendered as a foreign-object HTML box.
 */
@injectable()
export class GPatternPropertyLabelView extends GLabelView {
    protected override getClasses(model: Readonly<GLabel>): Record<string, boolean> {
        return {
            ...super.getClasses(model),
            "text-sm": true
        };
    }
}
