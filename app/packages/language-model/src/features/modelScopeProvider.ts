import { type AstReflection } from "@mdeo/language-common";
import { getExportetEntitiesFromRelativeFile, sharedImport } from "@mdeo/language-shared";
import type { ReferenceInfo, Scope } from "langium";
import { ObjectInstance, PropertyAssignment, type ModelType, type ObjectInstanceType } from "../grammar/modelTypes.js";
import { Class, ClassImport, resolveClassChain, type ClassImportType, type ClassType } from "@mdeo/language-metamodel";

const { DefaultScopeProvider, AstUtils, EMPTY_SCOPE } = sharedImport("langium");

/**
 * The scope provider for the Model language
 */
export class ModelScopeProvider extends DefaultScopeProvider {
    override getScope(context: ReferenceInfo): Scope {
        const document = AstUtils.getDocument(context.container);
        const reflection = this.reflection as AstReflection;

        if (context.property === "class" && reflection.isInstance(context.container, ObjectInstance)) {
            const model = context.container.$container as ModelType;
            const metamodelImport = model.import;
            return getExportetEntitiesFromRelativeFile<ClassType | ClassImportType>(
                document,
                metamodelImport.file,
                [Class, ClassImport],
                this.indexManager
            );
        } else if (context.property === "name" && reflection.isInstance(context.container, PropertyAssignment)) {
            let objectInstance = context.container.$container as ObjectInstanceType;
            // workaround for langium issue that will hopefully be fixed soon
            if (reflection.isInstance(objectInstance, PropertyAssignment)) {
                objectInstance = objectInstance.$container as ObjectInstanceType;
            }
            const classRef = objectInstance?.class?.ref;
            if (!classRef) {
                return EMPTY_SCOPE;
            }
            const classChain = resolveClassChain(classRef, reflection);
            return this.createScopeForNodes(classChain.flatMap((cls) => cls.properties));
        }
        return EMPTY_SCOPE;
    }
}
