import { BaseOperationHandler, OperationHandlerCommand, sharedImport } from "@mdeo/language-shared";
import type { Command, GModelElement } from "@eclipse-glsp/server";
import {
    MovePatternElementToConditionOperation,
    ModelTransformationElementType,
    MAIN_PATTERN_TARGET,
    NEW_FORBID_TARGET,
    NEW_REQUIRE_TARGET
} from "@mdeo/protocol-model-transformation";
import type { ContextActionRequestContext, ContextItemProvider } from "@mdeo/language-shared";
import type { ContextItem } from "@mdeo/protocol-common";
import type { AstNode } from "langium";
import type { WorkspaceEdit } from "vscode-languageserver-types";
import {
    Pattern,
    PatternApplicationCondition,
    PatternLink,
    PatternObjectInstance,
    PatternObjectInstanceReference,
    type PatternApplicationConditionType,
    type PatternLinkType,
    type PatternObjectInstanceType,
    type PatternType
} from "../../../grammar/modelTransformationTypes.js";

const { injectable } = sharedImport("inversify");
const { AstUtils, GrammarUtils } = sharedImport("langium");

/**
 * Handler that moves a pattern element into another application condition block, into a new
 * block, or back into the enclosing match pattern.
 *
 * Because a block is a graph of its own, an element never travels alone: the whole connected
 * component it belongs to inside its current container is moved with it. Moving a node out of
 * a block while leaving its links behind would otherwise split one graph into two halves that
 * can no longer refer to each other.
 */
@injectable()
export class MovePatternElementToConditionOperationHandler extends BaseOperationHandler implements ContextItemProvider {
    override readonly operationType = "movePatternElementToCondition";

    /**
     * Creates the command that performs the move.
     *
     * The moved elements are cut from their current container and re-inserted verbatim into
     * the destination, so that formatting, comments and property constraints survive the move.
     *
     * @param operation The requested move
     * @returns A command applying the workspace edit, or `undefined` when the element cannot
     *          be resolved or the move would leave a dangling reference behind
     */
    override async createCommand(operation: MovePatternElementToConditionOperation): Promise<Command | undefined> {
        const element = this.modelState.index.find(operation.elementId);
        if (element == undefined) {
            return undefined;
        }
        const astNode = this.index.getAstNode(element);
        if (astNode == undefined) {
            return undefined;
        }

        const pattern = this.findPattern(astNode);
        const container = this.findContainer(astNode);
        if (pattern == undefined || container == undefined) {
            return undefined;
        }

        const component = this.collectComponent(astNode, container);
        if (component.length === 0 || !this.isSelfContained(component)) {
            return undefined;
        }

        const target = this.resolveTarget(pattern, container, operation.target);
        if (target === undefined) {
            return undefined;
        }

        const movedText = component
            .map((node) => node.$cstNode?.text)
            .filter((text): text is string => text != undefined && text.length > 0);
        if (movedText.length !== component.length) {
            return undefined;
        }

        const edits: WorkspaceEdit[] = component
            .map((node) => (node.$cstNode != undefined ? this.deleteCstNode(node.$cstNode) : undefined))
            .filter((edit): edit is WorkspaceEdit => edit != undefined);

        const insertion =
            target.kind === "new"
                ? this.insertNewBlock(pattern, target.conditionKind, movedText)
                : this.insertIntoExisting(target.container, movedText);
        if (insertion == undefined) {
            return undefined;
        }
        edits.push(insertion);

        return new OperationHandlerCommand(this.modelState, this.mergeWorkspaceEdits(edits), undefined);
    }

