<template>
    <div class="w-full space-y-2">
        <div v-if="label" class="flex items-center justify-between">
            <span class="text-sm font-medium">{{ label }}</span>
        </div>

        <div class="border border-border rounded-md p-3 space-y-3">
            <div v-if="!hasValue" class="text-sm text-muted-foreground text-center py-2">
                No value set. Click the button below to add one.
            </div>

            <div v-else class="flex items-start gap-2">
                <div class="flex-1">
                    <ActionForm
                        :schema="schema.optional"
                        v-model="model"
                        :errors="errors"
                        :path="path"
                        :nested="true"
                    />
                </div>
                <Button
                    variant="ghost"
                    size="icon-sm"
                    class="shrink-0 mt-0.5"
                    aria-label="Remove value"
                    @click="removeValue"
                >
                    <X class="size-4" />
                </Button>
            </div>

            <div v-if="!hasValue" class="pt-1">
                <Button variant="outline" size="sm" class="w-full" @click="addValue">
                    <Plus class="size-4 mr-1" />
                    Add Value
                </Button>
            </div>
        </div>

        <FieldError v-if="fieldErrors.length > 0" :errors="fieldErrors.map((e) => e.message)" />
    </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { Plus, X } from "lucide-vue-next";
import type { ActionSchemaOptionalForm, ActionValidationError } from "@mdeo/language-common";
import { Button } from "@/components/ui/button";
import { FieldError } from "@/components/ui/field";
import { generateDefaultValue, getErrorsForPath } from "./actionFormUtils";
import ActionForm from "./ActionForm.vue";

const props = withDefaults(
    defineProps<{
        schema: ActionSchemaOptionalForm;
        errors: ActionValidationError[];
        path: string;
        label?: string;
        nested?: boolean;
    }>(),
    {
        nested: false
    }
);

const model = defineModel<unknown>();

const hasValue = computed(() => model.value !== undefined);

const fieldErrors = computed(() => getErrorsForPath(props.errors, props.path));

function addValue(): void {
    model.value = generateDefaultValue(props.schema.optional);
}

function removeValue(): void {
    model.value = undefined;
}
</script>
