<template>
    <Field class="w-full">
        <FieldLabel v-if="label">{{ label }}</FieldLabel>
        <FieldContent>
            <Switch v-if="isBoolean" v-model:checked="booleanModel" />
            <InputGroup v-else-if="isNumber">
                <NumberField
                    v-model="numberModel"
                    :min="numberMin"
                    :max="numberMax"
                    :step="numberStep"
                    :format-options="numberFormatOptions"
                    class="w-full"
                >
                    <NumberFieldInput :placeholder="placeholder" class="text-left px-3" />
                </NumberField>
            </InputGroup>
            <InputGroup v-else>
                <InputGroupInput type="text" v-model="stringModel" :placeholder="placeholder" />
            </InputGroup>
            <FieldError v-if="fieldErrors.length > 0" :errors="fieldErrors.map((e) => e.message)" />
        </FieldContent>
    </Field>
</template>

<script setup lang="ts">
import { computed, watch } from "vue";
import type { ActionSchemaTypeForm, ActionValidationError } from "@mdeo/language-common";
import { Field, FieldContent, FieldLabel, FieldError } from "@/components/ui/field";
import { Switch } from "@/components/ui/switch";
import { InputGroup, InputGroupInput } from "@/components/ui/input-group";
import { NumberField, NumberFieldInput } from "@/components/ui/number-field";
import { getErrorsForPath } from "./actionFormUtils";

const props = defineProps<{
    schema: ActionSchemaTypeForm;
    errors: ActionValidationError[];
    path: string;
    label?: string;
}>();

const model = defineModel<string | boolean | number | undefined>();

const isBoolean = computed(() => props.schema.type === "boolean");

const isNumber = computed(() => {
    const type = props.schema.type;
    return (
        type === "int8" ||
        type === "int16" ||
        type === "int32" ||
        type === "uint8" ||
        type === "uint16" ||
        type === "uint32" ||
        type === "float32" ||
        type === "float64"
    );
});

const isInteger = computed(() => {
    const type = props.schema.type;
    return (
        type === "int8" ||
        type === "int16" ||
        type === "int32" ||
        type === "uint8" ||
        type === "uint16" ||
        type === "uint32"
    );
});

const numberMin = computed(() => {
    const type = props.schema.type;
    switch (type) {
        case "int8":
            return -128;
        case "int16":
            return -32768;
        case "int32":
            return -2147483648;
        case "uint8":
        case "uint16":
        case "uint32":
            return 0;
        default:
            return undefined;
    }
});

const numberMax = computed(() => {
    const type = props.schema.type;
    switch (type) {
        case "int8":
            return 127;
        case "uint8":
            return 255;
        case "int16":
            return 32767;
        case "uint16":
            return 65535;
        case "int32":
            return 2147483647;
        case "uint32":
            return 4294967295;
        default:
            return undefined;
    }
});

const numberStep = computed(() => {
    return isInteger.value ? 1 : 0.01;
});

const numberFormatOptions = computed(() => {
    return {
        minimumFractionDigits: isInteger.value ? 0 : 0,
        maximumFractionDigits: isInteger.value ? 0 : 10
    };
});

const placeholder = computed(() => {
    if (props.schema.placeholder) {
        return props.schema.placeholder;
    }
    if (isNumber.value) {
        return "Enter a number...";
    }
    return "Enter text...";
});

const booleanModel = computed({
    get() {
        return model.value as boolean;
    },
    set(value: boolean) {
        model.value = value;
    }
});

const numberModel = computed({
    get() {
        if (model.value == undefined) {
            return undefined;
        }
        return Number(model.value);
    },
    set(value: number | undefined) {
        model.value = value;
    }
});

const stringModel = computed({
    get() {
        if (model.value == undefined) {
            return "";
        }
        return String(model.value);
    },
    set(value: string) {
        model.value = value;
    }
});

const fieldErrors = computed(() => getErrorsForPath(props.errors, props.path));
</script>
