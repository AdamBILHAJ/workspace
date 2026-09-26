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

import { useAuth } from "@/context/AuthContext";
import { getApiErrorMessage } from "@/lib/auth";
import {
  createOrganization,
  createWorkspace,
  listOrganizations,
  listWorkspaces,
} from "@/lib/workspaces";
import type {
  CreateWorkspaceInput,
  Organization,
  Workspace,
} from "@/lib/workspaces";

interface WorkspaceContextValue {
  workspaces: Workspace[];
  organizations: Organization[];
  isLoading: boolean;
  error: string | null;
  refresh: () => Promise<void>;
  createNewWorkspace: (input: CreateWorkspaceInput) => Promise<Workspace>;
}

const WorkspaceContext = createContext<WorkspaceContextValue | undefined>(
  undefined,
);

function byName<T extends { name: string }>(first: T, second: T): number {
  return first.name.localeCompare(second.name);
}

export function WorkspaceProvider({ children }: { children: ReactNode }) {
  const { user, isLoading: isAuthLoading } = useAuth();
  const [workspaces, setWorkspaces] = useState<Workspace[]>([]);
  const [organizations, setOrganizations] = useState<Organization[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const abortControllerRef = useRef<AbortController | null>(null);
  const operationIdRef = useRef(0);
  const isMountedRef = useRef(false);
  const userId = user?.id ?? null;
  const [loadedUserId, setLoadedUserId] = useState<number | null>(userId);

  if (loadedUserId !== userId) {
    setLoadedUserId(userId);
    setWorkspaces([]);
    setOrganizations([]);
    setError(null);
    setIsLoading(userId !== null && !isAuthLoading);
  }

  useEffect(() => {
    isMountedRef.current = true;

    return () => {
      isMountedRef.current = false;
    };
  }, []);

  const cancelPendingRequest = useCallback(() => {
    operationIdRef.current += 1;
    abortControllerRef.current?.abort();
    abortControllerRef.current = null;
  }, []);

  const startRequest = useCallback(() => {
    cancelPendingRequest();
    const controller = new AbortController();
    abortControllerRef.current = controller;

    return { controller, operationId: operationIdRef.current };
  }, [cancelPendingRequest]);

  const loadWorkspaces = useCallback(
    async (controller: AbortController, operationId: number) => {
      const isStale = () =>
        controller.signal.aborted ||
        !isMountedRef.current ||
        operationIdRef.current !== operationId;

      try {
        const [workspaceList, organizationList] = await Promise.all([
          listWorkspaces(controller.signal),
          listOrganizations(controller.signal),
        ]);

        if (isStale()) {
          return;
        }

        setWorkspaces(workspaceList);
        setOrganizations(organizationList);
      } catch (caughtError) {
        if (isStale()) {
          return;
        }

        setError(
          getApiErrorMessage(caughtError, "Unable to load your workspaces."),
        );
      } finally {
        if (abortControllerRef.current === controller) {
          abortControllerRef.current = null;
        }
        if (isMountedRef.current && operationIdRef.current === operationId) {
          setIsLoading(false);
        }
      }
    },
    [],
  );

  const refresh = useCallback(async () => {
    const { controller, operationId } = startRequest();
    setIsLoading(true);
    setError(null);
    await loadWorkspaces(controller, operationId);
  }, [loadWorkspaces, startRequest]);

  useEffect(() => {
    if (isAuthLoading || userId === null) {
      return;
    }

    const { controller, operationId } = startRequest();
    void loadWorkspaces(controller, operationId);

    return () => {
      cancelPendingRequest();
    };
  }, [cancelPendingRequest, isAuthLoading, loadWorkspaces, startRequest, userId]);

  const createNewWorkspace = useCallback(
    async ({
      name,
      organizationName,
      organizationId,
    }: CreateWorkspaceInput) => {
      const workspaceName = name.trim();
      const requestedOrganizationName = organizationName.trim();

      if (!workspaceName) {
        throw new Error("Workspace name is required.");
      }
      if (!requestedOrganizationName && organizationId === undefined) {
        throw new Error("Organization name is required.");
      }

      let targetOrganizationId = organizationId;

      if (targetOrganizationId === undefined) {
        const existingOrganization = organizations.find(
          (organization) =>
            organization.name.trim().toLowerCase() ===
            requestedOrganizationName.toLowerCase(),
        );

        targetOrganizationId = existingOrganization?.id;

        if (targetOrganizationId === undefined) {
          const createdOrganization = await createOrganization(
            requestedOrganizationName,
          );
          targetOrganizationId = createdOrganization.id;

          if (isMountedRef.current) {
            setOrganizations((current) =>
              [...current, createdOrganization].sort(byName),
            );
          }
        }
      }

      const createdWorkspace = await createWorkspace(
        targetOrganizationId,
        workspaceName,
      );

      if (isMountedRef.current) {
        setWorkspaces((current) => [...current, createdWorkspace].sort(byName));
      }

      return createdWorkspace;
    },
    [organizations],
  );

  const value = useMemo(
    () => ({
      workspaces,
      organizations,
      isLoading,
      error,
      refresh,
      createNewWorkspace,
    }),
    [createNewWorkspace, error, isLoading, organizations, refresh, workspaces],
  );

  return (
    <WorkspaceContext.Provider value={value}>
      {children}
    </WorkspaceContext.Provider>
  );
}

export function useWorkspace(): WorkspaceContextValue {
  const context = useContext(WorkspaceContext);

  if (!context) {
    throw new Error("useWorkspace must be used within a WorkspaceProvider");
  }

  return context;
}
