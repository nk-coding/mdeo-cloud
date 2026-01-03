import type { Kind, TypirSpecifics } from "typir";
import { CustomValueTypeImplementation, type CustomValueType } from "./custom-value-type.js";
import type { ExtendedTypirServices } from "../../service/extendedTypirServices.js";

export interface CustomValueFactoryService {}

export const CustomValueKindName = "CustomValue";

export class CustomValueKind<Specifics extends TypirSpecifics> implements Kind, CustomValueFactoryService {
    readonly $name: string = CustomValueKindName;

    constructor(readonly services: ExtendedTypirServices<Specifics>) {}
}
