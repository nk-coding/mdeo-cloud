<template>
    <div class="w-full max-w-md space-y-6">
        <Card>
            <CardHeader class="gap-4 text-center">
                <div class="flex items-center justify-center gap-3">
                    <Icon :showText="true" class="h-14 w-14" />
                    <p class="text-lg font-semibold tracking-[0.15em] text-foreground">MDEO</p>
                </div>
                <div>
                    <CardTitle>{{ headerText }}</CardTitle>
                </div>
            </CardHeader>
            <CardContent class="pt-0">
                <form @submit.prevent="handleSubmit">
                    <FieldGroup>
                        <Field>
                            <FieldLabel for="login-username">Username</FieldLabel>
                            <FieldContent>
                                <Input
                                    id="login-username"
                                    v-model="username"
                                    type="text"
                                    autocomplete="username"
                                    placeholder="jane.doe"
                                />
                            </FieldContent>
                        </Field>

                        <Field>
                            <FieldLabel for="login-password">Password</FieldLabel>
                            <FieldContent>
                                <PasswordField
                                    id="login-password"
                                    v-model="password"
                                    autocomplete="current-password"
                                    placeholder="Enter your password"
                                />
                            </FieldContent>
                        </Field>

                        <Field v-if="computedError">
                            <FieldError :errors="[computedError]" />
                        </Field>

                        <Field>
                            <FieldContent>
                                <Button type="submit" class="w-full" :disabled="authState.isAuthenticating.value">
                                    <span v-if="!authState.isAuthenticating.value">{{ submitLabel }}</span>
                                    <span v-else class="animate-pulse">{{ submittingLabel }}</span>
                                </Button>
                            </FieldContent>
                            <FieldDescription class="text-center">
                                {{ togglePrompt }}
                                <button
                                    type="button"
                                    class="font-semibold text-primary underline-offset-4 hover:underline"
                                    @click="toggleMode"
                                >
                                    {{ toggleAction }}
                                </button>
                            </FieldDescription>
                        </Field>
                    </FieldGroup>
                </form>
            </CardContent>
        </Card>
    </div>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import Icon from "../Icon.vue";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Field, FieldContent, FieldDescription, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field";
import PasswordField from "./PasswordField.vue";

import type { AuthState } from "@/data/authState";

const props = defineProps<{
    authState: AuthState;
}>();

const emit = defineEmits<{
    (e: "success"): void;
}>();

type AuthMode = "login" | "signup";

const username = ref("");
const password = ref("");
const localError = ref<string>();
const computedError = computed(() => localError.value ?? props.authState.authError.value);
const mode = ref<AuthMode>("login");
const isSignup = computed(() => mode.value === "signup");

const headerText = computed(() => (isSignup.value ? "Create your account" : "Login to continue"));
const submitLabel = computed(() => (isSignup.value ? "Create account" : "Continue"));
const submittingLabel = computed(() => (isSignup.value ? "Creating account…" : "Signing in…"));
const togglePrompt = computed(() => (isSignup.value ? "Already have an account?" : "Don't have an account?"));
const toggleAction = computed(() => (isSignup.value ? "Sign in" : "Create one"));

async function handleSubmit() {
    if (!username.value || !password.value) {
        localError.value = "Please enter both a username and password.";
        return;
    }
    localError.value = undefined;

    const result =
        mode.value === "signup"
            ? await props.authState.register(username.value, password.value)
            : await props.authState.login(username.value, password.value);

    if (result.success) {
        emit("success");
    }
}

function toggleMode() {
    mode.value = mode.value === "signup" ? "login" : "signup";
    localError.value = undefined;
    props.authState.authError.value = undefined;
}
</script>
