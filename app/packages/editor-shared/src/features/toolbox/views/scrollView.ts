import type { VNode } from "snabbdom";
import { sharedImport } from "../../../sharedImport.js";

const { html } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Sizes for scroll area calculations.
 */
interface Sizes {
    content: number;
    viewport: number;
    scrollbar: {
        size: number;
        paddingStart: number;
        paddingEnd: number;
    };
}

/**
 * State class for managing scroll behavior.
 * Should be instantiated once per scroll area and reused across updates.
 */
export class ScrollViewState {
    sizes: Sizes = {
        content: 0,
        viewport: 0,
        scrollbar: { size: 0, paddingStart: 0, paddingEnd: 0 }
    };
    hasThumb: boolean = false;
    thumbElement: HTMLElement | null = null;
    viewportElement: HTMLElement | null = null;
    contentElement: HTMLElement | null = null;
    scrollbarElement: HTMLElement | null = null;
    visible: boolean = false;
    pointerOffset: number = 0;
    resizeObserver: ResizeObserver | null = null;
    scrollListener: (() => void) | null = null;
    hoverTimeout: number | null = null;
}

/**
 * Creates a scroll view with a custom scrollbar.
 * This is a simplified implementation based on reka-ui ScrollArea,
 * supporting only vertical scrolling with hover visibility.
 *
 * @param state The scroll view state instance to use
 * @param contentGenerator Function that generates the content VNodes
 * @param containerClasses Additional classes for the viewport container
 * @returns The scroll area VNode
 */
export function generateScrollView(
    state: ScrollViewState,
    contentGenerator: () => VNode[],
    containerClasses: Record<string, boolean> = {}
): VNode {
    return generateScrollAreaRoot(state, contentGenerator, containerClasses);
}

/**
 * Generates the root scroll area container.
 */
function generateScrollAreaRoot(
    state: ScrollViewState,
    contentGenerator: () => VNode[],
    containerClasses: Record<string, boolean>
): VNode {
    return html(
        "div",
        {
            class: {
                relative: true,
                ...containerClasses
            },
            hook: {
                insert: (vnode) => {
                    setupScrollArea(state, vnode.elm as HTMLElement);
                },
                destroy: () => {
                    cleanupScrollArea(state);
                }
            }
        },
        generateViewport(state, contentGenerator),
        generateScrollbar(state)
    );
}

/**
 * Generates the viewport element that contains scrollable content.
 */
function generateViewport(state: ScrollViewState, contentGenerator: () => VNode[]): VNode {
    return html(
        "div",
        {
            class: {
                "size-full": true,
                "rounded-[inherit]": true,
                "outline-none": true,
                "no-scrollbar": true
            },
            style: {
                "overflow-x": "hidden",
                "overflow-y": "scroll"
            },
            attrs: {
                tabindex: "0"
            },
            hook: {
                insert: (vnode) => {
                    state.viewportElement = vnode.elm as HTMLElement;
                    setupViewport(state, vnode.elm as HTMLElement);
                }
            }
        },
        html(
            "div",
            {
                hook: {
                    insert: (vnode) => {
                        state.contentElement = vnode.elm as HTMLElement;
                    }
                }
            },
            ...contentGenerator()
        )
    );
}

/**
 * Generates the scrollbar element.
 */
function generateScrollbar(state: ScrollViewState): VNode {
    return html(
        "div",
        {
            class: {
                flex: true,
                "touch-none": true,
                "select-none": true,
                "p-px": true,
                "transition-colors": true,
                "h-full": true,
                "w-2.5": true,
                "border-l": true,
                "border-l-transparent": true,
                absolute: true,
                "top-0": true,
                "right-0": true,
                "opacity-0": true,
                "pointer-events-none": true,
                "transition-opacity": true,
                "duration-200": true
            },
            attrs: {
                "data-orientation": "vertical"
            },
            hook: {
                insert: (vnode) => {
                    state.scrollbarElement = vnode.elm as HTMLElement;
                    setupScrollbar(state, vnode.elm as HTMLElement);
                }
            }
        },
        generateThumb(state)
    );
}

/**
 * Generates the scrollbar thumb element.
 */
function generateThumb(state: ScrollViewState): VNode {
    return html("div", {
        class: {
            "bg-border": true,
            relative: true,
            "flex-1": true,
            "rounded-full": true
        },
        hook: {
            insert: (vnode) => {
                state.thumbElement = vnode.elm as HTMLElement;
            }
        },
        on: {
            pointerdown: (event: PointerEvent) => {
                handleThumbPointerDown(state, event);
            },
            pointerup: () => {
                handleThumbPointerUp(state);
            }
        }
    });
}

/**
 * Sets up the scroll area with hover and resize detection.
 */
function setupScrollArea(state: ScrollViewState, rootElement: HTMLElement): void {
    rootElement.addEventListener("pointerenter", () => {
        if (state.hoverTimeout !== null) {
            window.clearTimeout(state.hoverTimeout);
        }
        updateScrollbarVisibility(state, true);
    });

    rootElement.addEventListener("pointerleave", () => {
        state.hoverTimeout = window.setTimeout(() => {
            updateScrollbarVisibility(state, false);
            state.hoverTimeout = null;
        }, 600);
    });
}

