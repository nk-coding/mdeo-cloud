import { describe, it } from "node:test";
import assert from "node:assert/strict";

const { multiplicityBounds, formatBounds, refinementBoundsProblem, repeatedRefinements } =
    await import("../dist/validation/refinementBounds.js");

const single = (value: string, numericValue?: number) => ({ $type: "SingleMultiplicity", value, numericValue });
const range = (lower: number, upper: string, upperNumeric?: number) => ({
    $type: "RangeMultiplicity",
    lower,
    upper,
    upperNumeric
});

describe("multiplicityBounds", () => {
    it("reads every way of writing a multiplicity", () => {
        assert.deepEqual(multiplicityBounds(undefined), { lower: 1, upper: 1 });
        assert.deepEqual(multiplicityBounds(single("*")), { lower: 0, upper: -1 });
        assert.deepEqual(multiplicityBounds(single("+")), { lower: 1, upper: -1 });
        assert.deepEqual(multiplicityBounds(single("?")), { lower: 0, upper: 1 });
        assert.deepEqual(multiplicityBounds(single("3", 3)), { lower: 3, upper: 3 });
        assert.deepEqual(multiplicityBounds(range(2, "*")), { lower: 2, upper: -1 });
        assert.deepEqual(multiplicityBounds(range(1, "4", 4)), { lower: 1, upper: 4 });
    });

    it("formats bounds as the optimizer reports them", () => {
        assert.equal(formatBounds({ lower: 0, upper: -1 }), "[0..*]");
        assert.equal(formatBounds({ lower: 2, upper: 2 }), "[2..2]");
    });
});

describe("refinementBoundsProblem", () => {
    const declared = { lower: 0, upper: 5 };

    it("accepts bounds within the metamodel's, including equal ones", () => {
        for (const refined of [
            { lower: 0, upper: 5 },
            { lower: 1, upper: 5 },
            { lower: 2, upper: 3 },
            { lower: 5, upper: 5 }
        ]) {
            assert.equal(refinementBoundsProblem(refined, declared), undefined, JSON.stringify(refined));
        }
        assert.equal(refinementBoundsProblem({ lower: 1, upper: 1 }, { lower: 0, upper: 1 }), undefined);
        assert.equal(refinementBoundsProblem({ lower: 4, upper: -1 }, { lower: 1, upper: -1 }), undefined);
        assert.equal(refinementBoundsProblem({ lower: 1, upper: 9 }, { lower: 0, upper: -1 }), undefined);
    });

    it("rejects bounds that loosen the metamodel's", () => {
        assert.equal(
            refinementBoundsProblem({ lower: 0, upper: 6 }, declared),
            "A refinement can only tighten bounds: [0..6] does not lie within the metamodel's [0..5]."
        );
        assert.match(
            refinementBoundsProblem({ lower: 0, upper: -1 }, declared) ?? "",
            /\[0\.\.\*\] does not lie within/
        );
        assert.match(
            refinementBoundsProblem({ lower: 0, upper: 3 }, { lower: 1, upper: -1 }) ?? "",
            /does not lie within the metamodel's \[1\.\.\*\]/
        );
    });

    it("rejects an end that could hold no link", () => {
        assert.equal(
            refinementBoundsProblem({ lower: 0, upper: 0 }, declared),
            "A refined end must allow at least one link."
        );
    });
});

describe("repeatedRefinements", () => {
    it("reports every refinement of an end refined before", () => {
        assert.deepEqual(repeatedRefinements(["A.e", "B.f", "A.e", "A.g", "A.e"]), [2, 4]);
        assert.deepEqual(repeatedRefinements(["A.e", "B.e"]), []);
    });
});
