import type { AstReflection, ExtendedLangiumServices } from "@mdeo/language-common";
import { sharedImport } from "@mdeo/language-shared";
import type { AstNode, AstNodeDescriptionProvider, LangiumDocuments, ReferenceInfo, Scope } from "langium";
import { getScopeFromMetamodelFile, resolveClassChain, type ClassType } from "@mdeo/language-metamodel";
import { AssociationEndCache } from "@mdeo/language-model";
import { ClassMutation, EdgeMutation, type EdgeMutationType } from "../grammar/mdeoTypes.js";
import type { MdeoMetamodelResolver } from "./mdeoMetamodelResolver.js";
import type { MdeoAdditionalServices } from "../mdeoPlugin.js";

const { DefaultScopeProvider, AstUtils, EMPTY_SCOPE } = sharedImport("langium");

/**
 * Scope provider for the config-mdeo language.
 * Handles scoping for:
 * - Class references in mutation operators (from metamodel)
 * - Edge references in edge mutation operators (from class properties)
 */
export class MdeoScopeProvider extends DefaultScopeProvider {
    /**
     * The AST reflection service for type checking.
     */
    protected readonly astReflection: AstReflection;

    /**
     * The Langium documents service for accessing imported files.
     */
    protected readonly documents: LangiumDocuments;

    /**
     * Cache for association end lookups.
     */
    private readonly associationEndCache: AssociationEndCache;

    /**
     * The description provider for creating AST node descriptions.
     */
    private readonly descriptionProvider: AstNodeDescriptionProvider;

    /**
     * The metamodel resolver service.
     */
    private readonly metamodelResolver: MdeoMetamodelResolver;

    /**
     * Constructs a new MdeoScopeProvider.
     *
     * @param services The extended Langium services with MDEO additions
     */
    constructor(services: ExtendedLangiumServices & MdeoAdditionalServices) {
        super(services);
        this.astReflection = services.shared.AstReflection;
        this.documents = services.shared.workspace.LangiumDocuments;
        this.associationEndCache = new AssociationEndCache(services);
        this.descriptionProvider = services.workspace.AstNodeDescriptionProvider;
        this.metamodelResolver = services.MdeoMetamodelResolver;
    }

    /**
     * Gets the scope for a given reference context.
     *
     * @param context The reference context
     * @returns The scope for the reference
     */
    override getScope(context: ReferenceInfo): Scope {
        if (
            context.property === "class" &&
            (this.astReflection.isInstance(context.container, ClassMutation) ||
                this.astReflection.isInstance(context.container, EdgeMutation))
        ) {
            return this.getClassScope(context);
        }

        if (context.property === "edge" && this.astReflection.isInstance(context.container, EdgeMutation)) {
            return this.getEdgeScope(context);
        }

        return EMPTY_SCOPE;
    }

    /**
     * Gets the scope for class references in mutation operators.
     * Returns classes from the metamodel defined in the problem section.
     *
     * @param context The reference context
     * @returns Scope with metamodel classes
     */
    private getClassScope(context: ReferenceInfo): Scope {
        const document = AstUtils.getDocument(context.container);
        const uri = this.metamodelResolver.getMetamodelUri(document);
        if (uri == undefined) {
            return EMPTY_SCOPE;
        }

        const targetDoc = this.documents.getDocument(uri);
        if (targetDoc == undefined) {
            return EMPTY_SCOPE;
        }

        return getScopeFromMetamodelFile(targetDoc, this.documents, this.descriptionProvider);
    }

    /**
     * Gets the scope for edge references in edge mutation operators.
     * Returns properties and association ends from the referenced class.
     *
     * @param context The reference context
     * @returns Scope with class properties and association ends
     */
    private getEdgeScope(context: ReferenceInfo): Scope {
        const edgeMutation = context.container as EdgeMutationType;
        const classRef = edgeMutation.class?.ref as ClassType | undefined;

        if (classRef == undefined) {
            return EMPTY_SCOPE;
        }

        const classChain = resolveClassChain(classRef, this.astReflection);
        const properties = classChain.flatMap((cls) => cls.properties);
        const allAssociationEnds = classChain.flatMap((cls) => {
            return this.associationEndCache.getAssociationEndsForClass(cls);
        });

        const uniqueAssociationEnds = Array.from(new Map(allAssociationEnds.map((end) => [end.name, end])).values());
        const allFields: AstNode[] = [...properties, ...uniqueAssociationEnds];

        return this.createScopeForNodes(allFields);
    }
}
