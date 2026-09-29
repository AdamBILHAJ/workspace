import api from "@/lib/api";

export type WorkspaceRole = "OWNER" | "ADMIN" | "MEMBER";

/**
 * Global account role (user/domain/Role.java), distinct from the per-workspace
 * WorkspaceRole above. A MEMBER of a workspace is usually a ROLE_USER; the two
 * axes are unrelated and must not be conflated.
 */
export type GlobalRole = "ROLE_USER" | "ROLE_ADMIN";

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

export interface WorkspaceMember {
  id: number;
  role: WorkspaceRole;
  user: {
    id: number;
    email: string;
    firstName: string;
    lastName: string;
    role: GlobalRole;
  };
}

export interface AddMemberRequest {
  email: string;
}

export async function listWorkspaceMembers(
  workspaceId: number,
  signal?: AbortSignal,
): Promise<WorkspaceMember[]> {
  const response = await api.get<WorkspaceMember[]>(
    `/api/v1/workspaces/${workspaceId}/members`,
    { signal },
  );
  return response.data;
}

/**
 * Adds an existing user to a workspace. The server always assigns the MEMBER
 * role and normalises the email itself, so no role is sent. Requires OWNER or
 * ADMIN (403), the user to already be registered (404), and to not already be
 * a member (409).
 */
export async function addWorkspaceMember(
  workspaceId: number,
  payload: AddMemberRequest,
): Promise<WorkspaceMember> {
  const response = await api.post<WorkspaceMember>(
    `/api/v1/workspaces/${workspaceId}/members`,
    { email: payload.email.trim().toLowerCase() },
  );
  return response.data;
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
