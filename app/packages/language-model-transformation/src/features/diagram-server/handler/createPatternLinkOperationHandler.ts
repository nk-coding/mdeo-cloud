import type { GNode, ModelState, GModelIndex, CreateEdgeResult } from "@mdeo/language-shared";
import {
    BaseCreateEdgeOperationHandler,
    sharedImport,
    DefaultModelIdRegistry,
    ModelIdProvider as ModelIdProviderKey,
    type ModelIdProvider,
    type ModelIdRegistry
} from "@mdeo/language-shared";
import type { CreateEdgeOperation } from "@mdeo/editor-protocol";
import { LinkAssociationResolver, type LinkAssociationDisambiguation } from "@mdeo/language-model";
import type { AstNode } from "langium";
import type { CompositeCstNode } from "langium";
import {
    PatternObjectInstance,
    PatternObjectInstanceReference,
    PatternObjectInstanceDelete,
    PatternLink,
    PatternLinkEnd,
    PatternModifier,
    type PatternObjectInstanceType,
    type PatternObjectInstanceReferenceType,
    type PatternObjectInstanceDeleteType,
    type PatternLinkType,
    type PatternLinkEndType,
    type PatternModifierType,
    type PatternType
} from "../../../grammar/modelTransformationTypes.js";
import type { ClassType } from "@mdeo/language-metamodel";
import { ModelTransformationElementType, PatternModifierKind } from "../model/elementTypes.js";
import { GPatternInstanceNode } from "../model/patternInstanceNode.js";
import type { WorkspaceEdit } from "vscode-languageserver-types";

const { injectable, inject } = sharedImport("inversify");
const { ModelState: ModelStateKey, GModelIndex: GModelIndexKey } = sharedImport("@eclipse-glsp/server");

/**
 * Parameters included in the create-edge schema for pattern links.
 * Used for both disambiguation (multiple association candidates) and
 * modifier determination (persist-persist edge case).
 */
export interface PatternLinkSchemaParams extends LinkAssociationDisambiguation {
    /**
     * The modifier to apply to the new link.
     * Only relevant when both endpoints have NONE (persist) modifier, in which
     * case the schema resolver includes the user-selected context mode here.
     */
    modifier: PatternModifierKind;
}

/**
 * Resolved information about a pattern instance visual node.
 * Abstracts over PatternObjectInstance, PatternObjectInstanceReference, and PatternObjectInstanceDelete.
 */
interface ResolvedPatternInstance {
    /**
     * The semantic name used in PatternLinkEnd references.
     */
    name: string;
    /**
     * The metamodel class of this instance.
     */
    classType: ClassType;
    /**
     * The effective modifier of this instance in the current match.
     */
    modifier: PatternModifierKind;
    /**
     * The Pattern AST node that contains this element.
     */
    pattern: PatternType;
}

/**
 * Operation handler for creating pattern links between instances in a model transformation diagram.
 *
 * Handles create-edge operations for {@link ModelTransformationElementType.EDGE_PATTERN_LINK} edges.
 * The link modifier is inferred automatically from the source and target node modifiers; for
 * persist-persist pairs the user-supplied context mode (from {@link PatternLinkSchemaParams.modifier})
 * is used instead. The link is inserted into the correct pattern using {@link insertIntoScope}.
 */
@injectable()
export class CreatePatternLinkOperationHandler extends BaseCreateEdgeOperationHandler {
    override readonly operationType = "createEdge";
    override label = "Create pattern link";
    readonly elementTypeIds = [ModelTransformationElementType.EDGE_PATTERN_LINK];

    @inject(ModelStateKey)
    protected readonly localModelState!: ModelState;

    @inject(GModelIndexKey)
    protected readonly localIndex!: GModelIndex;

    @inject(ModelIdProviderKey)
    protected readonly idProvider!: ModelIdProvider;

