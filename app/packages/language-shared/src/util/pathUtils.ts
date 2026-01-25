/**
 * Utility functions for path manipulation.
 */

/**
 * Calculates the relative path from one file to another.
 *
 * @param fromPath Absolute path of the source file
 * @param toPath Absolute path of the target file
 * @returns Relative path from source file to target file
 */
export function calculateRelativePath(fromPath: string, toPath: string): string {
    const fromParts = fromPath.split("/");
    const toParts = toPath.split("/");

    fromParts.pop();

    let commonLength = 0;
    const maxLength = Math.min(fromParts.length, toParts.length);
    while (commonLength < maxLength && fromParts[commonLength] === toParts[commonLength]) {
        commonLength++;
    }

    const upLevels = fromParts.length - commonLength;
    const relativeParts: string[] = ["."];

    for (let i = 0; i < upLevels; i++) {
        relativeParts.push("..");
    }

    relativeParts.push(...toParts.slice(commonLength));

    return relativeParts.join("/");
}
