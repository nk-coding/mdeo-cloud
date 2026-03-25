import type { LayoutOperation } from "@mdeo/protocol-common";
import type { ContextActionRequestContext } from "../../context-actions/contextActionRequestContext.js";
import type { ContextItemProvider } from "../../context-actions/contextItemProvider.js";
import type { ContextItem } from "@mdeo/protocol-common";
import { ResetLayoutOperation } from "@mdeo/protocol-common";
import { sharedImport } from "../../sharedImport.js";
import { BaseOperationHandler } from "./baseOperationHandler.js";
import type { Command, DiagramConfiguration, GModelElement } from "@eclipse-glsp/server";
import { BaseLayoutEngine } from "../layoutEngine.js";
import { OperationHandlerCommand } from "./operationHandlerCommand.js";

const { injectable, inject } = sharedImport("inversify");
const { DiagramConfiguration: DiagramConfigurationKey } = sharedImport("@eclipse-glsp/server");

/**
 * Handler for layout operations.
 */
@injectable()
export class LayoutOperationHandler extends BaseOperationHandler implements ContextItemProvider {
    override readonly operationType = "layout" satisfies LayoutOperation["kind"];

    /**
     * The layout engine used to perform diagram layouting
     */
    @inject(BaseLayoutEngine)
    protected readonly layoutEngine!: BaseLayoutEngine;

    /**
     * Diagram configuration used to look up shape type hints.
     */
    @inject(DiagramConfigurationKey)
    protected readonly diagramConfiguration!: DiagramConfiguration;

    /**
     * Returns a reset-layout context item only for elements whose shape type hint
     * marks them as resizable. Edges and fixed-size nodes are excluded to avoid
     * context-menu visual overload.
     *
     * @param element The selected element
     * @param _context Request context
     * @returns A single reset-layout context item when the element is resizable,
     *   otherwise an empty array
     */
    getContextItems(element: GModelElement, _context: ContextActionRequestContext): ContextItem[] {
        const shapeHint = this.diagramConfiguration.shapeTypeHints.find((hint) => hint.elementTypeId === element.type);
        if (shapeHint == undefined || !shapeHint.resizable) {
            return [];
        }

        return [
            {
                id: `reset-layout-${element.id}`,
                label: "Reset Layout",
                icon: "rotate-ccw",
                sortString: "x",
                action: ResetLayoutOperation.create({ elementId: element.id })
            }
        ];
    }

    override async createCommand(operation: LayoutOperation): Promise<Command> {
        return new OperationHandlerCommand(this.modelState, undefined, await this.layoutEngine.layout(operation));
    }
}
