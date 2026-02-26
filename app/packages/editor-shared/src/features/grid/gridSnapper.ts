import type { Point } from "@eclipse-glsp/protocol";
import type { GModelElement } from "@eclipse-glsp/sprotty";
import { sharedImport } from "../../sharedImport.js";
import { GNode } from "../../model/node.js";

const { injectable } = sharedImport("inversify");
const { GridSnapper: GLSPGridSnapper } = sharedImport("@eclipse-glsp/client");

@injectable()
export class GridSnapper extends GLSPGridSnapper {
    override snap(position: Point, element: GModelElement): Point {
        if (element instanceof GNode) {
            let dx = 0;
            let dy = 0;
            if (element.vAlign === "center") {
                dy = element.bounds.height / 2;
            } else if (element.vAlign === "bottom") {
                dy = element.bounds.height;
            }
            if (element.hAlign === "center") {
                dx = element.bounds.width / 2;
            } else if (element.hAlign === "right") {
                dx = element.bounds.width;
            }
            const snapResult = super.snap({ x: position.x + dx, y: position.y + dy }, element);
            return { x: snapResult.x - dx, y: snapResult.y - dy };
        } else {
            return super.snap(position, element);
        }
    }
}
