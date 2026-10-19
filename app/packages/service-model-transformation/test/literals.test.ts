import { describe, it } from "node:test";
import assert from "node:assert/strict";
import { TestWorkspace } from "./testWorkspace.ts";
import { layeredProject } from "./fixtures.ts";

const { DOUBLE_LITERAL, FLOAT_LITERAL, INT_LITERAL, LONG_LITERAL } = await import("@mdeo/language-expression");
const { FLOAT, INT } = await import("@mdeo/language-common");
const { typedAstHandler } = await import("../dist/handler/typedAstHandler.js");

/**
 * The whole-input regular expression of a terminal (its grammar node keeps the pattern as text).
 */
function whole(terminal: { toRule(): any }): RegExp {
    const text: string = terminal.toRule().definition.regex;
    const source = text.slice(text.indexOf("/") + 1, text.lastIndexOf("/"));
    return new RegExp(`^(?:${source})$`, "u");
}

describe("number literals", () => {
    it("lex exponents in the expression tokens of the script and transformation languages", () => {
        for (const literal of ["1.0e300", "2.5E-3", "1e3", "1E+3", "0.5", "3.25e-12d", "1.0e300D"]) {
            assert.match(literal, whole(DOUBLE_LITERAL as any), literal);
        }
        for (const literal of ["1.5f", "1.0e30F", "2e-3f"]) {
            assert.match(literal, whole(FLOAT_LITERAL as any), literal);
        }
        for (const literal of ["1", "1e", "1.", ".5", "1.e3", "1e3L"]) {
            assert.doesNotMatch(literal, whole(DOUBLE_LITERAL as any), literal);
        }
        assert.match("42", whole(INT_LITERAL as any));
        assert.match("42L", whole(LONG_LITERAL as any));
    });

    it("lex exponents in the shared FLOAT token of models and configurations, as JavaScript prints numbers", () => {
        for (const value of [1e300, 2.5e-7, -1.5, 0.25, -3e-12]) {
            assert.match(String(value), whole(FLOAT as any), String(value));
        }
        assert.match("1.0e300", whole(FLOAT as any));
        assert.doesNotMatch("1.0e300", whole(INT as any));
    });

    it("parse in a transformation, with the value as written", async () => {
        const workspace = new TestWorkspace(
            layeredProject({
                "/optimize/literals.mt": `using "../metamodel/base.mm"

match {
    c: Container { }
    var huge = 1.0e300
    var small = 2.5E-3f
    var plain = 1e3
    where huge > 1.0e299 && small < 1.0f && plain == 1000.0
}
`
            })
        );
        const { data, errors } = await workspace.run(typedAstHandler, "/optimize/literals.mt");
        assert.deepEqual(errors, []);
        const values = JSON.stringify(data).match(/"value":"[0-9][^"]*"/g);
        assert.deepEqual(values?.slice(0, 3), ['"value":"1.0e300"', '"value":"2.5E-3f"', '"value":"1e3"']);
    });
});
