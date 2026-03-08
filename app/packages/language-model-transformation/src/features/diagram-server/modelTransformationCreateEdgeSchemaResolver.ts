import type { CreateEdgeSchema } from "@mdeo/editor-protocol";
import { CreateEdgeSchemaResolver, sharedImport } from "@mdeo/language-shared";
import type { ModelState, GModelIndex, InitialCreateEdgeSchemaRequest, TargetCreateEdgeSchemaRequest } from "@mdeo/language-shared";
import type { AstNode } from "langium";
import {
    PatternObjectInstance,
    PatternObjectInstanceReference,
    PatternObjectInstanceDelete,
    type PatternObjectInstanceType,
    type PatternObjectInstanceReferenceType,
    type PatternObjectInstanceDeleteType,
    type PatternType
} from "../../grammar/modelTransformationTypes.js";
import type { ClassType } from "@mdeo/language-metamodel";
import { LinkAssociationResolver } from "@mdeo/language-model";
import { GPatternInstanceNode } from "./model/patternInstanceNode.js";
import { GPatternLinkEdge } from "./model/patternLinkEdge.js";
import { GPatternLinkEndNode } from "./model/patternLinkEndNode.js";
import { GPatternLinkEndLabel } from "./model/patternLinkEndLabel.js";
import { ModelTransformationElementType, PatternModifierKind } from "./model/elementTypes.js";
import { EdgeLayoutMetadataUtil, NodeLayoutMetadataUtil } from "./metadataTypes.js";
import type { GModelElementSchema } from "@eclipse-glsp/protocol";
import type { PatternLinkSchemaParams } from "./handler/createPatternLinkOperationHandler.js";

const { injectable, inject } = sharedImport("inversify");
const { ModelState: ModelStateKey, GModelIndex: GModelIndexKey } = sharedImport("@eclipse-glsp/server");

/**
 * Context payload forwarded by the client's {@code PatternLinkContextProvider}.
 * Carries the currently active creation mode so the server can pre-select the
 * edge modifier for persist-persist pairs.
 */
interface PatternLinkCreationContext {
    /** The active node creation mode string (e.g. `"persist"`, `"create"`, `"delete"`). */
    mode?: string;
}

/**
 * Create-edge schema resolver for model transformation diagrams.
 *
 * Validates source/target combinations for pattern link creation and produces
 * the visual feedback schema used by the client during edge drawing. Only
 * {@link ModelTransformationElementType.EDGE_PATTERN_LINK} edges are supported.
 *
 * Resolution rules:
 * - Both endpoints must be {@link GPatternInstanceNode}s in the same pattern.
 * - Their modifiers must be compatible (no FORBID/REQUIRE mixed with CREATE/DELETE).
 * - There must be at least one valid metamodel association between the instance classes.
 */
@injectable()
export class ModelTransformationCreateEdgeSchemaResolver extends CreateEdgeSchemaResolver {
    @inject(ModelStateKey)
    protected readonly modelState!: ModelState;

    @inject(GModelIndexKey)
    protected readonly index!: GModelIndex;

    /**
     * Returns an initial schema when the source node is a pattern instance that can
     * participate in links (its class has at least one known association).
     *
     * @param request The initial schema request carrying the source element ID and optional context
     * @returns A basic GPatternLinkEdge schema, or undefined if the source is not suitable
     */
    override async getInitialSchema(request: InitialCreateEdgeSchemaRequest): Promise<CreateEdgeSchema | undefined> {
        const sourceInfo = this.resolvePatternInstanceByElementId(request.sourceElementId);
        if (sourceInfo === undefined) {
            return undefined;
        }

        const associationResolver = new LinkAssociationResolver(
            this.modelState.languageServices.shared.AstReflection
        );
        if (!associationResolver.hasAnyAssociation(sourceInfo.classType)) {
            return undefined;
        }

        return this.buildSchema(request.sourceElementId, undefined, {});
    }

