import type { Kind, TypirSpecifics } from "typir";
import { CustomValueTypeProvider, type CustomValueType, type CustomValueTypeConstructor } from "./custom-value-type.js";
import type { ExtendedTypirServices } from "../../service/extendedTypirServices.js";

export interface CustomValueFactoryService {
    /**
     * The custom value type constructor
     */
    CustomValueType: CustomValueTypeConstructor;

    /**
     * Type guard to check if a value is a CustomValueType.
     *
     * @param type The value to check
     * @returns true if the value is a CustomValueType
     */
    isCustomValueType(type: unknown): type is CustomValueType;
}

export const CustomValueKindName = "CustomValue";

export class CustomValueKind<Specifics extends TypirSpecifics> implements Kind, CustomValueFactoryService {
    readonly $name: string = CustomValueKindName;

    readonly CustomValueType: CustomValueTypeConstructor;

    constructor(readonly services: ExtendedTypirServices<Specifics>) {
        this.CustomValueType = CustomValueTypeProvider(services);
    }

    isCustomValueType(type: unknown): type is CustomValueType {
        return type instanceof this.CustomValueType;
    }
}
