import type {
    Action,
    CommandExecutionContext,
    CommandResult,
    Dimension,
    ElementAndBounds,
    GModelElement,
    GModelRoot,
    Point,
    RequestBoundsAction
} from "@eclipse-glsp/sprotty";
import { sharedImport } from "@mdeo/editor-shared";

const { injectable, inject } = sharedImport("inversify");
const {
    HiddenCommand,
    TYPES,
    RequestBoundsCommand: SprottyRequestBoundsCommand
} = sharedImport("@eclipse-glsp/sprotty");
const { ComputedBoundsAction } = sharedImport("@eclipse-glsp/protocol");

/**
 * An action to trigger an iterative request bounds process, which allows for multiple rounds of bounds computation.
 */
export interface IterativeRequestBoundsAction extends Action {
    kind: typeof IterativeRequestBoundsAction.KIND;
    /**
     * The original request bounds action
     */
    cause: RequestBoundsAction;
    /**
     * The bounds from the previous iteration
     */
    previousBounds: ElementAndBounds[];
}

export namespace IterativeRequestBoundsAction {
    export const KIND = "iterativeRequestBounds";

    /**
     * Type guard to check if an action is an IterativeRequestBoundsAction
     *
     * @param action the action to check
     * @returns true if the action is an IterativeRequestBoundsAction, false otherwise
     */
    export function is(action: Action): action is IterativeRequestBoundsAction {
        return action.kind === KIND;
    }

    /**
     * Factory function to create an IterativeRequestBoundsAction
     *
     * @param cause the original RequestBoundsAction that triggered the iterative process
     * @param previousBounds the bounds from the previous iteration, which can be used in subsequent iterations to determine if further updates are needed
     * @returns a new IterativeRequestBoundsAction instance with the provided cause and previous bounds
     */
    export function create(
        cause: RequestBoundsAction,
        previousBounds: ElementAndBounds[]
    ): IterativeRequestBoundsAction {
        return {
            kind: KIND,
            cause,
            previousBounds
        };
    }
}

/**
 * A command that executes an iterative request bounds process, which allows for multiple rounds of bounds computation.
 */
@injectable()
export class IterativeRequestBoundsCommand extends HiddenCommand {
    static readonly KIND = IterativeRequestBoundsAction.KIND;

    constructor(@inject(TYPES.Action) protected action: IterativeRequestBoundsAction) {
        super();
    }

    override execute(context: CommandExecutionContext): GModelRoot | CommandResult {
        const root = context.modelFactory.createRoot(this.action.cause.newRoot);
        const index = root.index;
        for (const boundsData of this.action.previousBounds) {
            const element = index.getById(boundsData.elementId);
            if (element != undefined) {
                if (boundsData.newPosition != undefined) {
                    (element as GModelElement & { position: Point }).position = { ...boundsData.newPosition };
                }
                if (boundsData.newSize != undefined) {
                    (element as GModelElement & { size: Dimension }).size = { ...boundsData.newSize };
                }
            }
        }
        return {
            model: root,
            modelChanged: true,
            cause: this.action
        };
    }

    get blockUntil(): (action: Action) => boolean {
        return (action) => action.kind === ComputedBoundsAction.KIND;
    }
}

/**
 * Custom RequestBoundsCommand that blocks until the iterative bounds computation is complete
 */
@injectable()
export class RequestBoundsCommand extends SprottyRequestBoundsCommand {
    override get blockUntil(): (action: Action) => boolean {
        return (action) => action.kind === IterativeRequestBoundsAction.KIND;
    }
}
