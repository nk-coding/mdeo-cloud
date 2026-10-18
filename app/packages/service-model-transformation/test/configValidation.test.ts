import { describe, it } from "node:test";
import assert from "node:assert/strict";
import { TestWorkspace } from "./testWorkspace.ts";
import { layeredProject } from "./fixtures.ts";

const { mdeoDiagnosticsRequestHandler } = await import("../../service-config-mdeo/dist/handler/mdeoRequestHandler.js");
const { ServiceMdeoMetamodelResolver } = await import("../../service-config-mdeo/dist/serviceMdeoMetamodelResolver.js");

const resolverModule = { MdeoMetamodelResolver: () => new ServiceMdeoMetamodelResolver() };

function search(...operators: string[]): string {
    return `search {
    mutations {
${operators.map((o) => `        using "${o}"`).join("\n")}
    }
}
`;
}

describe("configurations using callable transformations as operators", () => {
    const workspace = new TestWorkspace(layeredProject());

    it("accepts a wrapper and a callee whose inputs all have defaults", async () => {
        const errors = await workspace.configDiagnostics(
            mdeoDiagnosticsRequestHandler,
            resolverModule,
            "/optimize/shop.config",
            search("./move-with-stores.mt", "./generic/move-component.mt"),
            "/metamodel/shop.mm"
        );
        assert.deepEqual(errors, []);
    });

    it("rejects an operator with required inputs", async () => {
        const errors = await workspace.configDiagnostics(
            mdeoDiagnosticsRequestHandler,
            resolverModule,
            "/optimize/shop.config",
            search("./sync/follow-reader.mt"),
            "/metamodel/shop.mm"
        );
        assert.deepEqual(errors, [
            "The transformation 'followReader' at './sync/follow-reader.mt' has required inputs 'moved', 'target', so it cannot be a mutation operator: the optimizer passes no arguments. Give them defaults, or call it from an operator with 'call'."
        ]);
    });
});
