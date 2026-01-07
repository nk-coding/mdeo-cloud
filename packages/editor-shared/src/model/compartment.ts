import { sharedImport } from "../sharedImport.js";

const { GCompartment: SCompartmentImpl } = sharedImport("@eclipse-glsp/sprotty");

export class GCompartment extends SCompartmentImpl {}
