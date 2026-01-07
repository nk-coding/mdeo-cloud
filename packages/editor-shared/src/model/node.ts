import { sharedImport } from "../sharedImport.js";

const { GNode: SNodeImpl } = sharedImport("@eclipse-glsp/sprotty");

export class GNode extends SNodeImpl {}
