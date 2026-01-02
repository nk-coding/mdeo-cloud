import type { GModelFactory } from "@eclipse-glsp/server";
import type { PluginContext } from "@mdeo/language-common";
import type { MetamodelServiceProvider } from "../../plugin.js";

export function generateGModelFactory(context: PluginContext) {
    const { inversify, ["@eclipse-glsp/server"]: glspServer } = context;

    @inversify.injectable()
    class MetamodelGModelFactory implements GModelFactory {

        createModel(): void {
            throw new Error("Method not implemented.");
        }

    }
}