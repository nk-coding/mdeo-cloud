import type { TypirLangiumSpecifics } from "typir-langium";
import { PartialTypeSystem } from "./partialTypeSystem.js";
import { Class, Enum } from "@mdeo/language-metamodel";
import { getPackage } from "../features/metamodel/metamodelClassExtractor.js";
import type { InferenceProblem } from "typir";
import type { ClassTypeRef } from "../typir-extensions/config/type.js";

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
     * Registers type inference rules for Metamodel Class nodes.
     */
    private registerClassRules(): void {
        this.registerInferenceRule(Class, (node) => {
            const classPackage = getPackage(node);
            const className = `${classPackage}.${node.name}`;
            const classType = this.typir.TypeDefinitions.getClassTypeIfExisting(className);
            if (classType == undefined) {
                return <InferenceProblem<Specifics>>{
                    $problem: this.inferenceProblem,
                    languageNode: node,
                    location: "Class cannot be resolved",
                    subProblems: []
                };
            }
            const typeRef: ClassTypeRef = {
                type: className,
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
            const enumPackage = getPackage(node);
            const enumName = `${enumPackage}.${node.name}`;
            const enumType = this.typir.TypeDefinitions.getClassTypeIfExisting(enumName);
            if (enumType == undefined) {
                return <InferenceProblem<Specifics>>{
                    $problem: this.inferenceProblem,
                    languageNode: node,
                    location: "Enum cannot be resolved",
                    subProblems: []
                };
            }
            const typeRef: ClassTypeRef = {
                type: enumName,
                isNullable: false
            };
            return this.typir.TypeDefinitions.resolveCustomClassOrLambdaType(typeRef);
        });
    }
}
