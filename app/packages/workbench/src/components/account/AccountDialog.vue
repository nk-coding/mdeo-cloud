<template>
    <Dialog v-model:open="open">
        <DialogContent class="sm:max-w-md">
            <DialogHeader>
                <DialogTitle>Account</DialogTitle>
            </DialogHeader>

            <div class="space-y-5">
                <div class="flex items-center gap-4 rounded-3xl border border-border/70 bg-muted/40 p-4">
                    <div class="rounded-2xl bg-primary/10 p-3 text-primary">
                        <UserRound class="h-5 w-5" />
                    </div>
                    <div>
                        <p class="text-sm font-semibold text-foreground">{{ username }}</p>
                        <p class="text-xs text-muted-foreground">Signed in to MDEO</p>
                    </div>
                </div>

                <Collapsible v-model:open="isPasswordSectionOpen">
                    <CollapsibleTrigger asChild>
                        <Button variant="outline" class="w-full">
                            <component :is="isPasswordSectionOpen ? ChevronUp : ChevronDown" class="h-4 w-4" />
                            <span>Change password</span>
                        </Button>
                    </CollapsibleTrigger>
                    <CollapsibleContent>
                        <form class="mt-4 space-y-4" @submit.prevent="handlePasswordChange">
                            <FieldGroup>
                                <Field>
                                    <FieldLabel for="current-password">Current password</FieldLabel>
                                    <FieldContent>
                                        <Input
                                            id="current-password"
                                            v-model="currentPassword"
                                            type="password"
                                            autocomplete="current-password"
                                            placeholder="••••••••"
                                        />
                                    </FieldContent>
                                </Field>

                                <Field>
                                    <FieldLabel for="new-password">New password</FieldLabel>
                                    <FieldContent>
                                        <Input
                                            id="new-password"
                                            v-model="newPassword"
                                            type="password"
                                            autocomplete="new-password"
                                            placeholder="Choose something secure"
                                        />
                                    </FieldContent>
                                </Field>

                                <Field>
                                    <FieldLabel for="confirm-password">Confirm new password</FieldLabel>
                                    <FieldContent>
                                        <Input
                                            id="confirm-password"
                                            v-model="confirmPassword"
                                            type="password"
                                            autocomplete="new-password"
                                            placeholder="Repeat new password"
                                        />
                                    </FieldContent>
                                </Field>

                                <Field v-if="passwordError">
                                    <FieldError :errors="[passwordError]" />
                                </Field>

                                <Field v-if="passwordSuccess">
                                    <FieldContent>
                                        <div class="rounded-lg border border-emerald-400/40 bg-emerald-400/10 px-4 py-2 text-sm text-emerald-500">
                                            {{ passwordSuccess }}
                                        </div>
                                    </FieldContent>
                                </Field>

                                <Field>
                                    <FieldContent>
                                        <Button type="submit" class="w-full" :disabled="isUpdatingPassword">
                                            <span v-if="!isUpdatingPassword">Update password</span>
                                            <span v-else class="animate-pulse">Updating…</span>
                                        </Button>
                                    </FieldContent>
                                </Field>
                            </FieldGroup>
                        </form>
                    </CollapsibleContent>
                </Collapsible>

                <div class="flex flex-wrap justify-between gap-3">
                    <Button type="button" variant="destructive" class="flex-1" @click="handleLogout" :disabled="isLoggingOut">
                        <LogOut class="h-4 w-4" />
                        <span>{{ isLoggingOut ? "Logging out…" : "Log out" }}</span>
                    </Button>
                    <DialogClose asChild>
                        <Button type="button" variant="ghost" class="flex-1">Close</Button>
                    </DialogClose>
                </div>
            </div>
        </DialogContent>
    </Dialog>
</template>

<script setup lang="ts">
import { computed, inject, ref } from "vue";
import { UserRound, LogOut, ChevronDown, ChevronUp } from "lucide-vue-next";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogClose } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Field, FieldContent, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "@/components/ui/collapsible";
import { authStateKey } from "../workbench/util";

const open = defineModel<boolean>("open", { default: false });

const injectedAuthState = inject(authStateKey);
if (injectedAuthState == undefined) {
    throw new Error("AccountDialog requires an auth state");
}
const authState = injectedAuthState;

const currentPassword = ref("");
const newPassword = ref("");
const confirmPassword = ref("");
const passwordError = ref<string>();
const passwordSuccess = ref<string>();
const isUpdatingPassword = ref(false);
const isLoggingOut = ref(false);
const isPasswordSectionOpen = ref(false);

const username = computed(() => authState.user.value?.username ?? "Unknown user");

async function handlePasswordChange() {
    if (isUpdatingPassword.value) {
        return;
    }
    passwordError.value = undefined;
    passwordSuccess.value = undefined;

    if (!currentPassword.value || !newPassword.value) {
        passwordError.value = "Please provide your current and new password.";
        return;
    }

    if (newPassword.value !== confirmPassword.value) {
        passwordError.value = "New passwords do not match.";
        return;
    }

    isUpdatingPassword.value = true;
    try {
        const result = await authState.changePassword(currentPassword.value, newPassword.value);
        if (!result.success) {
            passwordError.value = result.error.message;
            return;
        }
        passwordSuccess.value = "Password updated successfully.";
        currentPassword.value = "";
        newPassword.value = "";
        confirmPassword.value = "";
    } finally {
        isUpdatingPassword.value = false;
    }
}

async function handleLogout() {
    if (isLoggingOut.value) {
        return;
    }
    isLoggingOut.value = true;
    try {
        await authState.logout();
        open.value = false;
    } finally {
        isLoggingOut.value = false;
    }
}
</script>
