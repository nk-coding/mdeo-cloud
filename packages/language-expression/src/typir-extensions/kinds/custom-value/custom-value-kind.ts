import type { Kind, TypirSpecifics } from "typir";
import type { ExtendedTypirServices } from "../../service/extendedTypirServices.js";

// eslint-disable-next-line @typescript-eslint/no-empty-object-type
export interface CustomValueFactoryService {}

export const CustomValueKindName = "CustomValue";

export class CustomValueKind<Specifics extends TypirSpecifics> implements Kind, CustomValueFactoryService {
    readonly $name: string = CustomValueKindName;

    constructor(readonly services: ExtendedTypirServices<Specifics>) {}
}
