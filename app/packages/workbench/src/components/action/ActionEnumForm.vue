<template>
    <Field class="w-full">
        <FieldLabel v-if="label">{{ label }}</FieldLabel>
        <FieldContent>
            <div v-if="schema.combobox" class="relative">
                <Combobox v-model="model" v-model:search-term="searchTerm" open-on-click>
                    <ComboboxAnchor class="w-full">
                        <InputGroup
                            class="**:data-[slot=command-input-wrapper]:grow **:data-[slot=command-input-wrapper]:border-none"
                        >
                            <ComboboxInput
                                :placeholder="schema.placeholder || 'Select or type...'"
                                :aria-describedby="fieldErrors.length > 0 ? errorId : undefined"
                            />
                            <ComboboxTrigger class="mr-3">
                                <ChevronDown class="w-4 h-4 opacity-50" />
                            </ComboboxTrigger>
                        </InputGroup>
                    </ComboboxAnchor>

                    <ComboboxList class="w-(--reka-combobox-trigger-width)" :align="'start'">
                        <ComboboxViewport>
                            <ComboboxEmpty>No results found.</ComboboxEmpty>
                            <ComboboxGroup>
                                <ComboboxItem
                                    v-for="value in filteredOptions"
                                    :key="value"
                                    :value="value"
                                    :text-value="value"
                                >
                                    {{ value }}
                                    <ComboboxItemIndicator>
                                        <Check />
                                    </ComboboxItemIndicator>
                                </ComboboxItem>
                            </ComboboxGroup>
                        </ComboboxViewport>
                    </ComboboxList>
                </Combobox>
            </div>

            <div v-else class="relative">
                <Select v-model="model">
                    <SelectTrigger
                        :id="selectId"
                        class="w-full"
                        :aria-describedby="fieldErrors.length > 0 ? errorId : undefined"
                    >
                        <SelectValue :placeholder="schema.placeholder || 'Select an option'" />
                    </SelectTrigger>
                    <SelectContent>
                        <SelectItem v-for="value in schema.enum" :key="value" :value="value">
                            {{ value }}
                        </SelectItem>
                    </SelectContent>
                </Select>
            </div>
            <FieldError v-if="fieldErrors.length > 0" :id="errorId" :errors="fieldErrors.map((e) => e.message)" />
        </FieldContent>
    </Field>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import type { ActionSchemaEnumForm, ActionValidationError } from "@mdeo/language-common";
import { Field, FieldContent, FieldLabel, FieldError } from "@/components/ui/field";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import {
    Combobox,
    ComboboxAnchor,
    ComboboxInput,
    ComboboxTrigger,
    ComboboxViewport,
    ComboboxEmpty,
    ComboboxGroup,
    ComboboxItem,
    ComboboxItemIndicator
} from "@/components/ui/combobox";
import ComboboxList from "@/components/ui/combobox/ComboboxList.vue";
import { InputGroup, InputGroupInput } from "@/components/ui/input-group";
import { ChevronDown, Check } from "lucide-vue-next";
import { getErrorsForPath } from "./actionFormUtils";

const props = defineProps<{
    schema: ActionSchemaEnumForm;
    errors: ActionValidationError[];
    path: string;
    label?: string;
}>();

const model = defineModel<string>();

const selectId = computed(() => `enum-select-${props.path.replace(/\//g, "-")}`);
const errorId = computed(() => `${selectId.value}-error`);
const fieldErrors = computed(() => getErrorsForPath(props.errors, props.path));
const searchTerm = ref("");

const filteredOptions = computed(() => {
    if (!searchTerm.value) {
        return props.schema.enum;
    }
    const term = searchTerm.value.toLowerCase();
    return props.schema.enum.filter((option) => option.toLowerCase().includes(term));
});
</script>
