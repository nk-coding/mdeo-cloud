<template>
    <div>
        <ActionTypeForm
            v-if="isTypeFormSchema"
            :schema="<ActionSchemaTypeForm>schema"
            v-model="typeFormModel"
            :errors="filteredErrors"
            :path="path"
            :label="label"
        />
        <ActionEnumForm
            v-else-if="isEnumFormSchema"
            :schema="<ActionSchemaEnumForm>schema"
            v-model="enumFormModel"
            :errors="filteredErrors"
            :path="path"
            :label="label"
        />
        <ActionElementsForm
            v-else-if="isElementsFormSchema"
            :schema="<ActionSchemaElementsForm>schema"
            v-model="modelValue"
            :errors="filteredErrors"
            :path="path"
            :label="label"
            :nested="nested"
        />
        <ActionOptionalForm
            v-else-if="isOptionalFormSchema"
            :schema="<ActionSchemaOptionalForm>schema"
            v-model="modelValue"
            :errors="filteredErrors"
            :path="path"
            :label="label"
            :nested="nested"
        />
        <ActionPropertiesForm
            v-else-if="isPropertiesFormSchema"
            :schema="<ActionSchemaPropertiesForm>schema"
            v-model="<Record<string, unknown>>modelValue"
            :errors="filteredErrors"
            :path="path"
            :label="label"
            :nested="nested"
        />
        <div v-else class="text-sm text-muted-foreground">Unsupported schema type</div>
    </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import type {
    ActionSchema,
    ActionSchemaTypeForm,
    ActionSchemaEnumForm,
    ActionSchemaElementsForm,
    ActionSchemaPropertiesForm,
    ActionSchemaOptionalForm,
    ActionValidationError
} from "@mdeo/language-common";
import ActionTypeForm from "./ActionTypeForm.vue";
import ActionEnumForm from "./ActionEnumForm.vue";
import ActionElementsForm from "./ActionElementsForm.vue";
import ActionOptionalForm from "./ActionOptionalForm.vue";
import ActionPropertiesForm from "./ActionPropertiesForm.vue";
import {
    isTypeForm,
    isEnumForm,
    isElementsForm,
    isOptionalForm,
    isPropertiesForm,
    filterErrorsByPath
} from "./actionFormUtils";

const props = withDefaults(
    defineProps<{
        schema: ActionSchema;
        errors: ActionValidationError[];
        path?: string;
        label?: string;
        nested?: boolean;
    }>(),
    {
        path: "",
        nested: false
    }
);

const modelValue = defineModel<unknown>();

/**
 * Whether the schema is a type form
 */
const isTypeFormSchema = computed(() => isTypeForm(props.schema));

/**
 * Whether the schema is an enum form
 */
const isEnumFormSchema = computed(() => isEnumForm(props.schema));

/**
 * Whether the schema is an elements form
 */
const isElementsFormSchema = computed(() => isElementsForm(props.schema));

/**
 * Whether the schema is an optional form
 */
const isOptionalFormSchema = computed(() => isOptionalForm(props.schema));

/**
 * Whether the schema is a properties form
 */
const isPropertiesFormSchema = computed(() => isPropertiesForm(props.schema));

/**
 * Errors filtered to this path and its children
 */
const filteredErrors = computed(() => filterErrorsByPath(props.errors, props.path));

/**
 * Type-safe computed for ActionTypeForm
 */
const typeFormModel = computed({
    get(): string | number | boolean | undefined {
        return modelValue.value as string | number | boolean | undefined;
    },
    set(value: string | number | boolean | undefined) {
        modelValue.value = value;
    }
});

/**
 * Type-safe computed for ActionEnumForm
 */
const enumFormModel = computed({
    get(): string | undefined {
        return modelValue.value as string | undefined;
    },
    set(value: string | undefined) {
        modelValue.value = value;
    }
});
</script>
