import { createModule } from "@mdeo/language-common";
import { MetaModelRule } from "./rules.js";
import { ID, INT, FLOAT } from "./terminals.js";

export const MetaModelGrammar = createModule(MetaModelRule, [
    ID,
    INT,
    FLOAT
]);