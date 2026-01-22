<template>
    <InputGroup>
        <InputGroupInput
            :id="id"
            v-model="model"
            :type="showPassword ? 'text' : 'password'"
            :autocomplete="autocomplete"
            :placeholder="placeholder"
            :disabled="disabled"
        />
        <InputGroupButton
            v-if="showToggle"
            type="button"
            variant="ghost"
            @click="showPassword = !showPassword"
            :aria-label="showPassword ? 'Hide password' : 'Show password'"
        >
            <component :is="showPassword ? EyeOff : Eye" class="size-4" />
        </InputGroupButton>
    </InputGroup>
</template>

<script setup lang="ts">
import { ref, watch } from "vue";
import { Eye, EyeOff } from "lucide-vue-next";
import { InputGroup, InputGroupInput, InputGroupButton } from "@/components/ui/input-group";

const props = defineProps({
    id: { type: String, required: false },
    placeholder: { type: String, required: false },
    autocomplete: { type: String, required: false },
    disabled: { type: Boolean, default: false },
    showToggle: { type: Boolean, default: true }
});

const model = defineModel<string>();

const showPassword = ref(false);
</script>
