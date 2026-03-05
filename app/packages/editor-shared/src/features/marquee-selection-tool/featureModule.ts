import { sharedImport } from "../../sharedImport.js";
import { MarqueeUtil } from "./marqueeUtil.js";

const { FeatureModule } = sharedImport("@eclipse-glsp/sprotty");
const { MarqueeUtil: GLSPMarqueeUtil } = sharedImport("@eclipse-glsp/client");

/**
 * Feature module for marquee selection.
 */
export const marqueeSelectionToolModule = new FeatureModule(
    (bind, unbind, isBound, rebind) => {
        bind(MarqueeUtil).toSelf().inSingletonScope();
        rebind(GLSPMarqueeUtil).toService(MarqueeUtil);
    },
    { featureId: Symbol("marquee") }
);
