import type { Action } from "@eclipse-glsp/protocol";

/**
 * GLSP action dispatched from a graphical editor client to the diagram server to request
 * that the textual source corresponding to a graphical element is revealed in the Monaco editor.
 *
 * <p>The server resolves the AST node whose ID matches {@link elementId}, extracts the CST
 * source range, and forwards it to the workbench via the
 * {@code textDocument/revealSource} LSP notification.
 */
export interface RevealSourceAction extends Action {
    kind: typeof RevealSourceAction.KIND;
    /**
     * ID of the graphical element whose textual source should be revealed.
     */
    elementId: string;
}

/**
 * Namespace for {@link RevealSourceAction}.
 */
export namespace RevealSourceAction {
    /**
     * Discriminant string for {@link RevealSourceAction}.
     */
    export const KIND = "revealSource" as const;

    /**
     * Type guard for {@link RevealSourceAction}.
     *
     * @param action The action to test.
     * @returns `true` when {@code action} is a {@link RevealSourceAction}.
     */
    export function is(action: Action): action is RevealSourceAction {
        return action.kind === KIND;
    }

    /**
     * Creates a new {@link RevealSourceAction}.
     *
     * @param elementId The ID of the graphical element to reveal.
     * @returns A new {@link RevealSourceAction}.
     */
    export function create(elementId: string): RevealSourceAction {
        return { kind: KIND, elementId };
    }
}