    /**
     * Offers the move targets available for the selected element.
     *
     * The action is offered for locally declared instances and for links, and only when the
     * whole component can move without breaking a reference.
     *
     * @param element The selected element
     * @param _context Additional request context
     * @returns The context items for this handler, or an empty array when not applicable
     */
    getContextItems(element: GModelElement, _context: ContextActionRequestContext): ContextItem[] {
        if (
            element.type !== ModelTransformationElementType.NODE_PATTERN_INSTANCE &&
            element.type !== ModelTransformationElementType.EDGE_PATTERN_LINK
        ) {
            return [];
        }

        const astNode = this.index.getAstNode(element);
        if (
            astNode == undefined ||
            (!this.reflection.isInstance(astNode, PatternObjectInstance) &&
                !this.reflection.isInstance(astNode, PatternLink) &&
                !this.reflection.isInstance(astNode, PatternObjectInstanceReference))
        ) {
            return [];
        }

        const pattern = this.findPattern(astNode);
        const container = this.findContainer(astNode);
        if (pattern == undefined || container == undefined) {
            return [];
        }

        const component = this.collectComponent(astNode, container);
        if (component.length === 0 || !this.isSelfContained(component)) {
            return [];
        }

        const currentCondition = this.reflection.isInstance(container, PatternApplicationCondition)
            ? (container as PatternApplicationConditionType)
            : undefined;

        const children: ContextItem[] = [];

        if (currentCondition != undefined) {
            children.push({
                id: `move-to-block-${element.id}-main`,
                label: "Match Pattern",
                icon: "square",
                action: MovePatternElementToConditionOperation.create({
                    elementId: element.id,
                    target: MAIN_PATTERN_TARGET
                })
            });
        }

        const conditions = this.getConditions(pattern);
        for (const [index, condition] of conditions.entries()) {
            if (condition === currentCondition) {
                continue;
            }
            children.push({
                id: `move-to-block-${element.id}-${index}`,
                label: this.describeCondition(condition, index),
                icon: condition.kind === "require" ? "square-check" : "square-slash",
                action: MovePatternElementToConditionOperation.create({
                    elementId: element.id,
                    target: `${index}`
                })
            });
        }

        children.push({
            id: `move-to-block-${element.id}-new-forbid`,
            label: "New Forbid Block",
            icon: "square-slash",
            action: MovePatternElementToConditionOperation.create({
                elementId: element.id,
                target: NEW_FORBID_TARGET
            })
        });
        children.push({
            id: `move-to-block-${element.id}-new-require`,
            label: "New Require Block",
            icon: "square-check",
            action: MovePatternElementToConditionOperation.create({
                elementId: element.id,
                target: NEW_REQUIRE_TARGET
            })
        });

        return [
            {
                id: `move-to-block-${element.id}`,
                label: "Move to Block",
                icon: "square-arrow-right",
                sortString: "cb",
                children
            }
        ];
    }

    /**
     * Returns the application condition blocks of a pattern, in declaration order.
     *
     * @param pattern The pattern to inspect
     * @returns The condition blocks
     */
    private getConditions(pattern: PatternType): PatternApplicationConditionType[] {
        return (pattern.elements ?? []).filter((element) =>
            this.reflection.isInstance(element, PatternApplicationCondition)
        ) as PatternApplicationConditionType[];
    }

    /**
     * Builds the label shown for a destination block.
     *
     * @param condition The condition block
     * @param index Its index within the pattern, used when the block is unnamed
     * @returns The label for the context item
     */
    private describeCondition(condition: PatternApplicationConditionType, index: number): string {
        const kind = condition.kind === "require" ? "Require" : "Forbid";
        return condition.name != undefined ? `${kind} ${condition.name}` : `${kind} #${index + 1}`;
    }

    /**
     * Returns the pattern a node belongs to.
     *
     * @param node The node to start from
     * @returns The enclosing pattern, or `undefined`
     */
    private findPattern(node: AstNode): PatternType | undefined {
        let current: AstNode | undefined = node.$container;
        while (current != undefined) {
            if (this.reflection.isInstance(current, Pattern)) {
                return current as PatternType;
            }
            current = current.$container;
        }
        return undefined;
    }

    /**
     * Returns the direct container of a pattern element: either the pattern itself or the
     * condition block it is declared in.
     *
     * @param node The element
     * @returns The container, or `undefined` when the node is not a pattern element
     */
    private findContainer(node: AstNode): PatternType | PatternApplicationConditionType | undefined {
        const container = node.$container;
        if (container == undefined) {
            return undefined;
        }
        if (
            this.reflection.isInstance(container, Pattern) ||
            this.reflection.isInstance(container, PatternApplicationCondition)
        ) {
            return container as PatternType | PatternApplicationConditionType;
        }
        return undefined;
    }

