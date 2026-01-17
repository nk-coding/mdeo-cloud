import { sharedImport } from "../../sharedImport.js";
import type { GEdge } from "../../model/edge.js";
import { GNode } from "../../model/node.js";
import type { EdgeMemento } from "./edgeMemento.js";
import { EdgeRouter } from "../edge-rourting/edgeRouter.js";
import type { Point, MoveAction } from "@eclipse-glsp/protocol";
import type {
    CommandExecutionContext,
    GModelElement,
    GModelRoot,
    ICommand,
    Locateable,
    CommandReturn,
    Animation as AnimationType,
    ResolvedElementMove,
    ElementMove
} from "@eclipse-glsp/sprotty";
import { MorphEdgesAnimation } from "./morphEdgesAnimation.js";

const { injectable, inject } = sharedImport("inversify");
const { MergeableCommand, MoveAnimation, CompoundAnimation, isLocateable } = sharedImport("@eclipse-glsp/sprotty");
const { Point: PointUtil } = sharedImport("@eclipse-glsp/protocol");
const { TYPES } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Command for moving elements in the diagram.
 * Handles both element positioning and edge routing updates.
 */
@injectable()
export class MoveCommand extends MergeableCommand {
    static readonly KIND = "move";

    @inject(EdgeRouter) protected edgeRouter!: EdgeRouter;

    protected resolvedMoves: Map<string, ResolvedElementMove> = new Map();
    protected edgeMementi: EdgeMemento[] = [];
    protected animation: AnimationType | undefined;

    constructor(@inject(TYPES.Action) protected readonly action: MoveAction) {
        super();
    }

    execute(context: CommandExecutionContext): CommandReturn {
        const attachedEdgeShifts = new Map<GEdge, Point>();

        this.action.moves.forEach((move) => {
            const element = context.root.index.getById(move.elementId);
            if (element && isLocateable(element)) {
                const resolvedMove = this.resolveElementMove(element, move);
                if (resolvedMove) {
                    this.resolvedMoves.set(resolvedMove.element.id, resolvedMove);
                    this.handleAttachedEdges(element, resolvedMove, attachedEdgeShifts);
                }
            }
        });

        this.doMove(attachedEdgeShifts);

        if (this.action.animate) {
            this.undoMove();
            this.animation = new CompoundAnimation(context.root, context, [
                new MoveAnimation(context.root, this.resolvedMoves, context, false),
                new MorphEdgesAnimation(context.root, this.edgeMementi, context, this.edgeRouter, false)
            ]);
            return this.animation.start();
        }

        return context.root;
    }

    /**
     * Resolves an element move from the action data.
     */
    protected resolveElementMove(
        element: GModelElement & Locateable,
        move: ElementMove
    ): ResolvedElementMove | undefined {
        const fromPosition = move.fromPosition || { ...element.position };
        return {
            element,
            fromPosition,
            toPosition: move.toPosition
        };
    }

    /**
     * Handles collecting attached edges for a moved element.
     */
    protected handleAttachedEdges(
        element: GModelElement,
        resolvedMove: ResolvedElementMove,
        attachedEdgeShifts: Map<GEdge, Point>
    ): void {
        const attachedEdges = this.getAttachedEdges(element);

        attachedEdges.forEach((edge) => {
            if (!this.isChildOfMovedElements(edge)) {
                const existingDelta = attachedEdgeShifts.get(edge);
                const newDelta = PointUtil.subtract(resolvedMove.toPosition, resolvedMove.fromPosition);
                const delta = existingDelta ? PointUtil.linear(existingDelta, newDelta, 0.5) : newDelta;
                attachedEdgeShifts.set(edge, delta);
            }
        });
    }

    /**
     * Gets all edges attached to an element (including children).
     */
    protected getAttachedEdges(element: GModelElement): GEdge[] {
        const edges: GEdge[] = [];

        const collectEdgesForNode = (node: GNode) => {
            edges.push(...node.incomingEdges());
            edges.push(...node.outgoingEdges());
        };

        if (element instanceof GNode) {
            collectEdgesForNode(element);
        }

        if ("children" in element && Array.isArray(element.children)) {
            element.children.forEach((child) => {
                if (child instanceof GNode) {
                    collectEdgesForNode(child);
                }
                edges.push(...this.getAttachedEdges(child));
            });
        }

        return edges;
    }

    /**
     * Performs the actual move operations.
     */
    protected doMove(attachedEdgeShifts: Map<GEdge, Point>): void {
        this.resolvedMoves.forEach((res) => {
            res.element.position = res.toPosition;
        });

        attachedEdgeShifts.forEach((delta, edge) => {
            const before = this.edgeRouter.computeRoute(edge);

            if (this.isAttachedEdge(edge)) {
                edge.meta.routingPoints = edge.meta.routingPoints.map((rp) => PointUtil.add(rp, delta));
            }

            const after = this.edgeRouter.computeRoute(edge);
            this.edgeMementi.push({ edge, before, after });
        });
    }

    /**
     * Checks if an element is a child of any moved elements.
     */
    protected isChildOfMovedElements(element: GModelElement): boolean {
        let current: GModelElement | undefined = element;
        while (current && "parent" in current) {
            current = current.parent as GModelElement | undefined;
            if (current && this.resolvedMoves.has(current.id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if an edge is attached to moved elements (both source and target are moved).
     */
    protected isAttachedEdge(edge: GEdge): boolean {
        const source = edge.index.getById(edge.sourceId);
        const target = edge.index.getById(edge.targetId);

        const checkMovedElementsAndChildren = (element: GModelElement | undefined): boolean => {
            if (!element) return false;
            return this.resolvedMoves.has(element.id) || this.isChildOfMovedElements(element);
        };

        return checkMovedElementsAndChildren(source) && checkMovedElementsAndChildren(target);
    }

    /**
     * Reverts the move operations.
     */
    protected undoMove(): void {
        this.resolvedMoves.forEach((res) => {
            res.element.position = res.fromPosition;
        });
        this.edgeMementi.forEach((memento) => {
            memento.edge.meta = memento.before.meta;
        });
    }

    undo(context: CommandExecutionContext): Promise<GModelRoot> {
        return new CompoundAnimation(context.root, context, [
            new MoveAnimation(context.root, this.resolvedMoves, context, true),
            new MorphEdgesAnimation(context.root, this.edgeMementi, context, this.edgeRouter, true)
        ]).start();
    }

    redo(context: CommandExecutionContext): Promise<GModelRoot> {
        return new CompoundAnimation(context.root, context, [
            new MoveAnimation(context.root, this.resolvedMoves, context, false),
            new MorphEdgesAnimation(context.root, this.edgeMementi, context, this.edgeRouter, false)
        ]).start();
    }

    override merge(other: ICommand): boolean {
        if (!this.action.animate && other instanceof MoveCommand) {
            other.resolvedMoves.forEach((otherMove, otherElementId) => {
                const existingMove = this.resolvedMoves.get(otherElementId);
                if (existingMove) {
                    existingMove.toPosition = otherMove.toPosition;
                } else {
                    this.resolvedMoves.set(otherElementId, otherMove);
                }
            });

            other.edgeMementi.forEach((otherMemento) => {
                const existingMemento = this.edgeMementi.find(
                    (edgeMemento) => edgeMemento.edge.id === otherMemento.edge.id
                );
                if (existingMemento) {
                    existingMemento.after = otherMemento.after;
                } else {
                    this.edgeMementi.push(otherMemento);
                }
            });
            return true;
        }
        return false;
    }
}
