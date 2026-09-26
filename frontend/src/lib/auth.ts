import { isAxiosError } from "axios";
import type { InternalAxiosRequestConfig } from "axios";

import api from "@/lib/api";

export const AUTH_TOKEN_KEY = "team_workspace_access_token";
export const AUTH_UNAUTHORIZED_EVENT = "team-workspace:unauthorized";

export interface AuthUser {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
}

export interface SignInData {
  email: string;
  password: string;
}

export interface SignUpData extends SignInData {
  firstName: string;
  lastName: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: "Bearer";
  userSummary: AuthUser;
}

interface ApiErrorPayload {
  message?: unknown;
  fieldErrors?: Record<string, unknown>;
}

interface AuthenticatedRequestConfig extends InternalAxiosRequestConfig {
  authToken?: string;
}

export function getAuthToken(): string | null {
  if (typeof window === "undefined") {
    return null;
  }

  try {
    return window.localStorage.getItem(AUTH_TOKEN_KEY);
  } catch {
    return null;
  }
}

export function clearAuthToken(): void {
  if (typeof window === "undefined") {
    return;
  }

  try {
    window.localStorage.removeItem(AUTH_TOKEN_KEY);
  } catch {
    return;
  }
}

function saveAuthToken(token: string): void {
  if (typeof window === "undefined") {
    throw new Error("Authentication storage is unavailable");
  }

  try {
    window.localStorage.setItem(AUTH_TOKEN_KEY, token);
  } catch {
    throw new Error("Unable to save your session");
  }
}

export function isUnauthorizedError(error: unknown): boolean {
  return isAxiosError(error) && error.response?.status === 401;
}

api.interceptors.request.use((config) => {
  const token = getAuthToken();

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
    (config as AuthenticatedRequestConfig).authToken = token;
  }

  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (
      isUnauthorizedError(error) &&
      isAxiosError<unknown>(error) &&
      typeof window !== "undefined"
    ) {
      const requestConfig = error.config as
        | AuthenticatedRequestConfig
        | undefined;
      const isCredentialRequest =
        requestConfig?.url === "/api/v1/auth/signin" ||
        requestConfig?.url === "/api/v1/auth/signup";
      const requestToken = requestConfig?.authToken;

      if (
        requestToken &&
        requestToken === getAuthToken() &&
        !isCredentialRequest
      ) {
        window.dispatchEvent(new Event(AUTH_UNAUTHORIZED_EVENT));
      }
    }

    return Promise.reject(error);
  },
);

export async function signIn(
  data: SignInData,
  signal?: AbortSignal,
): Promise<AuthUser> {
  const response = await api.post<AuthResponse>(
    "/api/v1/auth/signin",
    data,
    { signal },
  );
  saveAuthToken(response.data.accessToken);
  return response.data.userSummary;
}

export async function signUp(
  data: SignUpData,
  signal?: AbortSignal,
): Promise<AuthUser> {
  const response = await api.post<AuthResponse>(
    "/api/v1/auth/signup",
    data,
    { signal },
  );
  saveAuthToken(response.data.accessToken);
  return response.data.userSummary;
}

export async function getCurrentUser(signal?: AbortSignal): Promise<AuthUser> {
  const response = await api.get<AuthUser>("/api/v1/auth/me", { signal });
  return response.data;
}

export function getApiErrorMessage(
  error: unknown,
  fallbackMessage: string,
): string {
  if (isAxiosError<ApiErrorPayload>(error)) {
    const message = error.response?.data?.message;
    if (typeof message === "string" && message.length > 0) {
      return message;
    }

    const fieldError = Object.values(error.response?.data?.fieldErrors ?? {}).find(
      (value): value is string => typeof value === "string" && value.length > 0,
    );
    if (fieldError) {
      return fieldError;
    }
  }

  if (error instanceof Error && error.message.length > 0) {
    return error.message;
  }

  return fallbackMessage;
}
