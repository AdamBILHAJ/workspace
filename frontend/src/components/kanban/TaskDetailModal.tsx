"use client";

import {
  Activity,
  History,
  LoaderCircle,
  MessageSquare,
  Send,
} from "lucide-react";
import { useEffect, useState } from "react";
import type { KeyboardEvent } from "react";

import { PriorityBadge } from "@/components/kanban/PriorityBadge";
import { UserAvatar } from "@/components/kanban/UserAvatar";
import { Modal } from "@/components/ui/Modal";
import {
  addComment,
  getTaskActivity,
  getTaskComments,
  type TaskActivity,
  type TaskComment,
} from "@/lib/api";
import { getApiErrorMessage } from "@/lib/auth";
import type { Task } from "@/lib/projects";

type Tab = "comments" | "activity";

interface TaskDetailModalProps {
  onClose: () => void;
  task: Task;
}

const ACTION_LABELS: Record<TaskActivity["action"], string> = {
  TASK_CREATED: "created this task",
  TASK_MOVED: "moved this task",
  STATUS_CHANGED: "changed the status",
  COMMENT_ADDED: "added a comment",
};

function formatTimestamp(value: string): string {
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }
  return parsed.toLocaleString(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  });
}

export function TaskDetailModal({ onClose, task }: TaskDetailModalProps) {
  const [tab, setTab] = useState<Tab>("comments");
  const [comments, setComments] = useState<TaskComment[]>([]);
  const [activity, setActivity] = useState<TaskActivity[]>([]);
  const [draft, setDraft] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [reloadToken, setReloadToken] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    let isActive = true;

    async function load(): Promise<void> {
      try {
        const [loadedComments, loadedActivity] = await Promise.all([
          getTaskComments(task.id, controller.signal),
          getTaskActivity(task.id, controller.signal),
        ]);

        if (isActive) {
          setComments(loadedComments);
          setActivity(loadedActivity);
        }
      } catch (caughtError) {
        if (isActive && !controller.signal.aborted) {
          setLoadError(
            getApiErrorMessage(caughtError, "Unable to load this task."),
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
  }, [reloadToken, task.id]);

  async function handleSubmit() {
    const content = draft.trim();

    if (content.length === 0 || isSubmitting) {
      return;
    }

    setSubmitError(null);
    setIsSubmitting(true);

    try {
      const created = await addComment(task.id, content);
      setComments((current) => [...current, created]);
      setDraft("");
    } catch (caughtError) {
      setSubmitError(
        getApiErrorMessage(caughtError, "Unable to post your comment."),
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  function handleKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === "Enter" && (event.ctrlKey || event.metaKey)) {
      event.preventDefault();
      void handleSubmit();
    }
  }

  return (
    <Modal description={task.description ?? undefined} onClose={onClose} title={task.title}>
      <div className="mt-4 flex flex-wrap items-center gap-2">
        <PriorityBadge priority={task.priority} />
        <UserAvatar size="md" user={task.assignee} />
        <span className="text-xs text-slate-500 dark:text-slate-400">
          {task.assignee ? `Assigned to ${task.assignee.firstName} ${task.assignee.lastName}` : "Unassigned"}
        </span>
      </div>

      <div
        aria-label="Task details tabs"
        className="mt-5 flex gap-1 border-b border-slate-200 dark:border-slate-800"
        role="tablist"
      >
        {(
          [
            { id: "comments" as const, label: "Comments", icon: MessageSquare },
            { id: "activity" as const, label: "Activity", icon: History },
          ] satisfies { id: Tab; label: string; icon: typeof MessageSquare }[]
        ).map(({ id, label, icon: Icon }) => (
          <button
            aria-selected={tab === id}
            className={`-mb-px flex items-center gap-2 border-b-2 px-4 py-2.5 text-sm font-medium transition focus:outline-none focus:ring-4 focus:ring-indigo-500/20 ${
              tab === id
                ? "border-indigo-500 text-indigo-700 dark:text-indigo-300"
                : "border-transparent text-slate-500 hover:text-slate-800 dark:hover:text-slate-200"
            }`}
            id={`task-tab-${id}`}
            key={id}
            onClick={() => setTab(id)}
            role="tab"
            type="button"
          >
            <Icon aria-hidden="true" className="size-4" />
            {label}
            {id === "comments" && comments.length > 0 ? (
              <span className="rounded-full bg-slate-100 px-1.5 text-xs font-semibold text-slate-600 dark:bg-slate-800 dark:text-slate-300">
                {comments.length}
              </span>
            ) : null}
          </button>
        ))}
      </div>

      <div
        aria-labelledby={`task-tab-${tab}`}
        className="mt-4 max-h-80 min-h-40 overflow-y-auto"
        role="tabpanel"
      >
        {isLoading ? (
          <div
            aria-busy="true"
            aria-label="Loading task details"
            className="space-y-3 py-2"
          >
            {[0, 1, 2].map((item) => (
              <div
                className="h-14 animate-pulse rounded-xl bg-slate-100 dark:bg-slate-800"
                key={item}
              />
            ))}
          </div>
        ) : loadError ? (
          <div className="py-4 text-center">
            <div
              className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-900/60 dark:bg-red-950/40 dark:text-red-300"
              role="alert"
            >
              {loadError}
            </div>
            <button
              className="mt-3 inline-flex h-9 items-center gap-2 rounded-xl border border-slate-200 px-4 text-sm font-semibold text-slate-700 transition hover:bg-slate-100 focus:outline-none focus:ring-4 focus:ring-indigo-500/20 dark:border-slate-700 dark:text-slate-200 dark:hover:bg-slate-800"
              onClick={() => {
                setIsLoading(true);
                setLoadError(null);
                setReloadToken((token) => token + 1);
              }}
              type="button"
            >
              <LoaderCircle aria-hidden="true" className="size-4" />
              Try again
            </button>
          </div>
        ) : tab === "comments" ? (
          comments.length === 0 ? (
            <p className="py-8 text-center text-sm text-slate-500 dark:text-slate-400">
              No comments yet. Start the conversation below.
            </p>
          ) : (
            <ul className="space-y-3">
              {comments.map((comment) => (
                <li
                  className="flex gap-3 rounded-xl border border-slate-200 p-3 dark:border-slate-800"
                  key={comment.id}
                >
                  <span className="inline-flex size-8 shrink-0 items-center justify-center rounded-full bg-indigo-100 text-xs font-semibold text-indigo-700 dark:bg-indigo-950/60 dark:text-indigo-300">
                    {comment.authorName.charAt(0).toUpperCase()}
                  </span>
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-baseline gap-x-2">
                      <p className="text-sm font-medium">{comment.authorName}</p>
                      <time
                        className="text-xs text-slate-500 dark:text-slate-400"
                        dateTime={comment.createdAt}
                      >
                        {formatTimestamp(comment.createdAt)}
                      </time>
                    </div>
                    <p className="mt-1 whitespace-pre-wrap break-words text-sm text-slate-700 dark:text-slate-300">
                      {comment.content}
                    </p>
                  </div>
                </li>
              ))}
            </ul>
          )
        ) : activity.length === 0 ? (
          <p className="py-8 text-center text-sm text-slate-500 dark:text-slate-400">
            No activity recorded yet.
          </p>
        ) : (
          <ol className="relative space-y-4 border-l border-slate-200 pl-5 dark:border-slate-800">
            {activity.map((entry) => (
              <li className="relative" key={entry.id}>
                <span className="absolute -left-[27px] flex size-4 items-center justify-center rounded-full border-2 border-white bg-indigo-500 dark:border-slate-900">
                  <Activity aria-hidden="true" className="size-2 text-white" />
                </span>
                <p className="text-sm text-slate-700 dark:text-slate-300">
                  <span className="font-medium">{entry.actorName}</span>{" "}
                  {entry.details ?? ACTION_LABELS[entry.action]}
                </p>
                <time
                  className="text-xs text-slate-500 dark:text-slate-400"
                  dateTime={entry.createdAt}
                >
                  {formatTimestamp(entry.createdAt)}
                </time>
              </li>
            ))}
          </ol>
        )}
      </div>

      <form
        className="mt-5 border-t border-slate-200 pt-4 dark:border-slate-800"
        onSubmit={(event) => {
          event.preventDefault();
          void handleSubmit();
        }}
      >
        <label className="sr-only" htmlFor="new-comment">
          Add a comment
        </label>
        <textarea
          className="min-h-20 w-full rounded-xl border border-slate-200 bg-slate-50 px-3.5 py-2.5 text-sm text-slate-950 outline-none transition placeholder:text-slate-400 focus:border-indigo-500 focus:bg-white focus:ring-4 focus:ring-indigo-500/10 disabled:cursor-not-allowed disabled:opacity-60 dark:border-slate-700 dark:bg-slate-800 dark:text-white"
          disabled={isSubmitting || isLoading}
          id="new-comment"
          maxLength={4000}
          onChange={(event) => setDraft(event.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="Leave a comment. Press Ctrl+Enter to send."
          value={draft}
        />

        {submitError ? (
          <div
            className="mt-3 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-900/60 dark:bg-red-950/40 dark:text-red-300"
            role="alert"
          >
            {submitError}
          </div>
        ) : null}

        <div className="mt-3 flex items-center justify-between gap-3">
          <p className="hidden text-xs text-slate-500 sm:block">
            Ctrl+Enter to send
          </p>
          <button
            aria-busy={isSubmitting}
            className="ml-auto flex h-10 items-center justify-center gap-2 rounded-xl bg-slate-950 px-4 text-sm font-semibold text-white transition hover:bg-indigo-700 focus:outline-none focus:ring-4 focus:ring-indigo-500/20 disabled:cursor-not-allowed disabled:opacity-60 dark:bg-white dark:text-slate-950 dark:hover:bg-indigo-600"
            disabled={isSubmitting || draft.trim().length === 0}
            type="submit"
          >
            {isSubmitting ? (
              <LoaderCircle aria-hidden="true" className="size-4 animate-spin" />
            ) : (
              <Send aria-hidden="true" className="size-4" />
            )}
            Comment
          </button>
        </div>
      </form>
    </Modal>
  );
}