/**
 * Sets up the viewport with scroll and resize observers.
 */
function setupViewport(state: ScrollViewState, viewportElement: HTMLElement): void {
    state.resizeObserver = new ResizeObserver(() => {
        handleResize(state);
        if (state.hasThumb && !state.scrollListener) {
            setupScrollListener(state);
        }
    });

    state.resizeObserver.observe(viewportElement);

    Promise.resolve().then(() => {
        if (state.contentElement) {
            state.resizeObserver!.observe(state.contentElement);
        }
    });

    setTimeout(() => handleResize(state), 0);
}

/**
 * Sets up the scrollbar with pointer events.
 */
function setupScrollbar(state: ScrollViewState, scrollbarElement: HTMLElement): void {
    scrollbarElement.addEventListener("pointerdown", (event: PointerEvent) => {
        if (event.button === 0 && event.target === scrollbarElement) {
            handleScrollbarClick(state, event);
        }
    });
}

/**
 * Handles resize events to update overflow detection and sizes.
 */
function handleResize(state: ScrollViewState): void {
    if (!state.viewportElement || !state.contentElement || !state.scrollbarElement) {
        return;
    }

    const isOverflowY = state.viewportElement.offsetHeight < state.viewportElement.scrollHeight;

    state.hasThumb = isOverflowY;

    if (isOverflowY) {
        const scrollbarStyles = getComputedStyle(state.scrollbarElement);
        state.sizes = {
            content: state.viewportElement.scrollHeight,
            viewport: state.viewportElement.offsetHeight,
            scrollbar: {
                size: state.scrollbarElement.clientHeight,
                paddingStart: toInt(scrollbarStyles.paddingTop),
                paddingEnd: toInt(scrollbarStyles.paddingBottom)
            }
        };

        updateThumbSize(state);
        updateThumbPosition(state);
    }
}

/**
 * Sets up the scroll listener with unlinked rAF loop.
 * This should only be called once when the thumb becomes visible.
 */
function setupScrollListener(state: ScrollViewState): void {
    if (!state.viewportElement || state.scrollListener) {
        return;
    }

    state.scrollListener = addUnlinkedScrollListener(state.viewportElement, () => {
        updateThumbPosition(state);
    });

    updateThumbPosition(state);
}

/**
 * Updates the scrollbar visibility.
 */
function updateScrollbarVisibility(state: ScrollViewState, visible: boolean): void {
    if (!state.scrollbarElement || !state.hasThumb) {
        return;
    }

    state.visible = visible;
    if (visible) {
        state.scrollbarElement.classList.remove("opacity-0", "pointer-events-none");
        state.scrollbarElement.classList.add("opacity-100");
    } else {
        state.scrollbarElement.classList.remove("opacity-100");
        state.scrollbarElement.classList.add("opacity-0", "pointer-events-none");
    }
}

/**
 * Updates the thumb size based on content and viewport sizes.
 */
function updateThumbSize(state: ScrollViewState): void {
    if (!state.thumbElement || !state.hasThumb) {
        return;
    }

    const thumbSize = getThumbSize(state.sizes);
    state.thumbElement.style.height = `${thumbSize}px`;
}

/**
 * Updates the thumb position based on scroll position.
 */
function updateThumbPosition(state: ScrollViewState): void {
    if (!state.thumbElement || !state.viewportElement || !state.hasThumb) {
        return;
    }

    const scrollPos = state.viewportElement.scrollTop;
    const offset = getThumbOffsetFromScroll(scrollPos, state.sizes);
    state.thumbElement.style.transform = `translate3d(0, ${offset}px, 0)`;
}

/**
 * Handles pointer down on thumb.
 */
function handleThumbPointerDown(state: ScrollViewState, event: PointerEvent): void {
    if (!state.thumbElement) {
        return;
    }

    const thumbRect = state.thumbElement.getBoundingClientRect();
    const y = event.clientY - thumbRect.top;
    state.pointerOffset = y;

    const element = event.target as HTMLElement;
    element.setPointerCapture(event.pointerId);

    const prevWebkitUserSelect = document.body.style.webkitUserSelect;
    document.body.style.webkitUserSelect = "none";
    if (state.viewportElement) {
        state.viewportElement.style.scrollBehavior = "auto";
    }

    const handlePointerMove = (moveEvent: PointerEvent) => {
        if (!state.scrollbarElement || !state.viewportElement) {
            return;
        }

        const rect = state.scrollbarElement.getBoundingClientRect();
        const y = moveEvent.clientY - rect.top;
        const scrollPos = getScrollPositionFromPointer(y, state.pointerOffset, state.sizes);
        state.viewportElement.scrollTop = scrollPos;
    };

    const handlePointerUp = (upEvent: PointerEvent) => {
        const element = upEvent.target as HTMLElement;
        if (element.hasPointerCapture(upEvent.pointerId)) {
            element.releasePointerCapture(upEvent.pointerId);
        }

        document.body.style.webkitUserSelect = prevWebkitUserSelect;
        if (state.viewportElement) {
            state.viewportElement.style.scrollBehavior = "";
        }

        document.removeEventListener("pointermove", handlePointerMove);
        document.removeEventListener("pointerup", handlePointerUp);
    };

    document.addEventListener("pointermove", handlePointerMove);
    document.addEventListener("pointerup", handlePointerUp);
}