    /**
     * Collects the connected component of [start] inside its container.
     *
     * Instances are connected when a link of the same container joins them; a link belongs to
     * the component of its endpoints. A link whose endpoints both live outside the container
     * (an anchor-only link) forms a component on its own.
     *
     * @param start The selected element
     * @param container The container the element is declared in
     * @returns The elements to move, in declaration order
     */
    private collectComponent(start: AstNode, container: PatternType | PatternApplicationConditionType): AstNode[] {
        const elements = (container.elements ?? []) as AstNode[];
        const localInstances = new Set<PatternObjectInstanceType>();
        for (const element of elements) {
            if (this.reflection.isInstance(element, PatternObjectInstance)) {
                localInstances.add(element as PatternObjectInstanceType);
            }
        }

        const reachedInstances = new Set<PatternObjectInstanceType>();
        const reachedLinks = new Set<PatternLinkType>();

        if (this.reflection.isInstance(start, PatternObjectInstance)) {
            reachedInstances.add(start as PatternObjectInstanceType);
        } else if (this.reflection.isInstance(start, PatternLink)) {
            const link = start as PatternLinkType;
            reachedLinks.add(link);
            for (const endpoint of this.endpointsOf(link)) {
                if (localInstances.has(endpoint)) {
                    reachedInstances.add(endpoint);
                }
            }
        } else {
            // A reference only constrains a node of the enclosing pattern; it moves alone.
            return [start];
        }

        let changed = true;
        while (changed) {
            changed = false;
            for (const element of elements) {
                if (!this.reflection.isInstance(element, PatternLink)) {
                    continue;
                }
                const link = element as PatternLinkType;
                if (reachedLinks.has(link)) {
                    continue;
                }
                const endpoints = this.endpointsOf(link).filter((endpoint) => localInstances.has(endpoint));
                if (!endpoints.some((endpoint) => reachedInstances.has(endpoint))) {
                    continue;
                }
                reachedLinks.add(link);
                for (const endpoint of endpoints) {
                    reachedInstances.add(endpoint);
                }
                changed = true;
            }
        }

        return elements.filter(
            (element) =>
                reachedInstances.has(element as PatternObjectInstanceType) ||
                reachedLinks.has(element as PatternLinkType)
        );
    }

    /**
     * Returns the resolved endpoint instances of a link.
     *
     * @param link The link
     * @returns The resolved endpoints, skipping unresolved references
     */
    private endpointsOf(link: PatternLinkType): PatternObjectInstanceType[] {
        const endpoints: PatternObjectInstanceType[] = [];
        const source = link.source?.object?.ref as PatternObjectInstanceType | undefined;
        const target = link.target?.object?.ref as PatternObjectInstanceType | undefined;
        if (source != undefined) {
            endpoints.push(source);
        }
        if (target != undefined) {
            endpoints.push(target);
        }
        return endpoints;
    }

