import type { TypirLangiumSpecifics } from "typir-langium";
import { PartialTypeSystem } from "./partialTypeSystem.js";
import { Class, Enum } from "@mdeo/language-metamodel";
import type { InferenceProblem } from "typir";
import type { ClassTypeRef } from "../typir-extensions/config/type.js";
import { getClassPackage, getEnumPackage } from "../features/metamodel/metamodelClassExtractor.js";
import { sharedImport } from "@mdeo/language-shared";
import type { AstNode } from "langium";

const { AstUtils } = sharedImport("langium");

/**
 * Type system for the Metamodel language.
 * Provides type inference for metamodel Class and Enum nodes.
 */
export class MetamodelPartialTypeSystem<Specifics extends TypirLangiumSpecifics> extends PartialTypeSystem<
    Specifics,
    undefined
> {
    override registerRules(): void {
        this.registerClassRules();
        this.registerEnumRules();
    }

    /**
     * Gets the absolute path from a node's document.
     */
    private getAbsolutePathFromNode(node: AstNode): string {
        const document = AstUtils.getDocument(node);
        return document.uri.path;
    }

    /**
     * Registers type inference rules for Metamodel Class nodes.
     */
    private registerClassRules(): void {
        this.registerInferenceRule(Class, (node) => {
            const absolutePath = this.getAbsolutePathFromNode(node);
            const classPackage = getClassPackage(absolutePath);
            const classType = this.typir.TypeDefinitions.getClassTypeIfExisting(node.name, classPackage);
            if (classType == undefined) {
                return <InferenceProblem<Specifics>>{
                    $problem: this.inferenceProblem,
                    languageNode: node,
                    location: "Class cannot be resolved",
                    subProblems: []
                };
            }
            const typeRef: ClassTypeRef = {
                package: classPackage,
                type: node.name,
                isNullable: false
            };
            return this.typir.TypeDefinitions.resolveCustomClassOrLambdaType(typeRef);
        });
    }

    /**
     * Registers type inference rules for Metamodel Enum nodes.
     */
    private registerEnumRules(): void {
        this.registerInferenceRule(Enum, (node) => {
            const absolutePath = this.getAbsolutePathFromNode(node);
            const enumPackage = getEnumPackage(absolutePath);
            const enumType = this.typir.TypeDefinitions.getClassTypeIfExisting(node.name, enumPackage);
            if (enumType == undefined) {
                return <InferenceProblem<Specifics>>{
                    $problem: this.inferenceProblem,
                    languageNode: node,
                    location: "Enum cannot be resolved",
                    subProblems: []
                };
            }
            const typeRef: ClassTypeRef = {
                package: enumPackage,
                type: node.name,
                isNullable: false
            };
            return this.typir.TypeDefinitions.resolveCustomClassOrLambdaType(typeRef);
        });
    }
}
