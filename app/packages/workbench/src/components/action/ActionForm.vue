<template>
    <div>
        <ActionTypeForm
            v-if="isTypeFormSchema"
            :schema="<ActionSchemaTypeForm>schema"
            v-model="<string | boolean | number>modelValue"
            :errors="filteredErrors"
            :path="path"
            :label="label"
        />
        <ActionEnumForm
            v-else-if="isEnumFormSchema"
            :schema="<ActionSchemaEnumForm>schema"
            v-model="<string>modelValue"
            :errors="filteredErrors"
            :path="path"
            :label="label"
        />
        <ActionFileSelectForm
            v-else-if="isFileSelectFormSchema"
            :schema="<ActionSchemaFileSelectForm>schema"
            v-model="<string>modelValue"
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
    ActionSchemaFileSelectForm,
    ActionSchemaElementsForm,
    ActionSchemaPropertiesForm,
    ActionSchemaOptionalForm,
    ActionValidationError
} from "@mdeo/language-common";
import ActionTypeForm from "./ActionTypeForm.vue";
import ActionEnumForm from "./ActionEnumForm.vue";
import ActionFileSelectForm from "./ActionFileSelectForm.vue";
import ActionElementsForm from "./ActionElementsForm.vue";
import ActionOptionalForm from "./ActionOptionalForm.vue";
import ActionPropertiesForm from "./ActionPropertiesForm.vue";
import {
    isTypeForm,
    isEnumForm,
    isFileSelectForm,
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

const isTypeFormSchema = computed(() => isTypeForm(props.schema));

const isEnumFormSchema = computed(() => isEnumForm(props.schema));

const isFileSelectFormSchema = computed(() => isFileSelectForm(props.schema));

const isElementsFormSchema = computed(() => isElementsForm(props.schema));

const isOptionalFormSchema = computed(() => isOptionalForm(props.schema));

const isPropertiesFormSchema = computed(() => isPropertiesForm(props.schema));

const filteredErrors = computed(() => filterErrorsByPath(props.errors, props.path));
</script>