    /**
     * Creates a new pattern link between two pattern instance nodes.
     *
     * Validates that both source and target are {@link GPatternInstanceNode}s belonging to
     * the same pattern, infers the link modifier, checks for a valid metamodel association,
     * serializes the link, and inserts it into the pattern via {@link insertIntoScope}.
     *
     * @param operation The create-edge operation
     * @param sourceElement The source GNode (must be a GPatternInstanceNode)
     * @param targetElement The target GNode (must be a GPatternInstanceNode)
     * @returns The create-edge result, or undefined if the link cannot be created
     */
    protected override async createEdge(
        operation: CreateEdgeOperation,
        sourceElement: GNode,
        targetElement: GNode
    ): Promise<CreateEdgeResult | undefined> {
        if (operation.elementTypeId !== ModelTransformationElementType.EDGE_PATTERN_LINK) {
            return undefined;
        }

        if (!(sourceElement instanceof GPatternInstanceNode) || !(targetElement instanceof GPatternInstanceNode)) {
            return undefined;
        }

        const sourceModel = this.localModelState.sourceModel;
        const registry: ModelIdRegistry | undefined =
            sourceModel != undefined ? new DefaultModelIdRegistry(sourceModel, this.idProvider) : undefined;

        const sourceInfo = this.resolvePatternInstance(sourceElement, registry);
        const targetInfo = this.resolvePatternInstance(targetElement, registry);
        if (!sourceInfo || !targetInfo) {
            return undefined;
        }

        if (sourceInfo.pattern !== targetInfo.pattern) {
            return undefined;
        }

        const params = operation.schema.params as PatternLinkSchemaParams;
        const modifier = this.modifierStringToKind(params.modifier);

        const associationResolver = new LinkAssociationResolver(
            this.localModelState.languageServices.shared.AstReflection
        );
        const candidates = associationResolver.findCandidates(sourceInfo.classType, targetInfo.classType);
        if (candidates.length === 0) {
            return undefined;
        }

        const candidate = associationResolver.selectCandidate(candidates, params);
        if (!candidate) {
            return undefined;
        }

        const sourceProperty = params?.sourceProperty;
        const targetProperty = params?.targetProperty;

        const modifierNode: PatternModifierType | undefined =
            modifier !== PatternModifierKind.NONE
                ? { $type: PatternModifier.name, modifier: modifier as string }
                : undefined;

        const linkNode: PatternLinkType = {
            $type: PatternLink.name,
            modifier: modifierNode,
            source: {
                $type: PatternLinkEnd.name,
                object: { $refText: sourceInfo.name },
                property: sourceProperty ? { $refText: sourceProperty } : undefined
            } as PatternLinkEndType,
            target: {
                $type: PatternLinkEnd.name,
                object: { $refText: targetInfo.name },
                property: targetProperty ? { $refText: targetProperty } : undefined
            } as PatternLinkEndType
        };

        const linkText = await this.serializeNode(linkNode);
        const workspaceEdit = this.insertLinkIntoPattern(sourceInfo.pattern, linkText);

        const edgeId = this.computePatternLinkId(sourceInfo.name, targetInfo.name, sourceProperty, targetProperty);

        return {
            edgeId,
            edgeType: ModelTransformationElementType.EDGE_PATTERN_LINK,
            workspaceEdit
        };
    }

    /**
     * Resolves a GPatternInstanceNode to its underlying AST pattern instance information.
     *
     * Works for all three backing AST node types:
     * - {@link PatternObjectInstance}: direct instance in this match
     * - {@link PatternObjectInstanceReference}: a reference to an instance from a prior match
     * - {@link PatternObjectInstanceDelete}: a deletion of an instance from a prior match
     *
     * Uses the provided {@link ModelIdRegistry} to look up instance names consistently
     * with the server's IdProvider, so the generated edge ID is guaranteed to match.
     *
     * @param node The GPatternInstanceNode to resolve
     * @param registry The model ID registry for consistent name resolution (may be undefined)
     * @returns Resolved instance information, or undefined if resolution fails
     */
    private resolvePatternInstance(
        node: GPatternInstanceNode,
        registry: ModelIdRegistry | undefined
    ): ResolvedPatternInstance | undefined {
        const astNode = this.localIndex.getAstNode(node) as AstNode | undefined;
        if (!astNode) {
            return undefined;
        }

        const reflection = this.localModelState.languageServices.shared.AstReflection;

        if (reflection.isInstance(astNode, PatternObjectInstance)) {
            const instance = astNode as PatternObjectInstanceType;
            const name = registry?.getName(instance as AstNode) ?? instance.name;
            const classType = instance.class?.ref as ClassType | undefined;
            const modifier = this.modifierStringToKind(instance.modifier?.modifier);
            const pattern = instance.$container as PatternType;
            if (!name || !classType) return undefined;
            return { name, classType, modifier, pattern };
        }

        if (reflection.isInstance(astNode, PatternObjectInstanceReference)) {
            const ref = astNode as PatternObjectInstanceReferenceType;
            const instance = ref.instance?.ref;
            if (!instance) return undefined;
            const name = registry?.getName(instance as AstNode) ?? instance.name;
            const classType = instance.class?.ref as ClassType | undefined;
            const pattern = ref.$container as PatternType;
            if (!name || !classType) return undefined;
            return { name, classType, modifier: PatternModifierKind.NONE, pattern };
        }

        if (reflection.isInstance(astNode, PatternObjectInstanceDelete)) {
            const del = astNode as PatternObjectInstanceDeleteType;
            const instance = del.instance?.ref;
            if (!instance) return undefined;
            const name = registry?.getName(instance as AstNode) ?? instance.name;
            const classType = instance.class?.ref as ClassType | undefined;
            const pattern = del.$container as PatternType;
            if (!name || !classType) return undefined;
            return { name, classType, modifier: PatternModifierKind.DELETE, pattern };
        }

        return undefined;
    }

