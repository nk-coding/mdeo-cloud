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

    // Remove the filename from the source path
    fromParts.pop();

    // Find the common prefix length
    let commonLength = 0;
    const maxLength = Math.min(fromParts.length, toParts.length);
    while (commonLength < maxLength && fromParts[commonLength] === toParts[commonLength]) {
        commonLength++;
    }

    // Calculate how many levels up we need to go
    const upLevels = fromParts.length - commonLength;
    const relativeParts: string[] = ["."];

    // Add ".." for each level up
    for (let i = 0; i < upLevels; i++) {
        relativeParts.push("..");
    }

    // Add the remaining parts of the target path
    relativeParts.push(...toParts.slice(commonLength));

    return relativeParts.join("/");
}
