import type { GModelFactory } from "@eclipse-glsp/server";
import { sharedImport } from "@mdeo/language-shared";

const { injectable } = sharedImport("inversify");

@injectable()
export class MetamodelGModelFactory implements GModelFactory {
    createModel(): void {
        throw new Error("Method not implemented.");
    }
}
