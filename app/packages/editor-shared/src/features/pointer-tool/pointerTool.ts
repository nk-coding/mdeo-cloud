import type { Action } from "@eclipse-glsp/protocol";
import type { VNode } from "snabbdom";
import { sharedImport } from "../../sharedImport.js";
import type {
    DOMHelper,
    GModelElement,
    GModelRoot as GModelRootType,
    IActionDispatcher,
    IVNodePostprocessor
} from "@eclipse-glsp/sprotty";

const { inject, injectable, multiInject, optional } = sharedImport("inversify");
const { on, TYPES, GModelRoot } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Symbol for pointer listener injection
 */
export const IPointerListener = Symbol("IPointerListener");

/**
 * Types of pointer events that can be handled
 */
export type PointerEventKind =
    | "pointerOver"
    | "pointerEnter"
    | "pointerDown"
    | "pointerMove"
    | "pointerUp"
    | "pointerCancel"
    | "pointerOut"
    | "pointerLeave"
    | "gotPointerCapture"
    | "lostPointerCapture";

/**
 * Interface for listeners that react to pointer events on diagram elements.
 * Implementations can return actions to be dispatched in response to pointer events.
 */
export interface IPointerListener {
    /**
     * Called when pointer moves over an element.
     *
     * @param target The target element
     * @param event The pointer event
     * @returns Array of actions or promises that resolve to actions
     */
    pointerOver(target: GModelElement, event: PointerEvent): (Action | Promise<Action>)[];

    /**
     * Called when pointer enters an element.
     *
     * @param target The target element
     * @param event The pointer event
     * @returns Array of actions or promises that resolve to actions
     */
    pointerEnter(target: GModelElement, event: PointerEvent): (Action | Promise<Action>)[];

    /**
     * Called when pointer button is pressed down on an element.
     *
     * @param target The target element
     * @param event The pointer event
     * @returns Array of actions or promises that resolve to actions
     */
    pointerDown(target: GModelElement, event: PointerEvent): (Action | Promise<Action>)[];

    /**
     * Called when pointer moves on an element.
     *
     * @param target The target element
     * @param event The pointer event
     * @returns Array of actions or promises that resolve to actions
     */
    pointerMove(target: GModelElement, event: PointerEvent): (Action | Promise<Action>)[];

    /**
     * Called when pointer button is released on an element.
     *
     * @param target The target element
     * @param event The pointer event
     * @returns Array of actions or promises that resolve to actions
     */
    pointerUp(target: GModelElement, event: PointerEvent): (Action | Promise<Action>)[];

    /**
     * Called when pointer event is cancelled.
     *
     * @param target The target element
     * @param event The pointer event
     * @returns Array of actions or promises that resolve to actions
     */
    pointerCancel(target: GModelElement, event: PointerEvent): (Action | Promise<Action>)[];

    /**
     * Called when pointer moves out of an element.
     *
     * @param target The target element
     * @param event The pointer event
     * @returns Array of actions or promises that resolve to actions
     */
    pointerOut(target: GModelElement, event: PointerEvent): (Action | Promise<Action>)[];

    /**
     * Called when pointer leaves an element.
     *
     * @param target The target element
     * @param event The pointer event
     * @returns Array of actions or promises that resolve to actions
     */
    pointerLeave(target: GModelElement, event: PointerEvent): (Action | Promise<Action>)[];

    /**
     * Called when element gains pointer capture.
     *
     * @param target The target element
     * @param event The pointer event
     * @returns Array of actions or promises that resolve to actions
     */
    gotPointerCapture(target: GModelElement, event: PointerEvent): (Action | Promise<Action>)[];

    /**
     * Called when element loses pointer capture.
     *
     * @param target The target element
     * @param event The pointer event
     * @returns Array of actions or promises that resolve to actions
     */
    lostPointerCapture(target: GModelElement, event: PointerEvent): (Action | Promise<Action>)[];
}

