import { sharedImport } from "../../sharedImport.js";
import { ElementFinder } from "./elementFinder.js";

const { FeatureModule } = sharedImport("@eclipse-glsp/client");

/**
 * Feature module that provides element finding functionality.
 * Registers the ElementFinder service for finding GLSP elements from DOM elements or coordinates.
 */
export const elementFinderModule = new FeatureModule((bind) => {
    bind(ElementFinder).toSelf().inSingletonScope();
});
