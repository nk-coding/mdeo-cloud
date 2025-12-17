import { createTerminal } from "../grammar/rule/terminal/factory.js";

/**
 * Identifier token
 */
export const ID = createTerminal("ID")
    .returns(String)
    .as(/`[^`\n\r]+`|[\p{ID_Start}][\p{ID_Continue}]*/u);

/**
 * Integer token
 */
export const INT = createTerminal("INT")
    .returns(Number)
    .as(/[0-9]+/);

/**
 * Float token
 */
export const FLOAT = createTerminal("FLOAT")
    .returns(Number)
    .as(/[0-9]+\.[0-9]+/);

/**
 * String token
 */
export const STRING = createTerminal("STRING")
    .returns(String)
    .as(/"([^"\\\n]|\\(["\\nt]|u[0-9a-fA-F]{4}))*"/);

/**
 * Whitespace token
 */
export const WS = createTerminal("WS").hidden().as(/\s+/);

/**
 * Multi-line comment token
 */
export const ML_COMMENT = createTerminal("ML_COMMENT")
    .hidden()
    .as(/\/\*[\s\S]*?\*\//);

/**
 *  Single-line comment token
 */
export const SL_COMMENT = createTerminal("SL_COMMENT")
    .hidden()
    .as(/\/\/[^\n\r]*/);
