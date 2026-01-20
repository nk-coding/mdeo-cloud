<template>
    <div class="w-full space-y-2">
        <div v-if="label" class="flex items-center justify-between">
            <span class="text-sm font-medium">{{ label }}</span>
        </div>

        <div class="border border-border rounded-md p-3 space-y-3">
            <div v-if="!elements || elements.length === 0" class="text-sm text-muted-foreground text-center py-2">
                No items. Click the button below to add one.
            </div>

            <div v-for="(element, index) in elements" :key="index" class="flex items-start gap-2">
                <div class="flex-1">
                    <ActionForm
                        v-model="elements[index]"
                        :schema="schema.elements"
                        :errors="errors"
                        :path="`${path}/${index}`"
                        :nested="true"
                    />
                </div>
                <Button
                    variant="ghost"
                    size="icon-sm"
                    class="shrink-0 mt-0.5"
                    :aria-label="`Remove item ${index + 1}`"
                    @click="removeElement(index)"
                >
                    <X class="h-4 w-4" />
                </Button>
            </div>

            <div class="pt-1">
                <Button variant="outline" size="sm" class="w-full" @click="addElement">
                    <Plus class="h-4 w-4 mr-1" />
                    Add Item
                </Button>
            </div>
        </div>

        <FieldError v-if="fieldErrors.length > 0" :errors="fieldErrors.map((e) => e.message)" />
    </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { Plus, X } from "lucide-vue-next";
import type { ActionSchemaElementsForm, ActionValidationError } from "@mdeo/language-common";
import { Button } from "@/components/ui/button";
import { FieldError } from "@/components/ui/field";
import { generateDefaultValue, getErrorsForPath } from "./actionFormUtils";
import ActionForm from "./ActionForm.vue";

const props = withDefaults(
    defineProps<{
        schema: ActionSchemaElementsForm;
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

/**
 * The array elements, ensuring it's always an array
 */
const elements = computed(() => {
    if (Array.isArray(model.value)) {
        return model.value;
    }
    return [];
});

/**
 * Validation errors for the array itself (not child elements)
 */
const fieldErrors = computed(() => getErrorsForPath(props.errors, props.path));

/**
 * Adds a new element to the array
 */
function addElement(): void {
    model.value = [...elements.value, generateDefaultValue(props.schema.elements)];
}

/**
 * Removes an element at the specified index
 *
 * @param index The index of the element to remove
 */
function removeElement(index: number): void {
    model.value = elements.value.filter((_, i) => i !== index);
}
</script>