/**
 * Tool that manages pointer event listeners and dispatches their resulting actions.
 * Attaches pointer event handlers to the root SVG element and delegates to registered listeners.
 */
@injectable()
export class PointerTool implements IVNodePostprocessor {
    @inject(TYPES.IActionDispatcher) protected actionDispatcher!: IActionDispatcher;
    @inject(TYPES.DOMHelper) protected domHelper!: DOMHelper;

    /**
     * Creates a new pointer tool.
     *
     * @param pointerListeners Array of pointer listeners to handle events
     */
    constructor(@multiInject(IPointerListener) @optional() protected pointerListeners: IPointerListener[] = []) {}

    /**
     * Registers a new pointer listener.
     *
     * @param pointerListener The listener to register
     */
    register(pointerListener: IPointerListener): void {
        this.pointerListeners.push(pointerListener);
    }

    /**
     * Removes a pointer listener.
     *
     * @param pointerListener The listener to remove
     */
    deregister(pointerListener: IPointerListener): void {
        const index = this.pointerListeners.indexOf(pointerListener);
        if (index >= 0) {
            this.pointerListeners.splice(index, 1);
        }
    }

    /**
     * Finds the target element for a pointer event by traversing the DOM tree.
     *
     * @param model The root model
     * @param event The pointer event
     * @returns The target element or undefined if not found
     */
    protected getTargetElement(model: GModelRootType, event: PointerEvent): GModelElement | undefined {
        let target = event.target as Element;
        const index = model.index;
        while (target) {
            if (target.id) {
                const element = index.getById(this.domHelper.findSModelIdByDOMElement(target));
                if (element !== undefined) return element;
            }
            target = target.parentNode as Element;
        }
        return undefined;
    }

    /**
     * Handles a pointer event by delegating to all registered listeners.
     *
     * @param methodName The type of pointer event
     * @param model The root model
     * @param event The pointer event
     */
    protected handleEvent(methodName: PointerEventKind, model: GModelRootType, event: PointerEvent): void {
        const element = this.getTargetElement(model, event);
        if (!element) {
            return;
        }
        const actions = this.pointerListeners
            .map((listener) => listener[methodName](element, event))
            .reduce((a, b) => a.concat(b), []);
        if (actions.length > 0) {
            event.preventDefault();
            this.dispatchActions(actions);
        }
    }

    /**
     * Dispatches actions or action promises.
     *
     * @param actions Array of actions or promises that resolve to actions
     */
    protected dispatchActions(actions: (Action | Promise<Action>)[]): void {
        for (const actionOrPromise of actions) {
            if (this.isAction(actionOrPromise)) {
                this.actionDispatcher.dispatch(actionOrPromise);
            } else {
                actionOrPromise.then((action: Action) => {
                    this.actionDispatcher.dispatch(action);
                });
            }
        }
    }

    /**
     * Checks if a value is an Action (not a Promise).
     *
     * @param value The value to check
     * @returns True if the value is an Action
     */
    protected isAction(value: Action | Promise<Action>): value is Action {
        return (value as Action).kind !== undefined;
    }

    /**
     * Decorates the virtual node with pointer event handlers if it represents the root element.
     *
     * @param vnode The virtual node to decorate
     * @param element The model element
     * @returns The decorated virtual node
     */
    decorate(vnode: VNode, element: GModelElement): VNode {
        if (element instanceof GModelRoot) {
            const root = element as GModelRootType;
            on(vnode, "pointerover", this.handleEvent.bind(this, "pointerOver", root) as (e: Event) => void);
            on(vnode, "pointerenter", this.handleEvent.bind(this, "pointerEnter", root) as (e: Event) => void);
            on(vnode, "pointerdown", this.handleEvent.bind(this, "pointerDown", root) as (e: Event) => void);
            on(vnode, "pointermove", this.handleEvent.bind(this, "pointerMove", root) as (e: Event) => void);
            on(vnode, "pointerup", this.handleEvent.bind(this, "pointerUp", root) as (e: Event) => void);
            on(vnode, "pointercancel", this.handleEvent.bind(this, "pointerCancel", root) as (e: Event) => void);
            on(vnode, "pointerout", this.handleEvent.bind(this, "pointerOut", root) as (e: Event) => void);
            on(vnode, "pointerleave", this.handleEvent.bind(this, "pointerLeave", root) as (e: Event) => void);
            on(
                vnode,
                "gotpointercapture",
                this.handleEvent.bind(this, "gotPointerCapture", root) as (e: Event) => void
            );
            on(
                vnode,
                "lostpointercapture",
                this.handleEvent.bind(this, "lostPointerCapture", root) as (e: Event) => void
            );
        }
        return vnode;
    }

