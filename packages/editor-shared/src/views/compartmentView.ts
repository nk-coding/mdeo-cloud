import { sharedImport } from "../sharedImport.js";

const { GCompartmentView: SCompartmentView } = sharedImport("@eclipse-glsp/sprotty");
const { injectable } = sharedImport("inversify");

@injectable()
export class CompartmentView extends SCompartmentView {}