    /**
     * Returns a target-specific schema when both nodes form a valid pattern link pair.
     *
     * Validation:
     * 1. Both must be {@link GPatternInstanceNode}s backed by resolvable pattern instances.
     * 2. They must belong to the same {@link PatternType}.
     * 3. Their modifiers must be compatible according to the transformation modifier rules.
     * 4. At least one valid metamodel association must exist between the instance classes.
     *
     * When there are multiple candidates, an attempt is made to disambiguate using the
     * preferred label (source or target property name). The resolved modifier is included
     * in {@link PatternLinkSchemaParams.modifier} so the operation handler can use it.
     *
     * @param request The target schema request with source/target IDs and optional context
     * @returns A schema describing the edge and its creation params, or undefined if invalid
     */
    override async getTargetSchema(request: TargetCreateEdgeSchemaRequest): Promise<CreateEdgeSchema | undefined> {
        const sourceInfo = this.resolvePatternInstanceByElementId(request.sourceElementId);
        const targetInfo = this.resolvePatternInstanceByElementId(request.targetElementId);
        if (sourceInfo === undefined || targetInfo === undefined) {
            return undefined;
        }

        // Edges can only be created between instances in the same pattern.
        if (sourceInfo.pattern !== targetInfo.pattern) {
            return undefined;
        }

        const context = request.context as PatternLinkCreationContext | undefined;
        const contextModifier = this.modifierStringToKind(context?.mode);
        const modifier = this.determineEdgeModifier(sourceInfo.modifier, targetInfo.modifier, contextModifier);
        if (modifier === undefined) {
            return undefined;
        }

        const associationResolver = new LinkAssociationResolver(
            this.modelState.languageServices.shared.AstReflection
        );
        const candidates = associationResolver.findCandidates(sourceInfo.classType, targetInfo.classType);
        if (candidates.length === 0) {
            return undefined;
        }

        const params: PatternLinkSchemaParams = { modifier: modifier === PatternModifierKind.NONE ? undefined : modifier };

        if (candidates.length === 1) {
            return this.buildSchema(request.sourceElementId, request.targetElementId, params);
        }

        // Multiple candidates: pick the one with a preferred label for disambiguation.
        const selected = candidates.find(
            (candidate) => associationResolver.choosePreferredLabel(candidate) !== undefined
        );
        if (selected === undefined) {
            return undefined;
        }

        const label = associationResolver.choosePreferredLabel(selected);
        if (label === undefined) {
            return undefined;
        }

        if (label.end === "source") {
            params.sourceProperty = label.text;
        } else {
            params.targetProperty = label.text;
        }

        return this.buildSchema(request.sourceElementId, request.targetElementId, params, label);
    }

    /**
     * Builds a {@link CreateEdgeSchema} with a {@link GPatternLinkEdge} feedback template.
     *
     * The edge and any optional end-node labels are constructed so that the client can
     * render the feedback edge during dragging without encountering missing metadata errors.
     *
     * @param sourceElementId The source element ID
     * @param targetElementId The optional target element ID (undefined during initial schema)
     * @param params Backend parameters included in the schema (modifier, disambiguation properties)
     * @param label Optional label descriptor for rendering a property end-node
     * @returns The assembled {@link CreateEdgeSchema}
     */
    protected buildSchema(
        sourceElementId: string,
        targetElementId: string | undefined,
        params: PatternLinkSchemaParams,
        label?: { end: "source" | "target"; text: string }
    ): CreateEdgeSchema {
        const edgeId = "__create-edge-schema";

        const edge = GPatternLinkEdge.builder()
            .id(edgeId)
            .sourceId(sourceElementId)
            .targetId(targetElementId ?? sourceElementId)
            .modifier(PatternModifierKind.NONE)
            .meta(EdgeLayoutMetadataUtil.create())
            .build();

        if (label !== undefined) {
            const nodeId = `${edgeId}#${label.end}-node`;

            const endNode = GPatternLinkEndNode.builder()
                .id(nodeId)
                .end(label.end === "source" ? "target" : "source")
                .meta(NodeLayoutMetadataUtil.create(0, 0))
                .build();

            const endLabel = GPatternLinkEndLabel.builder()
                .id(`${edgeId}#${label.end}-label`)
                .text(label.text)
                .readonly(true)
                .build();

            endNode.children.push(endLabel);
            edge.children.push(endNode);
        }

        return {
            elementTypeId: ModelTransformationElementType.EDGE_PATTERN_LINK,
            template: edge as unknown as GModelElementSchema,
            params
        };
    }

