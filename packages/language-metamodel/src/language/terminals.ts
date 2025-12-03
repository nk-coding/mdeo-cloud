import { createTerminal } from "@mdeo/language-common";

export const ID = createTerminal("ID")
    .returns(String)
    .as(/`[^`\n\r]*`|[\p{ID_Start}][\p{ID_Continue}]*/u);

export const INT = createTerminal("INT")
    .returns(Number)
    .as(/[0-9]+/);

export const FLOAT = createTerminal("FLOAT")
    .returns(Number)
    .as(/[0-9]+\.[0-9]+/);