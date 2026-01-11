<template>
    <input
        ref="inputRef"
        v-model="localValue"
        :placeholder="placeholder"
        @keydown.enter="handleSubmit"
        @keydown.esc="handleCancel"
        @blur="handleBlur($event)"
        class="border-none outline-none ring-none w-0 flex-1"
        :aria-invalid="hasError"
    />
</template>

<script setup lang="ts">
import { ref, onMounted, watch, computed, useTemplateRef } from "vue";

const props = withDefaults(
    defineProps<{
        modelValue?: string;
        placeholder?: string;
        autoFocus?: boolean;
        validate?: (value: string) => boolean;
    }>(),
    {
        modelValue: "",
        placeholder: "",
        autoFocus: true,
        hasError: false
    }
);

const emit = defineEmits<{
    "update:modelValue": [value: string];
    submit: [value: string];
    cancel: [];
}>();

const inputRef = useTemplateRef("inputRef");
const localValue = ref(props.modelValue);

const hasError = computed(() => {
    if (props.validate) {
        return !props.validate(localValue.value);
    }
    return false;
});

watch(
    () => props.modelValue,
    (newValue) => {
        localValue.value = newValue;
    }
);

watch(localValue, (newValue) => {
    emit("update:modelValue", newValue);
});

function handleSubmit() {
    if (hasError.value) {
        return;
    }
    emit("submit", localValue.value.trim());
}

function handleBlur(event: FocusEvent) {
    const relatedTarget = event.relatedTarget;
    if (
        relatedTarget != undefined &&
        relatedTarget instanceof HTMLElement &&
        relatedTarget.dataset.slot === "context-menu-item"
    ) {
        setTimeout(() => {
            focusInput();
        }, 10);
        return;
    }
    if (hasError.value) {
        handleCancel();
    } else {
        handleSubmit();
    }
}

function handleCancel() {
    emit("cancel");
}

onMounted(() => {
    if (props.autoFocus && inputRef.value) {
        focusInput();
    }
});

function focusInput() {
    inputRef.value?.focus();
    inputRef.value?.select();
}
</script>
