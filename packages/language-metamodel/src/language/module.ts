import { createModule } from "@mdeo/language-common";
import { MetaModelRule } from "./rules.js";
import { WS, ML_COMMENT, SL_COMMENT } from "./terminals.js";

export const MetaModelGrammar = createModule(MetaModelRule, [WS, ML_COMMENT, SL_COMMENT]);