    /**
     * Resolves a diagram element ID to its pattern instance info.
     *
     * Handles all three backing AST types: {@link PatternObjectInstance},
     * {@link PatternObjectInstanceReference}, and {@link PatternObjectInstanceDelete}.
     *
     * @param elementId The GModel element ID
     * @returns Resolved info, or undefined when resolution fails
     */
    protected resolvePatternInstanceByElementId(
        elementId: string
    ): { classType: ClassType; modifier: PatternModifierKind; pattern: PatternType } | undefined {
        const modelElement = this.modelState.index.find(elementId);
        if (modelElement === undefined || !(modelElement instanceof GPatternInstanceNode)) {
            return undefined;
        }
        const astNode = this.index.getAstNode(modelElement) as AstNode | undefined;
        if (astNode === undefined) {
            return undefined;
        }

        const reflection = this.modelState.languageServices.shared.AstReflection;

        if (reflection.isInstance(astNode, PatternObjectInstance)) {
            const instance = astNode as PatternObjectInstanceType;
            const classType = instance.class?.ref as ClassType | undefined;
            if (!classType) return undefined;
            const modifier = this.modifierStringToKind(instance.modifier?.modifier);
            const pattern = instance.$container as PatternType;
            return { classType, modifier, pattern };
        }

        if (reflection.isInstance(astNode, PatternObjectInstanceReference)) {
            const ref = astNode as PatternObjectInstanceReferenceType;
            const classType = ref.instance?.ref?.class?.ref as ClassType | undefined;
            if (!classType) return undefined;
            const pattern = ref.$container as PatternType;
            return { classType, modifier: PatternModifierKind.NONE, pattern };
        }

        if (reflection.isInstance(astNode, PatternObjectInstanceDelete)) {
            const del = astNode as PatternObjectInstanceDeleteType;
            const classType = del.instance?.ref?.class?.ref as ClassType | undefined;
            if (!classType) return undefined;
            const pattern = del.$container as PatternType;
            return { classType, modifier: PatternModifierKind.DELETE, pattern };
        }

        return undefined;
    }

    /**
     * Determines the modifier for a new edge given the effective modifiers of its two endpoints.
     *
     * @param sourceModifier The effective modifier of the source node
     * @param targetModifier The effective modifier of the target node
     * @param contextModifier The user-supplied context modifier (relevant for persist-persist pairs)
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

        // Cross-group combinations are always invalid.
        if (isForbidRequire(sourceModifier) && isCreateDelete(targetModifier)) return undefined;
        if (isCreateDelete(sourceModifier) && isForbidRequire(targetModifier)) return undefined;

        // Both persist: use the context mode.
        if (isPersist(sourceModifier) && isPersist(targetModifier)) {
            return contextModifier;
        }

        // At least one CREATE/DELETE: edge is create/delete.
        if (isCreateDelete(sourceModifier) || isCreateDelete(targetModifier)) {
            return isCreateDelete(sourceModifier) ? sourceModifier : targetModifier;
        }

        // At least one FORBID/REQUIRE: edge is forbid/require.
        if (isForbidRequire(sourceModifier) && isForbidRequire(targetModifier)) {
            if (sourceModifier !== targetModifier) return undefined;
            return sourceModifier;
        }
        if (isForbidRequire(sourceModifier)) return sourceModifier;
        if (isForbidRequire(targetModifier)) return targetModifier;

        return undefined;
    }

    /**
     * Converts a modifier string to a {@link PatternModifierKind}.
     * Unknown or absent strings default to {@link PatternModifierKind.NONE}.
     *
     * @param modifier The raw modifier string
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