    /**
     * Inserts a serialized link text into the given pattern using {@link insertIntoScope}.
     *
     * The opening `{` and closing `}` keyword nodes are extracted from the pattern's
     * composite CST node. A blank line is added before the content when the pattern
     * already contains elements.
     *
     * @param pattern The target pattern whose CST scope receives the new link
     * @param text The already-serialized link text to insert
     * @returns The workspace edit that inserts the link before the closing `}`
     * @throws {Error} If the pattern has no CST node
     */
    private insertLinkIntoPattern(pattern: PatternType, text: string): WorkspaceEdit {
        const patternCst = pattern.$cstNode as CompositeCstNode | undefined;
        if (!patternCst) {
            throw new Error("Pattern has no CST node; cannot insert link.");
        }
        const content = patternCst.content;
        const openBrace = content[0]!;
        const closeBrace = content[content.length - 1]!;
        const hasContent = (pattern.elements?.length ?? 0) > 0;
        return this.insertIntoScope(openBrace, closeBrace, hasContent, text);
    }

    /**
     * Computes the pattern link edge ID in the same format as the ID provider.
     *
     * The ID follows the schema `PatternLink_<sourceEnd>--<targetEnd>`, where each end
     * is formatted as `<instanceName>[_<property>]`.
     *
     * @param sourceName The name of the source pattern object instance
     * @param targetName The name of the target pattern object instance
     * @param sourceProperty Optional source-side property name
     * @param targetProperty Optional target-side property name
     * @returns The stable edge ID compatible with the server ID provider
     */
    private computePatternLinkId(
        sourceName: string,
        targetName: string,
        sourceProperty: string | undefined,
        targetProperty: string | undefined
    ): string {
        const sourceEnd = sourceProperty ? `${sourceName}_${sourceProperty}` : sourceName;
        const targetEnd = targetProperty ? `${targetName}_${targetProperty}` : targetName;
        return `${PatternLink.name}_${sourceEnd}--${targetEnd}`;
    }

    /**
     * Determines the modifier for a new edge given the effective modifiers of its endpoints.
     *
     * Rules:
     * - If one endpoint has FORBID or REQUIRE, the other must be FORBID, REQUIRE, or NONE (persist);
     *   the edge takes the modifier of the non-persist endpoint. Mixed FORBID+REQUIRE is invalid.
     * - If one endpoint has CREATE or DELETE, the other must be CREATE, DELETE, or NONE (persist);
     *   the edge takes the modifier of the non-persist endpoint.
     * - Mixing FORBID/REQUIRE with CREATE/DELETE is always invalid.
     * - If both endpoints are NONE (persist), the context modifier is used as-is.
     *
     * @param sourceModifier The effective modifier of the source node
     * @param targetModifier The effective modifier of the target node
     * @param contextModifier The user-supplied modifier (relevant only for persist-persist edges)
     * @returns The inferred edge modifier, or undefined when the combination is invalid
     */
    private determineEdgeModifier(
        sourceModifier: PatternModifierKind,
        targetModifier: PatternModifierKind,
        contextModifier: PatternModifierKind
    ): PatternModifierKind | undefined {
        const isForbidRequire = (m: PatternModifierKind): boolean =>
            m === PatternModifierKind.FORBID || m === PatternModifierKind.REQUIRE;
        const isCreateDelete = (m: PatternModifierKind): boolean =>
            m === PatternModifierKind.CREATE || m === PatternModifierKind.DELETE;
        const isPersist = (m: PatternModifierKind): boolean => m === PatternModifierKind.NONE;

        // Incompatible cross-group combinations.
        if (isForbidRequire(sourceModifier) && isCreateDelete(targetModifier)) return undefined;
        if (isCreateDelete(sourceModifier) && isForbidRequire(targetModifier)) return undefined;

        // Both persist → use the user-supplied context modifier.
        if (isPersist(sourceModifier) && isPersist(targetModifier)) {
            return contextModifier;
        }

        // At least one endpoint is CREATE or DELETE.
        if (isCreateDelete(sourceModifier) || isCreateDelete(targetModifier)) {
            // Use the non-persist (create/delete) modifier.
            return isCreateDelete(sourceModifier) ? sourceModifier : targetModifier;
        }

        // At least one endpoint is FORBID or REQUIRE (and neither is create/delete).
        if (isForbidRequire(sourceModifier) && isForbidRequire(targetModifier)) {
            // Both forbid/require: they must agree.
            if (sourceModifier !== targetModifier) return undefined;
            return sourceModifier;
        }
        if (isForbidRequire(sourceModifier)) return sourceModifier;
        if (isForbidRequire(targetModifier)) return targetModifier;

        return undefined;
    }

    /**
     * Converts a modifier string from operation args or schema params to a {@link PatternModifierKind}.
     * Unknown or absent strings default to {@link PatternModifierKind.NONE}.
     *
     * @param modifier The raw modifier string (e.g. `"create"`, `"forbid"`)
     * @returns The corresponding {@link PatternModifierKind}
     */
    private modifierStringToKind(modifier: string | undefined): PatternModifierKind {
        switch (modifier) {
            case "create":
                return PatternModifierKind.CREATE;
            case "delete":
                return PatternModifierKind.DELETE;
            case "require":
                return PatternModifierKind.REQUIRE;
            case "forbid":
                return PatternModifierKind.FORBID;
            default:
                return PatternModifierKind.NONE;
        }
    }
}
