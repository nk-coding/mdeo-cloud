<template>
    <div class="w-full space-y-2">
        <div v-if="label" class="flex items-center justify-between">
            <span class="text-sm font-medium">{{ label }}</span>
        </div>

        <div :class="[nested ? 'border border-border rounded-md p-4' : '', 'space-y-4']">
            <template v-for="key in Object.keys(schema.properties)" :key="key">
                <ActionForm
                    v-model="model![key]"
                    :schema="schema.properties[key]!"
                    :errors="errors"
                    :path="`${path}/${key}`"
                    :label="schema.propertyLabels?.[key] ?? key"
                    :nested="true"
                />
            </template>
        </div>

        <FieldError v-if="fieldErrors.length > 0" :errors="fieldErrors.map((e) => e.message)" />
    </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import type { ActionSchemaPropertiesForm, ActionValidationError } from "@mdeo/language-common";
import { FieldError } from "@/components/ui/field";
import { getErrorsForPath } from "./actionFormUtils";
import ActionForm from "./ActionForm.vue";

const props = withDefaults(
    defineProps<{
        schema: ActionSchemaPropertiesForm;
        errors: ActionValidationError[];
        path: string;
        label?: string;
        nested?: boolean;
    }>(),
    {
        nested: false
    }
);

const model = defineModel<Record<string, unknown>>();




const fieldErrors = computed(() => getErrorsForPath(props.errors, props.path));
</script>
