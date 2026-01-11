import { computed, ref } from "vue";
import type { BackendApi, User } from "./api/backendApi";

/**
 * Manager for authentication state
 */
export class AuthState {
    /**
     * Currently authenticated user (if any)
     */
    readonly user = ref<User | undefined>();

    /**
     * Whether a user is currently authenticated
     */
    readonly isAuthenticated = computed(() => this.user.value != undefined);

    /**
     * Convenience flag indicating an administrator
     */
    readonly isAdmin = computed(() => this.user.value?.isAdmin ?? false);

    /**
     * Error message from authentication attempts
     */
    readonly authError = ref<string>();

    /**
     * Whether an authentication request is in progress
     */
    readonly isAuthenticating = ref(false);

    /**
     * Creates a new authentication state manager
     *
     * @param backendApi The backend API instance
     * @param onLogout Optional callback to execute after logout (e.g., to reset workbench)
     */
    constructor(
        readonly backendApi: BackendApi,
        private readonly onLogout?: () => void | Promise<void>
    ) {}

    /**
     * Checks if a user is already authenticated via session
     * Should be called on application startup
     */
    async checkAuthentication(): Promise<void> {
        try {
            const result = await this.backendApi.getCurrentUser();
            if (result.success) {
                this.user.value = result.value;
            }
        } catch (error) {
            // Ignore, user not authenticated
        }
    }

    /**
     * Performs login with the provided credentials
     *
     * @param username The username
     * @param password The password
     * @returns The authenticated user on success
     */
    async login(username: string, password: string): Promise<{ success: boolean; user?: User; error?: string }> {
        this.authError.value = undefined;
        this.isAuthenticating.value = true;
        try {
            const result = await this.backendApi.login(username, password);
            if (!result.success) {
                this.authError.value = result.error.message;
                return { success: false, error: result.error.message };
            }
            this.user.value = result.value;
            return { success: true, user: result.value };
        } finally {
            this.isAuthenticating.value = false;
        }
    }

    /**
     * Performs registration with the provided credentials
     *
     * @param username The username
     * @param password The password
     * @returns The authenticated user on success
     */
    async register(username: string, password: string): Promise<{ success: boolean; user?: User; error?: string }> {
        this.authError.value = undefined;
        this.isAuthenticating.value = true;
        try {
            const result = await this.backendApi.register(username, password);
            if (!result.success) {
                this.authError.value = result.error.message;
                return { success: false, error: result.error.message };
            }
            this.user.value = result.value;
            return { success: true, user: result.value };
        } finally {
            this.isAuthenticating.value = false;
        }
    }

    /**
     * Logs out the current user
     */
    async logout(): Promise<void> {
        await this.backendApi.logout();
        this.user.value = undefined;
        if (this.onLogout) {
            await this.onLogout();
        }
    }

    /**
     * Changes the current user's password
     *
     * @param currentPassword The current password
     * @param newPassword The new password
     */
    async changePassword(currentPassword: string, newPassword: string) {
        return this.backendApi.changePassword(currentPassword, newPassword);
    }
}
