<template>
    <component :is="iconComponent" :class="iconClass" />
</template>
<script setup lang="ts">
import { computed } from "vue";
import { 
    Clock, 
    Loader2, 
    CheckCircle2, 
    XCircle, 
    AlertCircle, 
} from "lucide-vue-next";
import type { ExecutionState } from "@/data/execution/execution";

const props = defineProps<{
    state: ExecutionState;
}>();

const iconComponent = computed(() => {
    switch (props.state) {
        case "submitted":
            return Clock;
        case "initializing":
            return Loader2;
        case "running":
            return Loader2;
        case "completed":
            return CheckCircle2;
        case "cancelled":
            return XCircle;
        case "failed":
            return AlertCircle;
        default:
            return Clock;
    }
});

const iconClass = computed(() => {
    const baseClasses = "";
    switch (props.state) {
        case "submitted":
            return `${baseClasses} text-muted-foreground`;
        case "initializing":
            return `${baseClasses} text-muted-foreground animate-spin`;
        case "running":
            return `${baseClasses} text-blue-500 animate-spin`;
        case "completed":
            return `${baseClasses} text-green-500`;
        case "cancelled":
            return `${baseClasses} text-yellow-500`;
        case "failed":
            return `${baseClasses} text-destructive`;
        default:
            return baseClasses;
    }
});
</script>
