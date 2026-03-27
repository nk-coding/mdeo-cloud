import type { CommandExecutionContext, CommandReturn } from "@eclipse-glsp/sprotty";
import { sharedImport } from "../../sharedImport.js";
import { getIssueMarker, getOrCreateIssueMarker, addSeverityCssClass } from "../../model/issueMarker.js";

const { injectable } = sharedImport("inversify");
const { GParentElement } = sharedImport("@eclipse-glsp/sprotty");
const {
    ApplyMarkersCommand: BaseApplyMarkersCommand,
    DeleteMarkersCommand: BaseDeleteMarkersCommand,
    createGIssue,
    removeCssClasses
} = sharedImport("@eclipse-glsp/client");

/**
 * Replaces {@link BaseApplyMarkersCommand} to create {@link GIssueMarker}
 * instances that do not extend GLSP's `GIssueMarker`, bypassing the
 * `instanceof` check that triggers the unsupported pre-rendered popup.
 */
@injectable()
export class ApplyMarkersCommand extends BaseApplyMarkersCommand {
    override execute(context: CommandExecutionContext): CommandReturn {
        this.action.markers.forEach((marker) => {
            const modelElement = context.root.index.getById(marker.elementId);
            if (modelElement instanceof GParentElement) {
                const issueMarker = getOrCreateIssueMarker(modelElement);
                issueMarker.issues.push(createGIssue(marker));
                addSeverityCssClass(modelElement, issueMarker);
            }
        });
        return context.root;
    }
}

/**
 * Replaces {@link BaseDeleteMarkersCommand} to locate {@link GIssueMarker}
 * instances that are not found by the default `getGIssueMarker` helper
 * since they do not extend GLSP's `GIssueMarker`.
 */
@injectable()
export class DeleteMarkersCommand extends BaseDeleteMarkersCommand {
    override execute(context: CommandExecutionContext): CommandReturn {
        this.action.markers.forEach((marker) => {
            const modelElement = context.root.index.getById(marker.elementId);
            if (modelElement instanceof GParentElement) {
                const issueMarker = getIssueMarker(modelElement);
                if (issueMarker) {
                    removeCssClasses(modelElement, "info", "warning", "error");
                    for (let i = 0; i < issueMarker.issues.length; i++) {
                        if (issueMarker.issues[i].message === marker.description) {
                            issueMarker.issues.splice(i--, 1);
                        }
                    }
                    if (issueMarker.issues.length === 0) {
                        modelElement.remove(issueMarker);
                    } else {
                        addSeverityCssClass(modelElement, issueMarker);
                    }
                }
            }
        });
        return context.root;
    }
}
