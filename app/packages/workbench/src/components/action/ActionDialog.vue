<template>
    <Dialog v-model:open="isOpen">
        <DialogContent class="sm:max-w-2xl w-full flex flex-col">
            <DialogHeader>
                <DialogTitle>{{ currentPage?.title ?? "Action" }}</DialogTitle>
                <DialogDescription v-if="currentPage?.description">
                    {{ currentPage.description }}
                </DialogDescription>
            </DialogHeader>

            <div class="flex max-h-[60vh]">
                <ScrollArea class="flex-1 min-h-10 -mx-2">
                    <ActionForm
                        v-if="currentPage"
                        class="m-2"
                        :schema="currentPage.schema"
                        :model-value="currentInputs"
                        :errors="validationErrors"
                        @update:model-value="onInputsUpdate"
                    />
                </ScrollArea>
            </div>

            <DialogFooter class="flex flex-row justify-between sm:justify-between">
                <div>
                    <Button v-if="canGoPrevious" variant="outline" :disabled="isSubmitting" @click="handlePrevious">
                        <ChevronLeft class="w-4 h-4 mr-1" />
                        Previous
                    </Button>
                </div>
                <div class="flex gap-2">
                    <Button variant="outline" :disabled="isSubmitting" @click="handleCancel">Cancel</Button>
                    <Button
                        v-if="currentPage?.isLastPage"
                        class="min-w-30"
                        :disabled="isSubmitting"
                        @click="handleSubmit"
                    >
                        <Loader2 v-if="isSubmitting" class="w-4 h-4 mr-1 animate-spin" />
                        <Check v-else class="w-4 h-4 mr-1" />
                        Submit
                    </Button>
                    <Button v-else class="min-w-27.5" :disabled="isSubmitting" @click="handleNext">
                        Next
                        <Loader2 v-if="isSubmitting" class="w-4 h-4 ml-1 animate-spin" />
                        <ChevronRight v-else class="w-4 h-4 ml-1" />
                    </Button>
                </div>
            </DialogFooter>
        </DialogContent>
    </Dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch, inject } from "vue";
import { ChevronLeft, ChevronRight, Check, Loader2 } from "lucide-vue-next";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import ActionForm from "./ActionForm.vue";
import {
    createActionProtocol,
    type ActionDialogPage,
    type ActionValidationError
} from "@mdeo/language-common";
import * as vscodeJsonrpc from "vscode-jsonrpc";
import { workbenchStateKey } from "../workbench/util";
import { deepCloneWithSchema, generateDefaultValue, verifyAndFix } from "./actionFormUtils";

const ActionProtocol = createActionProtocol(vscodeJsonrpc);

const { pendingAction, languageClient } = inject(workbenchStateKey)!;

const isOpen = ref(false);
const currentPage = ref<ActionDialogPage>();
const previousPagesStack = ref<{ page: ActionDialogPage; inputs: unknown }[]>([]);
const nextValuesStack = ref<unknown[]>([]);
const currentInputs = ref<unknown>({});
const validationErrors = ref<ActionValidationError[]>([]);
const isSubmitting = ref(false);
const canGoPrevious = computed(() => previousPagesStack.value.length > 0);

/**
 * Watches the pendingAction ref and starts the action dialog flow when set
 */
watch(
    pendingAction,
    async (newAction) => {
        if (newAction) {
            await startActionDialog(newAction);
        }
    },
    { immediate: true }
);

/**
 * Starts the action dialog by sending a request to the language client
 *
 * @param params The action start parameters
 */
async function startActionDialog(params: { type: string; languageId: string; data: unknown }): Promise<void> {
    resetDialogState();

    try {
        const response = await languageClient.value?.sendRequest(ActionProtocol.ActionStartRequest, params);

        if (response) {
            currentPage.value = response.page;
            currentInputs.value = generateDefaultValue(response.page.schema);
            isOpen.value = true;
        }
    } catch (error) {
        pendingAction.value = undefined;
    }
}

/**
 * Resets the dialog state to initial values
 */
function resetDialogState(): void {
    currentPage.value = undefined;
    previousPagesStack.value = [];
    nextValuesStack.value = [];
    currentInputs.value = {};
    validationErrors.value = [];
    isSubmitting.value = false;
}

/**
 * Handles updates to the form inputs
 *
 * @param value The new input values
 */
function onInputsUpdate(value: unknown): void {
    currentInputs.value = value;
}

/**
 * Builds the complete inputs array from all pages
 *
 * @returns Array of input values from all pages (stack of pages with last being current)
 */
function buildAllInputs(): Record<string, unknown>[] {
    const allInputs: Record<string, unknown>[] = [];
    for (const { page, inputs } of previousPagesStack.value) {
        allInputs.push(deepCloneWithSchema(inputs, page.schema) as Record<string, unknown>);
    }
    if (currentPage.value) {
        allInputs.push(deepCloneWithSchema(currentInputs.value, currentPage.value.schema) as Record<string, unknown>);
    }
    return allInputs;
}

/**
 * Handles the Previous button click
 */
function handlePrevious(): void {
    if (previousPagesStack.value.length > 0) {
        // Save current values to nextValuesStack for potential reuse
        nextValuesStack.value.push(currentInputs.value);
        
        const previous = previousPagesStack.value.pop()!;
        currentPage.value = previous.page;
        currentInputs.value = previous.inputs;
        validationErrors.value = [];
    }
}

/**
 * Handles the Next button click - submits current page and advances
 */
async function handleNext(): Promise<void> {
    if (pendingAction.value == undefined || languageClient.value == undefined) {
        return;
    }

    isSubmitting.value = true;
    try {
        const response = await languageClient.value.sendRequest(ActionProtocol.ActionSubmitRequest, {
            config: pendingAction.value,
            inputs: buildAllInputs()
        });

        if (response != undefined) {
            handleSubmitResponse(response);
        }
    } finally {
        isSubmitting.value = false;
    }
}

/**
 * Handles the Submit button click - submits final page
 */
async function handleSubmit(): Promise<void> {
    await handleNext();
}

/**
 * Processes the submit response from the language server
 *
 * @param response The action submit response
 */
function handleSubmitResponse(response: {
    kind: string;
    errors?: ActionValidationError[];
    page?: ActionDialogPage;
}): void {
    if (response.kind === "validation") {
        validationErrors.value = response.errors ?? [];
    } else if (response.kind === "nextPage" && response.page) {
        previousPagesStack.value.push({
            page: currentPage.value!,
            inputs: currentInputs.value
        });
        
        // Restore values from nextValuesStack or generate defaults
        if (nextValuesStack.value.length > 0) {
            const savedValues = nextValuesStack.value.pop()!;
            currentInputs.value = verifyAndFix(savedValues, response.page.schema);
        } else {
            currentInputs.value = generateDefaultValue(response.page.schema);
        }
        
        validationErrors.value = [];
        currentPage.value = response.page;
    } else if (response.kind === "completion") {
        closeDialog();
    }
}

/**
 * Handles the Cancel button click
 */
function handleCancel(): void {
    closeDialog();
}

/**
 * Closes the dialog without resetting state (to preserve animation)
 */
function closeDialog(): void {
    isOpen.value = false;
    pendingAction.value = undefined;
}
</script>
