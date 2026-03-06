import type { AstReflection } from "@mdeo/language-common";
import {
    resolveClassChain,
    type AssociationType,
    type AssociationEndType,
    type ClassType
} from "@mdeo/language-metamodel";
import { BaseMetamodelHelper } from "../baseMetamodelHelper.js";

/**
 * Oriented association candidate for a concrete source→target object direction.
 */
export interface LinkAssociationCandidate {
    /**
     * The underlying association.
     */
    association: AssociationType;
    /**
     * The end that acts as the source for this direction.
     */
    sourceEnd: AssociationEndType;
    /**
     * The end that acts as the target for this direction.
     */
    targetEnd: AssociationEndType;
}

/**
 * Disambiguation payload used for create-edge schema params.
 */
export interface LinkAssociationDisambiguation {
    /**
     * The name of the source-side association end property, if specified.
     */
    sourceProperty?: string;
    /**
     * The name of the target-side association end property, if specified.
     */
    targetProperty?: string;
}

/**
 * Resolves valid associations for link creation between two classes,
 * considering inheritance and both association directions.
 *
 * Extends {@link BaseMetamodelHelper} for shared metamodel traversal and
 * adds orientation-aware candidate lookup and disambiguation support.
 */
export class LinkAssociationResolver extends BaseMetamodelHelper {
    /**
     * Creates a resolver instance.
     *
     * @param reflection The AST reflection used for runtime type checks
     */
    constructor(reflection: AstReflection) {
        super(reflection);
    }

    /**
     * Finds all valid oriented candidates between source and target classes.
     *
     * Each association that connects the two classes (considering inheritance) is
     * returned once per valid direction, so an association that applies in both
     * directions may yield two candidates.
     *
     * @param sourceClass The source object class
     * @param targetClass The target object class
     * @returns All matching oriented association candidates
     */
    findCandidates(sourceClass: ClassType, targetClass: ClassType): LinkAssociationCandidate[] {
        const result: LinkAssociationCandidate[] = [];
        const sourceChain = new Set(resolveClassChain(sourceClass, this.reflection));
        const targetChain = new Set(resolveClassChain(targetClass, this.reflection));

        const associations = this.findAssociationsBetweenClasses(sourceClass, targetClass);

        for (const association of associations) {
            const assocSourceClass = this.resolveToClass(association.source?.class?.ref);
            const assocTargetClass = this.resolveToClass(association.target?.class?.ref);

            if (!assocSourceClass || !assocTargetClass || !association.source || !association.target) {
                continue;
            }

            if (sourceChain.has(assocSourceClass) && targetChain.has(assocTargetClass)) {
                result.push({
                    association,
                    sourceEnd: association.source,
                    targetEnd: association.target
                });
            }

            if (sourceChain.has(assocTargetClass) && targetChain.has(assocSourceClass)) {
                result.push({
                    association,
                    sourceEnd: association.target,
                    targetEnd: association.source
                });
            }
        }

        return result;
    }

    /**
     * Selects a unique candidate using disambiguation params.
     *
     * @param candidates Candidate associations for the source-target pair
     * @param params Optional disambiguation parameters from schema params
     * @returns The uniquely matching candidate, or undefined when zero or multiple match
     */
    selectCandidate(
        candidates: LinkAssociationCandidate[],
        params: LinkAssociationDisambiguation | undefined
    ): LinkAssociationCandidate | undefined {
        if (candidates.length === 0) {
            return undefined;
        }
        if (candidates.length === 1) {
            return candidates[0];
        }

        if (params == undefined) {
            return undefined;
        }

        const filtered = candidates.filter((candidate) => {
            const sourceMatch =
                params.sourceProperty == undefined || params.sourceProperty === candidate.sourceEnd.name;
            const targetMatch =
                params.targetProperty == undefined || params.targetProperty === candidate.targetEnd.name;
            return sourceMatch && targetMatch;
        });

        return filtered.length === 1 ? filtered[0] : undefined;
    }

    /**
     * Chooses a preferred label for rendering a disambiguation UI.
     *
     * Prefers the source-end name; falls back to the target-end name.
     *
     * @param candidate The candidate association orientation
     * @returns Preferred label metadata for rendering, or undefined when no names are available
     */
    choosePreferredLabel(candidate: LinkAssociationCandidate): { end: "source" | "target"; text: string } | undefined {
        if (candidate.sourceEnd.name) {
            return { end: "source", text: candidate.sourceEnd.name };
        }
        if (candidate.targetEnd.name) {
            return { end: "target", text: candidate.targetEnd.name };
        }
        return undefined;
    }
}
