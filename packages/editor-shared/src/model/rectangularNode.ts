import type { LayoutContainer } from "@eclipse-glsp/sprotty";
import { GNode } from "./node.js";

export class GRectangularNode extends GNode implements LayoutContainer {
    override layout = "vbox";

    override layoutOptions = {
        paddingTop: 2,
        paddingLeft: 2,
        paddingBottom: 2,
        paddingRight: 2,
        hGrab: true,
        vGrab: true,
        vGap: 0
    };
}