    /**
     * Checks that nothing outside the moved component refers to one of its instances.
     *
     * A block-local node is invisible outside its block, so a move that leaves such a
     * reference behind would turn a valid transformation into an unresolvable one. Rather
     * than producing broken source, the move is not offered at all in that case.
     *
     * @param component The elements about to be moved
     * @returns `true` when the move keeps every reference resolvable
     */
    private isSelfContained(component: AstNode[]): boolean {
        const moved = new Set(component);
        const movedInstances = component.filter((node) =>
            this.reflection.isInstance(node, PatternObjectInstance)
        ) as PatternObjectInstanceType[];
        if (movedInstances.length === 0) {
            return true;
        }

        const root = AstUtils.getDocument(component[0]).parseResult.value;
        for (const node of AstUtils.streamAllContents(root)) {
            if (moved.has(node)) {
                continue;
            }
            for (const info of AstUtils.streamReferences(node)) {
                const reference = info.reference as { ref?: AstNode };
                const target = reference.ref;
                if (
                    target != undefined &&
                    movedInstances.includes(target as PatternObjectInstanceType) &&
                    !this.isInside(node, moved)
                ) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Checks whether a node is contained in one of the moved elements.
     *
     * @param node The node to check
     * @param moved The moved elements
     * @returns `true` when the node is one of them or nested inside one
     */
    private isInside(node: AstNode, moved: Set<AstNode>): boolean {
        let current: AstNode | undefined = node;
        while (current != undefined) {
            if (moved.has(current)) {
                return true;
            }
            current = current.$container;
        }
        return false;
    }

    /**
     * Resolves the requested destination.
     *
     * @param pattern The enclosing pattern
     * @param container The container the element currently lives in
     * @param target The requested destination
     * @returns The resolved destination, or `undefined` when it does not exist or equals the
     *          current container
     */
    private resolveTarget(
        pattern: PatternType,
        container: PatternType | PatternApplicationConditionType,
        target: string
    ):
        | { kind: "existing"; container: PatternType | PatternApplicationConditionType }
        | { kind: "new"; conditionKind: "forbid" | "require" }
        | undefined {
        if (target === MAIN_PATTERN_TARGET) {
            return container === pattern ? undefined : { kind: "existing", container: pattern };
        }
        if (target === NEW_FORBID_TARGET) {
            return { kind: "new", conditionKind: "forbid" };
        }
        if (target === NEW_REQUIRE_TARGET) {
            return { kind: "new", conditionKind: "require" };
        }

        const index = Number.parseInt(target, 10);
        const conditions = this.getConditions(pattern);
        if (Number.isNaN(index) || index < 0 || index >= conditions.length) {
            return undefined;
        }
        const condition = conditions[index];
        return condition === container ? undefined : { kind: "existing", container: condition };
    }

    /**
     * Builds the edit inserting the moved elements into an existing container.
     *
     * @param container The destination pattern or block
     * @param movedText The source text of the moved elements
     * @returns The workspace edit, or `undefined` when the container has no braces
     */
    private insertIntoExisting(
        container: PatternType | PatternApplicationConditionType,
        movedText: string[]
    ): WorkspaceEdit | undefined {
        const cstNode = container.$cstNode;
        if (cstNode == undefined) {
            return undefined;
        }
        const openBrace = GrammarUtils.findNodeForKeyword(cstNode, "{");
        const closeBrace = GrammarUtils.findNodeForKeyword(cstNode, "}");
        if (openBrace == undefined || closeBrace == undefined) {
            return undefined;
        }
        return this.insertIntoScope(openBrace, closeBrace, true, movedText.join("\n"));
    }

    /**
     * Builds the edit creating a new condition block that holds the moved elements.
     *
     * @param pattern The pattern the block is appended to
     * @param conditionKind Whether a `forbid` or a `require` block is created
     * @param movedText The source text of the moved elements
     * @returns The workspace edit, or `undefined` when the pattern has no braces
     */
    private insertNewBlock(
        pattern: PatternType,
        conditionKind: "forbid" | "require",
        movedText: string[]
    ): WorkspaceEdit | undefined {
        const cstNode = pattern.$cstNode;
        if (cstNode == undefined) {
            return undefined;
        }
        const openBrace = GrammarUtils.findNodeForKeyword(cstNode, "{");
        const closeBrace = GrammarUtils.findNodeForKeyword(cstNode, "}");
        if (openBrace == undefined || closeBrace == undefined) {
            return undefined;
        }

        const name = this.findFreeConditionName(pattern, conditionKind);
        const body = movedText.map((text) => `    ${text}`).join("\n");
        return this.insertIntoScope(openBrace, closeBrace, true, `${conditionKind} ${name} {\n${body}\n}`);
    }

    /**
     * Picks a block name that is not yet used within the pattern.
     *
     * @param pattern The pattern the new block belongs to
     * @param conditionKind The kind of block being created
     * @returns An unused block name
     */
    private findFreeConditionName(pattern: PatternType, conditionKind: "forbid" | "require"): string {
        const used = new Set(
            this.getConditions(pattern)
                .map((condition) => condition.name)
                .filter((name): name is string => name != undefined)
        );
        let index = 1;
        while (used.has(`${conditionKind}${index}`)) {
            index++;
        }
        return `${conditionKind}${index}`;
    }
}
