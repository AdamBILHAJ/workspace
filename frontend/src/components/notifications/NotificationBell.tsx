"use client";

import { Bell, CheckCheck, LoaderCircle } from "lucide-react";
import Link from "next/link";
import { useCallback, useEffect, useRef, useState } from "react";

import {
  getNotifications,
  markAllNotificationsRead,
  markNotificationRead,
  type AppNotification,
} from "@/lib/api";
import { getApiErrorMessage } from "@/lib/auth";

function formatRelative(value: string): string {
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return "";
  }

  const seconds = Math.round((Date.now() - parsed.getTime()) / 1000);

  if (seconds < 60) return "just now";
  if (seconds < 3600) return `${Math.floor(seconds / 60)}m ago`;
  if (seconds < 86400) return `${Math.floor(seconds / 3600)}h ago`;
  if (seconds < 604800) return `${Math.floor(seconds / 86400)}d ago`;

  return parsed.toLocaleDateString();
}

export function NotificationBell() {
  const [notifications, setNotifications] = useState<AppNotification[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [isOpen, setIsOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [isMutating, setIsMutating] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [reloadToken, setReloadToken] = useState(0);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
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
      controller.abort();
    };
  }, [reloadToken]);

  useEffect(() => {
    if (!isOpen) {
      return;
    }

    function handlePointerDown(event: MouseEvent) {
      if (!containerRef.current?.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }

    function handleKeyDown(event: globalThis.KeyboardEvent) {
      if (event.key === "Escape") {
        setIsOpen(false);
      }
    }

    document.addEventListener("mousedown", handlePointerDown);
    document.addEventListener("keydown", handleKeyDown);

    return () => {
      document.removeEventListener("mousedown", handlePointerDown);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [isOpen]);

  const handleOpen = useCallback(() => {
    setIsOpen((current) => !current);
  }, []);

  async function handleMarkAllRead() {
    if (isMutating) {
      return;
    }

    setIsMutating(true);
    setError(null);

    try {
      await markAllNotificationsRead();
      setNotifications((current) =>
        current.map((notification) => ({ ...notification, isRead: true })),
      );
      setUnreadCount(0);
    } catch (caughtError) {
      setError(
        getApiErrorMessage(caughtError, "Unable to mark notifications as read."),
      );
    } finally {
      setIsMutating(false);
    }
  }

  async function handleOpenNotification(notification: AppNotification) {
    if (!notification.isRead) {
      try {
        await markNotificationRead(notification.id);
        setNotifications((current) =>
          current.map((candidate) =>
            candidate.id === notification.id
              ? { ...candidate, isRead: true }
              : candidate,
          ),
        );
        setUnreadCount((current) => Math.max(0, current - 1));
      } catch {
        setError(
          getApiErrorMessage(
            new Error("Unable to mark that notification as read."),
            "Unable to mark that notification as read.",
          ),
        );
      }
    }

    setIsOpen(false);
  }

  return (
    <div className="relative" ref={containerRef}>
      <button
        aria-expanded={isOpen}
        aria-haspopup="menu"
        aria-label={
          unreadCount > 0
            ? `Notifications, ${unreadCount} unread`
            : "Notifications"
        }
        className="relative flex size-10 shrink-0 items-center justify-center rounded-xl border border-slate-200 text-slate-600 transition hover:border-slate-300 hover:text-slate-900 focus:outline-none focus:ring-4 focus:ring-indigo-500/20 dark:border-slate-700 dark:text-slate-300 dark:hover:text-white"
        onClick={handleOpen}
        type="button"
      >
        <Bell aria-hidden="true" className="size-4" />
        {unreadCount > 0 ? (
          <span className="absolute -right-1 -top-1 inline-flex min-w-5 items-center justify-center rounded-full bg-rose-500 px-1 text-[10px] font-bold leading-5 text-white">
            {unreadCount > 99 ? "99+" : unreadCount}
          </span>
        ) : null}
      </button>

      {isOpen ? (
        <div
          aria-label="Notifications"
          className="absolute right-0 z-40 mt-2 w-80 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-xl sm:w-96 dark:border-slate-800 dark:bg-slate-900"
          role="menu"
        >
          <div className="flex items-center justify-between gap-3 border-b border-slate-200 px-4 py-3 dark:border-slate-800">
            <h2 className="text-sm font-semibold">Notifications</h2>
            {unreadCount > 0 ? (
              <button
                className="inline-flex items-center gap-1.5 rounded-lg px-2 py-1 text-xs font-semibold text-indigo-600 transition hover:bg-indigo-50 focus:outline-none focus:ring-4 focus:ring-indigo-500/20 disabled:cursor-not-allowed disabled:opacity-60 dark:text-indigo-300 dark:hover:bg-indigo-950/50"
                disabled={isMutating}
                onClick={() => void handleMarkAllRead()}
                type="button"
              >
                {isMutating ? (
                  <LoaderCircle aria-hidden="true" className="size-3.5 animate-spin" />
                ) : (
                  <CheckCheck aria-hidden="true" className="size-3.5" />
                )}
                Mark all as read
              </button>
            ) : null}
          </div>

          {error ? (
            <div className="border-b border-slate-200 px-4 py-3 dark:border-slate-800">
              <p
                className="text-xs text-red-600 dark:text-red-400"
                role="alert"
              >
                {error}
              </p>
              <button
                className="mt-1 text-xs font-semibold text-indigo-600 underline dark:text-indigo-300"
                onClick={() => {
                  setError(null);
                  setIsLoading(true);
                  setReloadToken((token) => token + 1);
                }}
                type="button"
              >
                Retry
              </button>
            </div>
          ) : null}

          <div className="max-h-96 overflow-y-auto">
            {isLoading ? (
              <div aria-busy="true" className="space-y-2 p-4">
                {[0, 1, 2].map((item) => (
                  <div
                    className="h-12 animate-pulse rounded-lg bg-slate-100 dark:bg-slate-800"
                    key={item}
                  />
                ))}
              </div>
            ) : notifications.length === 0 ? (
              <p className="px-4 py-10 text-center text-sm text-slate-500 dark:text-slate-400">
                You are all caught up.
              </p>
            ) : (
              <ul>
                {notifications.map((notification) => {
                  const body = (
                    <>
                      <div className="flex items-start gap-2">
                        {!notification.isRead ? (
                          <span
                            aria-label="Unread"
                            className="mt-1.5 size-2 shrink-0 rounded-full bg-indigo-500"
                          />
                        ) : (
                          <span aria-hidden="true" className="mt-1.5 size-2 shrink-0" />
                        )}
                        <div className="min-w-0 flex-1">
                          <p className="break-words text-sm font-medium">
                            {notification.title}
                          </p>
                          <p className="mt-0.5 break-words text-xs text-slate-500 dark:text-slate-400">
                            {notification.message}
                          </p>
                          <p className="mt-1 text-[11px] text-slate-400 dark:text-slate-500">
                            {formatRelative(notification.createdAt)}
                          </p>
                        </div>
                      </div>
                    </>
                  );

                  const className = `block w-full border-b border-slate-100 px-4 py-3 text-left transition last:border-b-0 hover:bg-slate-50 focus:outline-none focus:ring-4 focus:ring-indigo-500/20 dark:border-slate-800 dark:hover:bg-slate-800/60 ${
                    notification.isRead
                      ? "opacity-70"
                      : "bg-indigo-50/40 dark:bg-indigo-950/20"
                  }`;

                  return (
                    <li key={notification.id}>
                      {notification.targetUrl ? (
                        <Link
                          className={className}
                          href={notification.targetUrl}
                          onClick={() => void handleOpenNotification(notification)}
                          role="menuitem"
                        >
                          {body}
                        </Link>
                      ) : (
                        <button
                          className={className}
                          onClick={() => void handleOpenNotification(notification)}
                          role="menuitem"
                          type="button"
                        >
                          {body}
                        </button>
                      )}
                    </li>
                  );
                })}
              </ul>
            )}
          </div>
        </div>
      ) : null}
    </div>
  );
}
