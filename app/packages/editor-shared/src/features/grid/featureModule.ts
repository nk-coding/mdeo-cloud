import { sharedImport } from "../../sharedImport.js";
import { GridSnapper } from "./gridSnapper.js";

const { FeatureModule } = sharedImport("@eclipse-glsp/sprotty");
const { TYPES } = sharedImport("@eclipse-glsp/client");

export const gridModule = new FeatureModule(
    (bind, unbind, isBound, rebind) => {
        bind(GridSnapper).toSelf().inSingletonScope();
        rebind(TYPES.ISnapper).toService(GridSnapper);
    },
    { featureId: Symbol("grid") }
);
