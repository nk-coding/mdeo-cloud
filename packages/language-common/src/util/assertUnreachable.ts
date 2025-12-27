/**
 * Throws an exception if called, indicating that an unreachable code path has been reached.
 * 
 * @param _ the value that should not be reachable
 */
export function assertUnreachable(_: never): never {
    throw new Error('Error: Got unexpected value.');
}