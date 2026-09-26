import api from "@/lib/api";

export type WorkspaceRole = "OWNER" | "ADMIN" | "MEMBER";

export interface Organization {
  id: number;
  name: string;
  slug: string;
}

export interface Workspace {
  id: number;
  name: string;
  slug: string;
  organization: Organization;
  role: WorkspaceRole;
}

export interface CreateWorkspaceInput {
  name: string;
  organizationName: string;
  organizationId?: number;
}

export async function listWorkspaces(signal?: AbortSignal): Promise<Workspace[]> {
  const response = await api.get<Workspace[]>("/api/v1/workspaces", { signal });
  return response.data;
}

export async function listOrganizations(
  signal?: AbortSignal,
): Promise<Organization[]> {
  const response = await api.get<Organization[]>("/api/v1/organizations", {
    signal,
  });
  return response.data;
}

export async function createOrganization(
  name: string,
): Promise<Organization> {
  const response = await api.post<Organization>("/api/v1/organizations", {
    name,
  });
  return response.data;
}

export async function createWorkspace(
  organizationId: number,
  name: string,
): Promise<Workspace> {
  const response = await api.post<Workspace>(
    `/api/v1/organizations/${organizationId}/workspaces`,
    { name },
  );
  return response.data;
}
