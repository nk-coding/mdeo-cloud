import type { AstReflection, ExtendedLangiumServices } from "@mdeo/language-common";
import {
    sharedImport,
    resolveRelativePath,
    isImportReference,
    getExportedEntitiesFromGlobalScope,
    getImportedEntitiesFromCurrentFile
} from "@mdeo/language-shared";
import type { AstNode, LangiumDocuments, ReferenceInfo, Scope } from "langium";
import { Config, type ConfigType } from "@mdeo/language-config";
import { resolveClassChain, type ClassType } from "@mdeo/language-metamodel";
import { AssociationEndCache } from "./associationEndCache.js";
import {
    configOptimizationFileScopingConfig,
    ConstraintReference,
    Objective,
    Refinement,
    MetamodelClass,
    type RefinementType,
    type GoalSectionType,
    GoalSection,
    type ProblemSectionType,
    ProblemSection
} from "../grammar/optimizationTypes.js";

const { DefaultScopeProvider, AstUtils, EMPTY_SCOPE, StreamScope, stream } = sharedImport("langium");

/**
 * Scope provider for the config-optimization language.
 * Handles scoping for:
 * - Import references (functions from script files)
 * - Constraint/Objective function references (from imports)
 * - Class references in refinements (from metamodel)
 * - Field references in refinements (from class properties)
 */
export class OptimizationScopeProvider extends DefaultScopeProvider {
    /**
     * The AST reflection service for type checking (extended version with isInstance).
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
     * Constructs a new OptimizationScopeProvider.
     *
     * @param services The extended Langium services.
     */
    constructor(services: ExtendedLangiumServices) {
        super(services);
        this.astReflection = services.shared.AstReflection;
        this.documents = services.shared.workspace.LangiumDocuments;
        this.associationEndCache = new AssociationEndCache(services);
    }

    /**
     * Gets the scope for a given reference context.
     *
     * @param context The reference context
     * @returns The scope for the reference
     */
    override getScope(context: ReferenceInfo): Scope {
        const document = AstUtils.getDocument(context.container);

        // Handle import references (entity in import statement)
        if (isImportReference(context, configOptimizationFileScopingConfig)) {
            return getExportedEntitiesFromGlobalScope(
                document,
                context,
                configOptimizationFileScopingConfig,
                this.indexManager
            );
        }

        // Handle constraint function references
        if (
            context.property === "constraint" &&
            this.astReflection.isInstance(context.container, ConstraintReference)
        ) {
            return this.getFunctionScope(context);
        }

        // Handle objective function references
        if (context.property === "objective" && this.astReflection.isInstance(context.container, Objective)) {
            return this.getFunctionScope(context);
        }

        // Handle class references in refinements
        if (context.property === "class" && this.astReflection.isInstance(context.container, Refinement)) {
            return this.getClassScope(context);
        }

        // Handle field references in refinements
        if (context.property === "field" && this.astReflection.isInstance(context.container, Refinement)) {
            return this.getFieldScope(context);
        }

        return EMPTY_SCOPE;
    }

    /**
     * Gets the scope for function references in constraints and objectives.
     * Returns functions imported in the containing goal section.
     *
     * @param context The reference context
     * @returns Scope with imported functions
     */
    private getFunctionScope(context: ReferenceInfo): Scope {
        const goalSection = this.findContainingGoalSection(context.container);
        if (goalSection == undefined) {
            return EMPTY_SCOPE;
        }

        return getImportedEntitiesFromCurrentFile(goalSection.imports, this.nameProvider, this.descriptions);
    }

    /**
     * Gets the scope for class references in refinements.
     * Returns classes from the metamodel defined in the problem section.
     *
     * @param context The reference context
     * @returns Scope with metamodel classes
     */
    private getClassScope(context: ReferenceInfo): Scope {
        const document = AstUtils.getDocument(context.container);
        const problemSection = this.findProblemSection(context.container);
        if (problemSection == undefined) {
            return EMPTY_SCOPE;
        }

        const metamodelPath = problemSection.metamodel;
        if (metamodelPath == undefined) {
            return EMPTY_SCOPE;
        }

        // Get classes from the metamodel file
        const targetUri = resolveRelativePath(document, metamodelPath).toString();
        const classDescriptions = this.indexManager.allElements(MetamodelClass.name, new Set([targetUri])).toArray();

        return new StreamScope(stream(classDescriptions));
    }

    /**
     * Gets the scope for field references in refinements.
     * Returns properties and association ends from the referenced class.
     *
     * @param context The reference context
     * @returns Scope with class properties and association ends
     */
    private getFieldScope(context: ReferenceInfo): Scope {
        const refinement = context.container as RefinementType;
        const classRef = refinement.class?.ref as ClassType | undefined;

        if (classRef == undefined) {
            return EMPTY_SCOPE;
        }

        // Get the class chain (includes parent classes)
        const classChain = resolveClassChain(classRef, this.astReflection);

        // Collect properties from all classes in the chain
        const properties = classChain.flatMap((cls) => cls.properties);

        // Collect association ends that reference any class in the chain
        const allAssociationEnds = classChain.flatMap((cls) => {
            return this.associationEndCache.getAssociationEndsForClass(cls);
        });

        // Deduplicate association ends by name
        const uniqueAssociationEnds = Array.from(new Map(allAssociationEnds.map((end) => [end.name, end])).values());

        // Combine properties and association ends
        const allFields: AstNode[] = [...properties, ...uniqueAssociationEnds];

        return this.createScopeForNodes(allFields);
    }

    /**
     * Finds the containing goal section for an AST node.
     *
     * @param node The AST node
     * @returns The goal section, or undefined if not found
     */
    private findContainingGoalSection(node: AstNode): GoalSectionType | undefined {
        let current: AstNode | undefined = node;
        while (current != undefined) {
            if (this.astReflection.isInstance(current, GoalSection)) {
                return current as GoalSectionType;
            }
            // Also check if current is a wrapper with a content field
            const anyNode = current as any;
            if (anyNode.content && this.astReflection.isInstance(anyNode.content, GoalSection)) {
                return anyNode.content as GoalSectionType;
            }
            current = current.$container;
        }
        return undefined;
    }

    /**
     * Finds the problem section in the config document.
     *
     * @param node Any AST node in the config document
     * @returns The problem section, or undefined if not found
     */
    private findProblemSection(node: AstNode): ProblemSectionType | undefined {
        // Navigate up to the Config root
        let current: AstNode | undefined = node;
        while (current != undefined && !this.astReflection.isInstance(current, Config)) {
            current = current.$container;
        }

        if (current == undefined) {
            return undefined;
        }

        // Find the problem section among the config sections
        const config = current as ConfigType;
        for (const section of config.sections) {
            // Check if the section itself is a ProblemSection
            if (this.astReflection.isInstance(section, ProblemSection)) {
                return section as ProblemSectionType;
            }
            // Also check if section is a wrapper with a content field
            const anySection = section as any;
            if (anySection.content && this.astReflection.isInstance(anySection.content, ProblemSection)) {
                return anySection.content as ProblemSectionType;
            }
        }

        return undefined;
    }
}
