import api from "@/lib/api";

export type TaskPriority = "LOW" | "MEDIUM" | "HIGH" | "URGENT";

export interface Project {
  id: number;
  name: string;
  key: string;
  description: string | null;
  workspaceId: number;
  totalTasks: number;
  completedTasks: number;
  completionPercent: number;
}

export interface TaskUserSummary {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
}

export interface Task {
  id: number;
  projectId: number;
  columnId: number;
  title: string;
  description: string | null;
  priority: TaskPriority;
  orderIndex: number;
  assignee: TaskUserSummary | null;
  reporter: TaskUserSummary;
}

export interface KanbanColumn {
  id: number;
  name: string;
  orderIndex: number;
  tasks: Task[];
}

export interface Board {
  project: {
    id: number;
    name: string;
    key: string;
    description: string | null;
  };
  columns: KanbanColumn[];
}

export interface CreateProjectInput {
  name: string;
  description?: string;
  key?: string;
}

export interface CreateTaskInput {
  title: string;
  description?: string;
  priority: TaskPriority;
  assigneeId?: number | null;
}

export const TASK_PRIORITIES: TaskPriority[] = [
  "LOW",
  "MEDIUM",
  "HIGH",
  "URGENT",
];

export async function listProjects(
  workspaceId: number,
  signal?: AbortSignal,
): Promise<Project[]> {
  const response = await api.get<Project[]>(
    `/api/v1/workspaces/${workspaceId}/projects`,
    { signal },
  );
  return response.data;
}

export async function createProject(
  workspaceId: number,
  input: CreateProjectInput,
): Promise<Project> {
  const response = await api.post<Project>(
    `/api/v1/workspaces/${workspaceId}/projects`,
    input,
  );
  return response.data;
}

export async function getProjectBoard(
  projectId: number,
  signal?: AbortSignal,
): Promise<Board> {
  const response = await api.get<Board>(`/api/v1/projects/${projectId}/board`, {
    signal,
  });
  return response.data;
}

export async function addColumn(
  projectId: number,
  name: string,
): Promise<KanbanColumn> {
  const response = await api.post<KanbanColumn>(
    `/api/v1/projects/${projectId}/columns`,
    { name },
  );
  return response.data;
}

export async function createTask(
  columnId: number,
  input: CreateTaskInput,
): Promise<Task> {
  const response = await api.post<Task>(`/api/v1/columns/${columnId}/tasks`, {
    title: input.title,
    description: input.description ?? null,
    priority: input.priority,
    assigneeId: input.assigneeId ?? null,
  });
  return response.data;
}

export async function moveTask(
  taskId: number,
  columnId: number,
  orderIndex: number,
): Promise<Task> {
  const response = await api.patch<Task>(
    `/api/v1/tasks/${taskId}/move`,
    { columnId, orderIndex },
  );
  return response.data;
}
