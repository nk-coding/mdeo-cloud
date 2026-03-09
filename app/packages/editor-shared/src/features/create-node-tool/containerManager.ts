import type { Point } from "@eclipse-glsp/protocol";
import { sharedImport } from "../../sharedImport.js";
import type { GModelElement } from "@eclipse-glsp/sprotty";
import type { ContainerElement, InsertOptions, TrackedInsert } from "@eclipse-glsp/client";

const { injectable } = sharedImport("inversify");
const { ContainerManager: GLSPContainerManager } = sharedImport("@eclipse-glsp/client");
const { findChildrenAtPosition, isContainable, CSS_GHOST_ELEMENT, DEFAULT_INSERT_OPTIONS } =
    sharedImport("@eclipse-glsp/client");
const { translatePoint } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Custom container manager that automatically considers all elements at the drop position as potential containers, instead of just the topmost one.
 */
@injectable()
export class ContainerManager extends GLSPContainerManager {
    override insert(
        proxy: GModelElement,
        location: Point,
        elementTypeId: string,
        opts?: Partial<InsertOptions>
    ): TrackedInsert {
        const options = { ...DEFAULT_INSERT_OPTIONS, ...opts };
        const container = this.findContainerExtended(location, proxy, elementTypeId);
        let valid = true;
        if (options.validateLocationInContainer) {
            valid = opts?.validLocationOverwrite ?? this.changeBoundsManager.hasValidPosition(proxy, location);
        }
        if (container == undefined) {
            valid = false;
        }
        let relativeLocation = location;
        if (container != undefined) {
            const translatedPoint = translatePoint(location, proxy.root, container);
            relativeLocation = {
                x: translatedPoint.x,
                y: translatedPoint.y
            };
        }

        return { elementTypeId, container, location: relativeLocation, valid, options };
    }

    /**
     * Finds a container for the given location and context by considering all elements at the position as potential containers.
     *
     * @param location The location for which to find a container.
     * @param ctx The context element to start the search from.
     * @param elementTypeId The type of the element to be created, used for validating potential containers.
     * @returns The found container element or undefined if no suitable container is found.
     */
    protected findContainerExtended(
        location: Point,
        ctx: GModelElement,
        elementTypeId: string
    ): ContainerElement | undefined {
        const elements = [ctx.root, ...findChildrenAtPosition(ctx.root, location)];
        return elements
            .reverse()
            .find(
                (element) =>
                    isContainable(element) &&
                    !element.cssClasses?.includes(CSS_GHOST_ELEMENT) &&
                    element.isContainableElement(elementTypeId)
            ) as ContainerElement | undefined;
    }
}
