import { createTerminal } from "@mdeo/language-common";

export const ID = createTerminal("ID")
    .returns(String)
    .as(/`[^`\n\r]+`|[\p{ID_Start}][\p{ID_Continue}]*/u);

export const INT = createTerminal("INT")
    .returns(Number)
    .as(/[0-9]+/);

export const FLOAT = createTerminal("FLOAT")
    .returns(Number)
    .as(/[0-9]+\.[0-9]+/);

export const WS = createTerminal("WS").hidden().as(/\s+/);
export const ML_COMMENT = createTerminal("ML_COMMENT")
    .hidden()
    .as(/\/\*[\s\S]*?\*\//);
export const SL_COMMENT = createTerminal("SL_COMMENT")
    .hidden()
    .as(/\/\/[^\n\r]*/);
