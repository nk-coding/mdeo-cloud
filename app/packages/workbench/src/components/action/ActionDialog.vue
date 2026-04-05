<template>
    <Dialog v-model:open="isOpen">
        <DialogContent class="sm:max-w-2xl w-full flex flex-col">
            <DialogHeader>
                <DialogTitle>{{ errorState?.message ?? currentPage?.title ?? "Action" }}</DialogTitle>
                <DialogDescription v-if="errorState?.description ?? currentPage?.description">
                    {{ errorState?.description ?? currentPage?.description }}
                </DialogDescription>
            </DialogHeader>

            <div v-if="!errorState" class="flex max-h-[60vh]">
                <ScrollArea class="flex-1 min-h-10 -mx-2">
                    <ActionForm
                        v-if="currentPage"
                        v-model="currentInputs"
                        class="m-2"
                        :schema="currentPage.schema"
                        :errors="validationErrors"
                    />
                </ScrollArea>
            </div>

            <DialogFooter v-if="errorState" class="flex flex-row justify-end sm:justify-end">
                <Button @click="closeDialog">OK</Button>
            </DialogFooter>
            <DialogFooter v-else class="flex flex-row justify-between sm:justify-between">
                <div>
                    <Button v-if="canGoPrevious" variant="outline" :disabled="isSubmitting" @click="handlePrevious">
                        <ChevronLeft class="size-4 mr-1" />
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
                        <Loader2 v-if="isSubmitting" class="size-4 mr-1 animate-spin" />
                        <Check v-else class="size-4 mr-1" />
                        {{ currentPage?.submitButtonLabel ?? "Submit" }}
                    </Button>
                    <Button v-else class="min-w-27.5" :disabled="isSubmitting" @click="handleNext">
                        Next
                        <Loader2 v-if="isSubmitting" class="size-4 ml-1 animate-spin" />
                        <ChevronRight v-else class="size-4 ml-1" />
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
    type ActionValidationError,
    type ActionStartResponse,
    type ActionSubmitResponse,
    type ActionExecutionRequest,
    type ActionStartErrorResponse,
    type ActionSubmitErrorResponse
} from "@mdeo/language-common";
import * as vscodeJsonrpc from "vscode-jsonrpc";
import { workbenchStateKey } from "../workbench/util";
import { deepCloneWithSchema, generateDefaultValue, verifyAndFix } from "./actionFormUtils";
import { showApiError, showError, showSuccess } from "@/lib/notifications";

const ActionProtocol = createActionProtocol(vscodeJsonrpc);

const workbenchState = inject(workbenchStateKey)!;
const { pendingAction, languageClient, backendApi, project } = workbenchState;

const isOpen = ref(false);
const currentPage = ref<ActionDialogPage>();
const errorState = ref<{ message: string; description?: string }>();
const previousPagesStack = ref<{ page: ActionDialogPage; inputs: unknown }[]>([]);
const nextValuesStack = ref<unknown[]>([]);
const currentInputs = ref<unknown>({});
const validationErrors = ref<ActionValidationError[]>([]);
const isSubmitting = ref(false);
const canGoPrevious = computed(() => previousPagesStack.value.length > 0);

watch(
    pendingAction,
    async (newAction) => {
        if (newAction) {
            await startActionDialog(newAction);
        }
    },
    { immediate: true }
);

async function startActionDialog(params: { type: string; languageId: string; data: unknown }): Promise<void> {
    resetDialogState();

    try {
        const response = (await languageClient.value?.sendRequest(ActionProtocol.ActionStartRequest, params)) as
            | ActionStartResponse
            | undefined;

        if (response) {
            if (response.kind === "completion") {
                await handleCompletionEffects(response.executions);
                pendingAction.value = undefined;
            } else if (response.kind === "page") {
                currentPage.value = response.page;
                currentInputs.value = generateDefaultValue(response.page.schema);
                isOpen.value = true;
            } else if (response.kind === "error") {
                handleErrorResponse(response);
                pendingAction.value = undefined;
            }
        }
    } catch (error) {
        showError("Failed to start action", {
            description: error instanceof Error ? error.message : String(error)
        });
        pendingAction.value = undefined;
    }
}

function resetDialogState(): void {
    currentPage.value = undefined;
    errorState.value = undefined;
    previousPagesStack.value = [];
    nextValuesStack.value = [];
    currentInputs.value = {};
    validationErrors.value = [];
    isSubmitting.value = false;
}

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

function handlePrevious(): void {
    if (previousPagesStack.value.length > 0) {
        nextValuesStack.value.push(currentInputs.value);

        const previous = previousPagesStack.value.pop()!;
        currentPage.value = previous.page;
        currentInputs.value = previous.inputs;
        validationErrors.value = [];
    }
}

async function handleNext(): Promise<void> {
    if (pendingAction.value == undefined || languageClient.value == undefined) {
        return;
    }

    isSubmitting.value = true;
    try {
        const response = (await languageClient.value.sendRequest(ActionProtocol.ActionSubmitRequest, {
            config: pendingAction.value,
            inputs: buildAllInputs()
        })) as ActionSubmitResponse | undefined;

        if (response != undefined) {
            await handleSubmitResponse(response);
        }
    } finally {
        isSubmitting.value = false;
    }
}

async function handleSubmit(): Promise<void> {
    await handleNext();
}

async function createExecutions(executions: ActionExecutionRequest[]): Promise<void> {
    if (!project.value) {
        return;
    }

    for (const executionRequest of executions) {
        try {
            const result = await backendApi.executions.create(project.value.id, {
                filePath: executionRequest.filePath,
                data: executionRequest.data
            });

            if (result.success) {
                workbenchState.addExecution(result.value);
                showSuccess("Execution submitted", {
                    description: `${result.value.name} has been submitted`
                });
            } else {
                showApiError("create execution", result.error.message);
            }
        } catch (error) {
            showError("Failed to create execution", {
                description: error instanceof Error ? error.message : String(error)
            });
        }
    }
}

async function handleCompletionEffects(executions?: ActionExecutionRequest[]): Promise<void> {
    if (executions != undefined && executions.length > 0) {
        await createExecutions(executions);
    }
}

function handleErrorResponse(response: ActionStartErrorResponse | ActionSubmitErrorResponse): void {
    errorState.value = { message: response.message, description: response.description };
    isOpen.value = true;
}

async function handleSubmitResponse(response: ActionSubmitResponse): Promise<void> {
    if (response.kind === "validation") {
        validationErrors.value = response.errors ?? [];
    } else if (response.kind === "nextPage" && response.page) {
        previousPagesStack.value.push({
            page: currentPage.value!,
            inputs: currentInputs.value
        });

        if (nextValuesStack.value.length > 0) {
            const savedValues = nextValuesStack.value.pop()!;
            currentInputs.value = verifyAndFix(savedValues, response.page.schema);
        } else {
            currentInputs.value = generateDefaultValue(response.page.schema);
        }

        validationErrors.value = [];
        currentPage.value = response.page;
    } else if (response.kind === "completion") {
        await handleCompletionEffects(response.executions);
        closeDialog();
    } else if (response.kind === "error") {
        handleErrorResponse(response);
    }
}

function handleCancel(): void {
    closeDialog();
}

function closeDialog(): void {
    isOpen.value = false;
    pendingAction.value = undefined;
}
</script>