/**
 * Handles pointer up on thumb.
 */
function handleThumbPointerUp(state: ScrollViewState): void {
    state.pointerOffset = 0;
}

/**
 * Handles click on scrollbar track.
 */
function handleScrollbarClick(state: ScrollViewState, event: PointerEvent): void {
    if (!state.scrollbarElement || !state.viewportElement) {
        return;
    }

    const rect = state.scrollbarElement.getBoundingClientRect();
    const y = event.clientY - rect.top;
    const thumbSize = getThumbSize(state.sizes);
    const scrollPos = getScrollPositionFromPointer(y, thumbSize / 2, state.sizes);
    state.viewportElement.scrollTop = scrollPos;
}

/**
 * Cleans up resources when the scroll area is destroyed.
 */
function cleanupScrollArea(state: ScrollViewState): void {
    if (state.resizeObserver) {
        state.resizeObserver.disconnect();
        state.resizeObserver = null;
    }

    if (state.scrollListener) {
        state.scrollListener();
        state.scrollListener = null;
    }

    if (state.hoverTimeout !== null) {
        window.clearTimeout(state.hoverTimeout);
        state.hoverTimeout = null;
    }
}

/**
 * Converts a string value to an integer.
 */
function toInt(value?: string): number {
    return value ? Number.parseInt(value, 10) : 0;
}

/**
 * Calculates the thumb size based on viewport and content sizes.
 */
function getThumbSize(sizes: Sizes): number {
    const ratio = getThumbRatio(sizes.viewport, sizes.content);
    const scrollbarPadding = sizes.scrollbar.paddingStart + sizes.scrollbar.paddingEnd;
    const thumbSize = (sizes.scrollbar.size - scrollbarPadding) * ratio;
    return Math.max(thumbSize, 18);
}

/**
 * Calculates the ratio of viewport to content size.
 */
function getThumbRatio(viewportSize: number, contentSize: number): number {
    const ratio = viewportSize / contentSize;
    return Number.isNaN(ratio) ? 0 : ratio;
}

/**
 * Calculates thumb offset from scroll position.
 */
function getThumbOffsetFromScroll(scrollPos: number, sizes: Sizes): number {
    const thumbSizePx = getThumbSize(sizes);
    const scrollbarPadding = sizes.scrollbar.paddingStart + sizes.scrollbar.paddingEnd;
    const scrollbar = sizes.scrollbar.size - scrollbarPadding;
    const maxScrollPos = sizes.content - sizes.viewport;
    const maxThumbPos = scrollbar - thumbSizePx;
    const scrollWithoutMomentum = clamp(scrollPos, 0, maxScrollPos);
    const interpolate = linearScale([0, maxScrollPos], [0, maxThumbPos]);
    return interpolate(scrollWithoutMomentum);
}

/**
 * Calculates scroll position from pointer position.
 */
function getScrollPositionFromPointer(pointerPos: number, pointerOffset: number, sizes: Sizes): number {
    const thumbSizePx = getThumbSize(sizes);
    const thumbCenter = thumbSizePx / 2;
    const offset = pointerOffset || thumbCenter;
    const thumbOffsetFromEnd = thumbSizePx - offset;
    const minPointerPos = sizes.scrollbar.paddingStart + offset;
    const maxPointerPos = sizes.scrollbar.size - sizes.scrollbar.paddingEnd - thumbOffsetFromEnd;
    const maxScrollPos = sizes.content - sizes.viewport;
    const interpolate = linearScale([minPointerPos, maxPointerPos], [0, maxScrollPos]);
    return interpolate(pointerPos);
}

/**
 * Creates a linear scale function for mapping values.
 */
function linearScale(input: readonly [number, number], output: readonly [number, number]): (value: number) => number {
    return (value: number) => {
        if (input[0] === input[1] || output[0] === output[1]) {
            return output[0];
        }
        const ratio = (output[1] - output[0]) / (input[1] - input[0]);
        return output[0] + ratio * (value - input[0]);
    };
}

/**
 * Clamps a value between min and max.
 */
function clamp(value: number, min: number, max: number): number {
    return Math.min(Math.max(value, min), max);
}

/**
 * Custom scroll handler to avoid scroll-linked effects.
 * Uses requestAnimationFrame to efficiently track scroll position changes.
 */
function addUnlinkedScrollListener(node: HTMLElement, handler: () => void): () => void {
    let prevPosition = { left: node.scrollLeft, top: node.scrollTop };
    let rAF = 0;

    function loop() {
        const position = { left: node.scrollLeft, top: node.scrollTop };
        const isVerticalScroll = prevPosition.top !== position.top;
        if (isVerticalScroll) {
            handler();
        }
        prevPosition = position;
        rAF = window.requestAnimationFrame(loop);
    }

    loop();
    return () => window.cancelAnimationFrame(rAF);
}
