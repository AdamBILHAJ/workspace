"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import type { ReactNode } from "react";

import {
  AUTH_TOKEN_KEY,
  AUTH_UNAUTHORIZED_EVENT,
  clearAuthToken,
  getAuthToken,
  getCurrentUser,
  isUnauthorizedError,
  signIn as signInRequest,
  signUp as signUpRequest,
} from "@/lib/auth";
import type { AuthUser, SignInData, SignUpData } from "@/lib/auth";

interface AuthContextValue {
  user: AuthUser | null;
  isLoading: boolean;
  signIn: (data: SignInData) => Promise<AuthUser>;
  signUp: (data: SignUpData) => Promise<AuthUser>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const operationIdRef = useRef(0);
  const abortControllerRef = useRef<AbortController | null>(null);
  const isMountedRef = useRef(false);

  useEffect(() => {
    let isActive = true;
    isMountedRef.current = true;

    function cancelCurrentRequest(): void {
      abortControllerRef.current?.abort();
      abortControllerRef.current = null;
    }

    function isCurrentOperation(operationId: number): boolean {
      return (
        isActive &&
        isMountedRef.current &&
        operationIdRef.current === operationId
      );
    }

    function beginOperation(): {
      operationId: number;
      controller: AbortController;
    } {
      cancelCurrentRequest();
      const controller = new AbortController();
      abortControllerRef.current = controller;
      const operationId = ++operationIdRef.current;
      setIsLoading(true);
      return { operationId, controller };
    }

    function clearSession(): void {
      operationIdRef.current += 1;
      cancelCurrentRequest();
      clearAuthToken();
      if (isActive) {
        setUser(null);
        setIsLoading(false);
      }
    }

    async function hydrateSession(): Promise<void> {
      const token = getAuthToken();
      const { operationId, controller } = beginOperation();

      if (!token) {
        if (isCurrentOperation(operationId)) {
          setUser(null);
          setIsLoading(false);
        }
        return;
      }

      try {
        const currentUser = await getCurrentUser(controller.signal);
        if (isCurrentOperation(operationId)) {
          setUser(currentUser);
        }
      } catch (error) {
        if (!isCurrentOperation(operationId)) {
          return;
        }

        if (isUnauthorizedError(error) && getAuthToken() === token) {
          clearAuthToken();
        }
        setUser(null);
      } finally {
        if (abortControllerRef.current === controller) {
          abortControllerRef.current = null;
        }
        if (isCurrentOperation(operationId)) {
          setIsLoading(false);
        }
      }
    }

    function handleStorageChange(event: StorageEvent): void {
      if (event.key !== null && event.key !== AUTH_TOKEN_KEY) {
        return;
      }

      if (event.newValue) {
        void hydrateSession();
      } else {
        clearSession();
      }
    }

    window.addEventListener(AUTH_UNAUTHORIZED_EVENT, clearSession);
    window.addEventListener("storage", handleStorageChange);
    void hydrateSession();

    return () => {
      isActive = false;
      isMountedRef.current = false;
      operationIdRef.current += 1;
      cancelCurrentRequest();
      window.removeEventListener(AUTH_UNAUTHORIZED_EVENT, clearSession);
      window.removeEventListener("storage", handleStorageChange);
    };
  }, []);

  const signIn = useCallback(async (data: SignInData) => {
    abortControllerRef.current?.abort();
    const controller = new AbortController();
    abortControllerRef.current = controller;
    const operationId = ++operationIdRef.current;
    setIsLoading(true);

    try {
      const authenticatedUser = await signInRequest(data, controller.signal);
      if (
        !isMountedRef.current ||
        operationIdRef.current !== operationId
      ) {
        throw new Error("Your session changed. Please try again.");
      }
      setUser(authenticatedUser);
      return authenticatedUser;
    } catch (error) {
      if (controller.signal.aborted) {
        throw new Error("Your session changed. Please try again.");
      }
      throw error;
    } finally {
      if (abortControllerRef.current === controller) {
        abortControllerRef.current = null;
      }
      if (
        isMountedRef.current &&
        operationIdRef.current === operationId
      ) {
        setIsLoading(false);
      }
    }
  }, []);

  const signUp = useCallback(async (data: SignUpData) => {
    abortControllerRef.current?.abort();
    const controller = new AbortController();
    abortControllerRef.current = controller;
    const operationId = ++operationIdRef.current;
    setIsLoading(true);

    try {
      const authenticatedUser = await signUpRequest(data, controller.signal);
      if (
        !isMountedRef.current ||
        operationIdRef.current !== operationId
      ) {
        throw new Error("Your session changed. Please try again.");
      }
      setUser(authenticatedUser);
      return authenticatedUser;
    } catch (error) {
      if (controller.signal.aborted) {
        throw new Error("Your session changed. Please try again.");
      }
      throw error;
    } finally {
      if (abortControllerRef.current === controller) {
        abortControllerRef.current = null;
      }
      if (
        isMountedRef.current &&
        operationIdRef.current === operationId
      ) {
        setIsLoading(false);
      }
    }
  }, []);

  const logout = useCallback(() => {
    operationIdRef.current += 1;
    abortControllerRef.current?.abort();
    abortControllerRef.current = null;
    clearAuthToken();
    if (isMountedRef.current) {
      setUser(null);
      setIsLoading(false);
    }
  }, []);

  const value = useMemo(
    () => ({ user, isLoading, signIn, signUp, logout }),
    [isLoading, logout, signIn, signUp, user],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }

  return context;
}
