import axios from "axios";

export const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_BASE_URL,
  timeout: 15_000,
  headers: {
    Accept: "application/json",
    "Content-Type": "application/json",
  },
});

export interface TaskComment {
  id: number;
  taskId: number;
  content: string;
  authorId: number;
  authorName: string;
  authorEmail: string;
  createdAt: string;
}

export type ActivityAction =
  | "TASK_CREATED"
  | "TASK_MOVED"
  | "STATUS_CHANGED"
  | "COMMENT_ADDED";

export interface TaskActivity {
  id: number;
  taskId: number;
  action: ActivityAction;
  details: string | null;
  actorId: number;
  actorName: string;
  createdAt: string;
}

export interface AppNotification {
  id: number;
  title: string;
  message: string;
  isRead: boolean;
  targetUrl: string | null;
  createdAt: string;
}

export interface NotificationsResponse {
  notifications: AppNotification[];
  unreadCount: number;
}

export async function getTaskComments(
  taskId: number,
  signal?: AbortSignal,
): Promise<TaskComment[]> {
  const response = await api.get<TaskComment[]>(
    `/api/v1/tasks/${taskId}/comments`,
    { signal },
  );
  return response.data;
}

export async function addComment(
  taskId: number,
  content: string,
): Promise<TaskComment> {
  const response = await api.post<TaskComment>(
    `/api/v1/tasks/${taskId}/comments`,
    { content },
  );
  return response.data;
}

export async function getTaskActivity(
  taskId: number,
  signal?: AbortSignal,
): Promise<TaskActivity[]> {
  const response = await api.get<TaskActivity[]>(
    `/api/v1/tasks/${taskId}/activity`,
    { signal },
  );
  return response.data;
}

export async function getNotifications(
  signal?: AbortSignal,
): Promise<NotificationsResponse> {
  const response = await api.get<NotificationsResponse>("/api/v1/notifications", {
    signal,
  });
  return response.data;
}

export async function markNotificationRead(
  notificationId: number,
): Promise<AppNotification> {
  const response = await api.patch<AppNotification>(
    `/api/v1/notifications/${notificationId}/read`,
  );
  return response.data;
}

export async function markAllNotificationsRead(): Promise<number> {
  const response = await api.patch<{ updatedCount: number }>(
    "/api/v1/notifications/read-all",
  );
  return response.data.updatedCount;
}

export default api;
