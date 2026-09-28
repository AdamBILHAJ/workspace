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
import {
  getNotifications,
  markAllNotificationsRead,
  markNotificationRead,
  type AppNotification,
  type NotificationSocketEvent,
} from "@/lib/api";
import { getApiErrorMessage, getAuthToken } from "@/lib/auth";
import {
  MARK_ALL_READ_DESTINATION,
  MARK_READ_DESTINATION,
  NotificationSocket,
} from "@/lib/notificationSocket";

interface NotificationContextValue {
  notifications: AppNotification[];
  unreadCount: number;
  isLoading: boolean;
  isMutating: boolean;
  /** False while the socket is down; actions then fall back to REST. */
  isConnected: boolean;
  error: string | null;
  clearError: () => void;
  retry: () => void;
  markAsRead: (notificationId: number) => Promise<void>;
  markAllAsRead: () => Promise<void>;
}

const NotificationContext = createContext<NotificationContextValue | undefined>(
  undefined,
);

/**
 * Single source of truth for the notification list.
 *
 * <p>REST supplies the initial load and the offline fallback; the socket then
 * keeps the list current. Because the server echoes an authoritative
 * `unreadCount` on every event, the badge is driven by the server rather than
 * guessed at, and "mark all read" converges across every tab and session of the
 * same user without extra coordination.
 */
export function NotificationProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth();

  const [notifications, setNotifications] = useState<AppNotification[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [isMutating, setIsMutating] = useState(false);
  const [isSocketConnected, setIsSocketConnected] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [reloadToken, setReloadToken] = useState(0);

  const socketRef = useRef<NotificationSocket | null>(null);
  const isMountedRef = useRef(false);

  const applyEvent = useCallback((event: NotificationSocketEvent) => {
    setUnreadCount(event.unreadCount);

    switch (event.type) {
      case "NEW_NOTIFICATION":
        setNotifications((current) => {
          // A refetch that raced the push can deliver the same row twice.
          if (current.some((item) => item.id === event.payload.id)) {
            return current;
          }
          return [event.payload, ...current];
        });
        break;
      case "NOTIFICATION_READ":
        setNotifications((current) =>
          current.map((item) =>
            item.id === event.payload.id ? { ...item, isRead: true } : item,
          ),
        );
        break;
      case "ALL_READ":
        setNotifications((current) =>
          current.map((item) => ({ ...item, isRead: true })),
        );
        break;
    }
  }, []);

  // A session without a user can never be live, regardless of when the close
  // callback lands.
  const isConnected = user !== null && isSocketConnected;

  // Initial load and manual retry. The socket deliberately does not replay
  // anything missed while offline, so this is the recovery path.
  useEffect(() => {
    isMountedRef.current = true;
    const controller = new AbortController();
    let isActive = true;

    async function load(): Promise<void> {
      try {
        const data = await getNotifications(controller.signal);

        if (isActive) {
          setNotifications(data.notifications);
          setUnreadCount(data.unreadCount);
        }
      } catch (caughtError) {
        if (isActive && !controller.signal.aborted) {
          setError(
            getApiErrorMessage(caughtError, "Unable to load notifications."),
          );
        }
      } finally {
        if (isActive) {
          setIsLoading(false);
        }
      }
    }

    void load();

    return () => {
      isActive = false;
      isMountedRef.current = false;
      controller.abort();
    };
  }, [reloadToken]);

  // Socket lifetime follows the authenticated session.
  useEffect(() => {
    if (!user) {
      socketRef.current?.disconnect();
      socketRef.current = null;
      return;
    }

    const token = getAuthToken();

    if (!token) {
      return;
    }

    const socket = new NotificationSocket();
    socketRef.current = socket;

    socket.connect(token, {
      onEvent: applyEvent,
      onConnected: () => {
        if (isMountedRef.current) {
          setIsSocketConnected(true);
        }
      },
      onDisconnected: () => {
        if (isMountedRef.current) {
          setIsSocketConnected(false);
        }
      },
      onFailure: (message) => {
        if (!isMountedRef.current) {
          return;
        }
        setIsSocketConnected(false);
        setError(message);
        // A refused action means local state may be ahead of the server.
        setReloadToken((token_) => token_ + 1);
      },
    });

    return () => {
      socket.disconnect();
      if (socketRef.current === socket) {
        socketRef.current = null;
      }
    };
  }, [user, applyEvent]);

  const markAsRead = useCallback(async (notificationId: number) => {
    const socket = socketRef.current;

    if (socket?.isConnected) {
      // Optimistic: the badge moves now, and the server event confirms it.
      setNotifications((current) =>
        current.map((item) =>
          item.id === notificationId ? { ...item, isRead: true } : item,
        ),
      );
      setUnreadCount((current) => Math.max(0, current - 1));

      if (
        socket.publish(
          MARK_READ_DESTINATION,
          JSON.stringify({ id: notificationId }),
        )
      ) {
        return;
      }
    }

    try {
      const updated = await markNotificationRead(notificationId);
      setNotifications((current) =>
        current.map((item) => (item.id === updated.id ? updated : item)),
      );
      setUnreadCount((current) => Math.max(0, current - 1));
    } catch (caughtError) {
      setError(
        getApiErrorMessage(caughtError, "Unable to mark that as read."),
      );
    }
  }, []);

  const markAllAsRead = useCallback(async () => {
    if (isMutating) {
      return;
    }

    setIsMutating(true);
    setError(null);

    const socket = socketRef.current;

    try {
      if (socket?.isConnected) {
        setNotifications((current) =>
          current.map((item) => ({ ...item, isRead: true })),
        );
        setUnreadCount(0);

        if (socket.publish(MARK_ALL_READ_DESTINATION, "")) {
          return;
        }
      }

      await markAllNotificationsRead();
      setNotifications((current) =>
        current.map((item) => ({ ...item, isRead: true })),
      );
      setUnreadCount(0);
    } catch (caughtError) {
      setError(
        getApiErrorMessage(caughtError, "Unable to mark notifications as read."),
      );
    } finally {
      setIsMutating(false);
    }
  }, [isMutating]);

  const value = useMemo<NotificationContextValue>(
    () => ({
      notifications,
      unreadCount,
      isLoading,
      isMutating,
      isConnected,
      error,
      clearError: () => setError(null),
      retry: () => {
        setIsLoading(true);
        setReloadToken((token) => token + 1);
      },
      markAsRead,
      markAllAsRead,
    }),
    [
      notifications,
      unreadCount,
      isLoading,
      isMutating,
      isConnected,
      error,
      markAsRead,
      markAllAsRead,
    ],
  );

  return (
    <NotificationContext.Provider value={value}>
      {children}
    </NotificationContext.Provider>
  );
}

export function useNotifications(): NotificationContextValue {
  const context = useContext(NotificationContext);

  if (!context) {
    throw new Error("useNotifications must be used within NotificationProvider");
  }

  return context;
}