    /**
     * Called after the diagram is updated.
     */
    postUpdate(): void {}
}

/**
 * Base implementation of IPointerListener that provides no-op implementations for all methods.
 * Extend this class to only override the events you need to handle.
 */
@injectable()
export class PointerListener implements IPointerListener {
    /**
     * Called when pointer moves over an element.
     *
     * @param _target The target element
     * @param _event The pointer event
     * @returns Array of actions or promises that resolve to actions
     */
    pointerOver(_target: GModelElement, _event: PointerEvent): (Action | Promise<Action>)[] {
        return [];
    }

    /**
     * Called when pointer enters an element.
     *
     * @param _target The target element
     * @param _event The pointer event
     * @returns Array of actions or promises that resolve to actions
     */
    pointerEnter(_target: GModelElement, _event: PointerEvent): (Action | Promise<Action>)[] {
        return [];
    }

    /**
     * Called when pointer button is pressed down on an element.
     *
     * @param _target The target element
     * @param _event The pointer event
     * @returns Array of actions or promises that resolve to actions
     */
    pointerDown(_target: GModelElement, _event: PointerEvent): (Action | Promise<Action>)[] {
        return [];
    }

    /**
     * Called when pointer moves on an element.
     *
     * @param _target The target element
     * @param _event The pointer event
     * @returns Array of actions or promises that resolve to actions
     */
    pointerMove(_target: GModelElement, _event: PointerEvent): (Action | Promise<Action>)[] {
        return [];
    }

    /**
     * Called when pointer button is released on an element.
     *
     * @param _target The target element
     * @param _event The pointer event
     * @returns Array of actions or promises that resolve to actions
     */
    pointerUp(_target: GModelElement, _event: PointerEvent): (Action | Promise<Action>)[] {
        return [];
    }

    /**
     * Called when pointer event is cancelled.
     *
     * @param _target The target element
     * @param _event The pointer event
     * @returns Array of actions or promises that resolve to actions
     */
    pointerCancel(_target: GModelElement, _event: PointerEvent): (Action | Promise<Action>)[] {
        return [];
    }

    /**
     * Called when pointer moves out of an element.
     *
     * @param _target The target element
     * @param _event The pointer event
     * @returns Array of actions or promises that resolve to actions
     */
    pointerOut(_target: GModelElement, _event: PointerEvent): (Action | Promise<Action>)[] {
        return [];
    }

    /**
     * Called when pointer leaves an element.
     *
     * @param _target The target element
     * @param _event The pointer event
     * @returns Array of actions or promises that resolve to actions
     */
    pointerLeave(_target: GModelElement, _event: PointerEvent): (Action | Promise<Action>)[] {
        return [];
    }

    /**
     * Called when element gains pointer capture.
     *
     * @param _target The target element
     * @param _event The pointer event
     * @returns Array of actions or promises that resolve to actions
     */
    gotPointerCapture(_target: GModelElement, _event: PointerEvent): (Action | Promise<Action>)[] {
        return [];
    }

    /**
     * Called when element loses pointer capture.
     *
     * @param _target The target element
     * @param _event The pointer event
     * @returns Array of actions or promises that resolve to actions
     */
    lostPointerCapture(_target: GModelElement, _event: PointerEvent): (Action | Promise<Action>)[] {
        return [];
    }
}
