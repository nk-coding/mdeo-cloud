import { sharedImport } from "../../sharedImport.js";
import { PointerTool, IPointerListener } from "./pointerTool.js";
import { PointerCapturePointerListener } from "./pointerCapturePointerListener.js";

const { FeatureModule } = sharedImport("@eclipse-glsp/client");
const { TYPES } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Feature module that provides pointer event handling and pointer capture support.
 * Registers the pointer tool as a VNode postprocessor and sets up pointer capture listener.
 */
export const pointerToolModule = new FeatureModule(
    (bind) => {
        bind(PointerTool).toSelf().inSingletonScope();
        bind(TYPES.IVNodePostprocessor).toService(PointerTool);

        bind(PointerCapturePointerListener).toSelf().inSingletonScope();
        bind(IPointerListener).toService(PointerCapturePointerListener);
    },
    { featureId: Symbol("pointer-tool") }
);
