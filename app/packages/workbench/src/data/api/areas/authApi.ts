import { ApiResult, CommonErrorCode, type CommonError } from "../apiResult";
import type { BackendApiCore } from "../backendApi";

/**
 * User information returned from authentication operations
 */
export interface User {
    id: string;
    username: string;
    isAdmin: boolean;
}

/**
 * API for authentication operations including login, logout, registration,
 * and password management.
 */
export class AuthApi {
    /**
     * Creates a new AuthApi instance
     *
     * @param core The core backend API providing HTTP utilities
     */
    constructor(private readonly core: BackendApiCore) {}

    /**
     * Gets the currently authenticated user
     *
     * @returns A promise resolving to the current user or an error
     */
    async getCurrentUser(): Promise<ApiResult<User, CommonError>> {
        try {
            const response = await fetch(`${this.core.baseUrl}/auth/me`, {
                method: "GET",
                credentials: "include"
            });

            if (!response.ok) {
                if (response.status === 401) {
                    return ApiResult.commonFailure(CommonErrorCode.Unknown, "Not authenticated");
                }
                return ApiResult.commonFailure(CommonErrorCode.Unavailable, "Failed to get current user");
            }

            const data = await response.json();
            return ApiResult.success(data);
        } catch (error) {
            return ApiResult.commonFailure(CommonErrorCode.Unavailable, String(error));
        }
    }

    /**
     * Authenticates a user with username and password
     *
     * @param username The username to authenticate
     * @param password The password for authentication
     * @returns A promise resolving to the authenticated user or an error
     */
    async login(username: string, password: string): Promise<ApiResult<User, CommonError>> {
        try {
            const response = await fetch(`${this.core.baseUrl}/auth/login`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                credentials: "include",
                body: JSON.stringify({ username, password })
            });

            if (!response.ok) {
                if (response.status === 401) {
                    return ApiResult.commonFailure(CommonErrorCode.Unknown, "Invalid credentials");
                }
                return ApiResult.commonFailure(CommonErrorCode.Unavailable, "Login failed");
            }

            const data = await response.json();
            return ApiResult.success(data.user);
        } catch (error) {
            return ApiResult.commonFailure(CommonErrorCode.Unavailable, String(error));
        }
    }

    /**
     * Registers a new user account
     *
     * @param username The username for the new account
     * @param password The password for the new account
     * @returns A promise resolving to the created user or an error
     */
    async register(username: string, password: string): Promise<ApiResult<User, CommonError>> {
        try {
            const response = await fetch(`${this.core.baseUrl}/auth/register`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                credentials: "include",
                body: JSON.stringify({ username, password })
            });

            if (!response.ok) {
                if (response.status === 409) {
                    return ApiResult.commonFailure(CommonErrorCode.Unknown, "Username already exists");
                }
                return ApiResult.commonFailure(CommonErrorCode.Unavailable, "Registration failed");
            }

            const data = await response.json();
            return ApiResult.success(data.user);
        } catch (error) {
            return ApiResult.commonFailure(CommonErrorCode.Unavailable, String(error));
        }
    }

    /**
     * Logs out the currently authenticated user
     */
    async logout(): Promise<void> {
        await fetch(`${this.core.baseUrl}/auth/logout`, {
            method: "POST",
            credentials: "include"
        });
    }

    /**
     * Changes the current user's password
     *
     * @param currentPassword The current password for verification
     * @param newPassword The new password to set
     * @returns A promise resolving to success or an error
     */
    async changePassword(currentPassword: string, newPassword: string): Promise<ApiResult<void, CommonError>> {
        try {
            const response = await fetch(`${this.core.baseUrl}/auth/password`, {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                credentials: "include",
                body: JSON.stringify({ currentPassword, newPassword })
            });

            if (!response.ok) {
                if (response.status === 400) {
                    return ApiResult.commonFailure(CommonErrorCode.Unknown, "Current password is incorrect");
                }
                return ApiResult.commonFailure(CommonErrorCode.Unavailable, "Failed to change password");
            }

            return ApiResult.success(undefined);
        } catch (error) {
            return ApiResult.commonFailure(CommonErrorCode.Unavailable, String(error));
        }
    }
}
