import { sharedImport } from "../../sharedImport.js";
import { EdgeRouter } from "../edge-rourting/edgeRouter.js";

const { FeatureModule } = sharedImport("@eclipse-glsp/client");

/**
 * Feature module for the edge routing to
 */
export const edgeRoutingModule = new FeatureModule((bind) => {
    bind(EdgeRouter).toSelf().inSingletonScope();
});
