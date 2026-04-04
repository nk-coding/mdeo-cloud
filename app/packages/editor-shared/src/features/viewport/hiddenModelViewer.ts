import { sharedImport } from "../../sharedImport.js";

const { injectable } = sharedImport("inversify");
const { HiddenModelViewer } = sharedImport("@eclipse-glsp/sprotty");

type HiddenModelViewerUpdateArgs = Parameters<InstanceType<typeof HiddenModelViewer>["update"]>;

/**
 * Overrides the sprotty {@link HiddenModelViewer} so the hidden bounds-computation
 * div is `display: block` only for the brief window during which sprotty renders
 * and reads element bounds, then immediately hidden again.
 *
 * This replaces the previous approach of injecting a `<style>` tag from the Vue
 * workbench layer that toggled the element per-tab-active-state.
 */
@injectable()
export class MdeoHiddenModelViewer extends HiddenModelViewer {
    /**
     * The HTML element used by sprotty
     */
    private element: HTMLElement | null = null;

    override update(...args: HiddenModelViewerUpdateArgs): void {
        if (this.element != null) {
            document.body.appendChild(this.element);
        }
        super.update(...args);
        this.element = document.getElementById(this.options.hiddenDiv);
        this.element?.remove();
    }
}
