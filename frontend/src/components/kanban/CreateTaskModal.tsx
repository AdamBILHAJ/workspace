"use client";

import { LoaderCircle } from "lucide-react";
import { useState } from "react";
import type { FormEvent } from "react";

import { Modal } from "@/components/ui/Modal";
import { getApiErrorMessage } from "@/lib/auth";
import { TASK_PRIORITIES, type TaskPriority } from "@/lib/projects";
import { TASK_PRIORITY_LABELS } from "@/components/kanban/PriorityBadge";
import type { WorkspaceMember } from "@/lib/workspaces";

interface CreateTaskModalProps {
  columnName: string;
  members: WorkspaceMember[];
  onClose: () => void;
  onCreate: (input: {
    title: string;
    description: string;
    priority: TaskPriority;
    assigneeId: number | null;
  }) => Promise<void>;
}

const FIELD_CLASS =
  "h-11 w-full rounded-xl border border-slate-200 bg-slate-50 px-3.5 text-sm text-slate-950 outline-none transition placeholder:text-slate-400 focus:border-indigo-500 focus:bg-white focus:ring-4 focus:ring-indigo-500/10 disabled:cursor-not-allowed disabled:opacity-60 dark:border-slate-700 dark:bg-slate-800 dark:text-white";

export function CreateTaskModal({
  columnName,
  members,
  onClose,
  onCreate,
}: CreateTaskModalProps) {
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [priority, setPriority] = useState<TaskPriority>("MEDIUM");
  const [assigneeId, setAssigneeId] = useState<string>("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (isSubmitting) {
      return;
    }

    setError(null);
    setIsSubmitting(true);

    try {
      await onCreate({
        title: title.trim(),
        description: description.trim(),
        priority,
        assigneeId: assigneeId ? Number(assigneeId) : null,
      });
      onClose();
    } catch (caughtError) {
      setError(
        getApiErrorMessage(caughtError, "Unable to create the task."),
      );
      setIsSubmitting(false);
    }
  }

  return (
    <Modal
      description={`The task will be added to the ${columnName} column.`}
      isDismissible={!isSubmitting}
      onClose={onClose}
      title="Create a task"
    >
      <form className="mt-6 space-y-4" onSubmit={handleSubmit}>
        <div>
          <label
            className="mb-2 block text-sm font-medium text-slate-700 dark:text-slate-200"
            htmlFor="task-title"
          >
            Title
          </label>
          <input
            autoComplete="off"
            autoFocus
            className={FIELD_CLASS}
            disabled={isSubmitting}
            id="task-title"
            maxLength={200}
            onChange={(event) => setTitle(event.target.value)}
            placeholder="Draft the release notes"
            required
            value={title}
          />
        </div>

        <div>
          <label
            className="mb-2 block text-sm font-medium text-slate-700 dark:text-slate-200"
            htmlFor="task-description"
          >
            Description
          </label>
          <textarea
            className="min-h-24 w-full rounded-xl border border-slate-200 bg-slate-50 px-3.5 py-2.5 text-sm text-slate-950 outline-none transition placeholder:text-slate-400 focus:border-indigo-500 focus:bg-white focus:ring-4 focus:ring-indigo-500/10 disabled:cursor-not-allowed disabled:opacity-60 dark:border-slate-700 dark:bg-slate-800 dark:text-white"
            disabled={isSubmitting}
            id="task-description"
            maxLength={4000}
            onChange={(event) => setDescription(event.target.value)}
            placeholder="Add context, acceptance criteria, or links"
            value={description}
          />
        </div>

        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label
              className="mb-2 block text-sm font-medium text-slate-700 dark:text-slate-200"
              htmlFor="task-priority"
            >
              Priority
            </label>
            <select
              className={FIELD_CLASS}
              disabled={isSubmitting}
              id="task-priority"
              onChange={(event) =>
                setPriority(event.target.value as TaskPriority)
              }
              value={priority}
            >
              {TASK_PRIORITIES.map((value) => (
                <option key={value} value={value}>
                  {TASK_PRIORITY_LABELS[value]}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label
              className="mb-2 block text-sm font-medium text-slate-700 dark:text-slate-200"
              htmlFor="task-assignee"
            >
              Assignee
            </label>
            <select
              className={FIELD_CLASS}
              disabled={isSubmitting}
              id="task-assignee"
              onChange={(event) => setAssigneeId(event.target.value)}
              value={assigneeId}
            >
              <option value="">Unassigned</option>
              {members.map((member) => (
                <option key={member.id} value={member.user.id}>
                  {`${member.user.firstName} ${member.user.lastName}`.trim() ||
                    member.user.email}
                </option>
              ))}
            </select>
          </div>
        </div>

        {error ? (
          <div
            className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-900/60 dark:bg-red-950/40 dark:text-red-300"
            role="alert"
          >
            {error}
          </div>
        ) : null}

        <div className="flex flex-col-reverse gap-3 pt-2 sm:flex-row sm:justify-end">
          <button
            className="h-11 rounded-xl border border-slate-200 px-5 text-sm font-semibold text-slate-700 transition hover:bg-slate-100 focus:outline-none focus:ring-4 focus:ring-indigo-500/20 disabled:cursor-not-allowed disabled:opacity-60 dark:border-slate-700 dark:text-slate-200 dark:hover:bg-slate-800"
            disabled={isSubmitting}
            onClick={onClose}
            type="button"
          >
            Cancel
          </button>
          <button
            aria-busy={isSubmitting}
            className="flex h-11 items-center justify-center gap-2 rounded-xl bg-slate-950 px-5 text-sm font-semibold text-white transition hover:bg-indigo-700 focus:outline-none focus:ring-4 focus:ring-indigo-500/20 disabled:cursor-not-allowed disabled:opacity-60 dark:bg-white dark:text-slate-950 dark:hover:bg-indigo-600"
            disabled={isSubmitting}
            type="submit"
          >
            {isSubmitting ? (
              <>
                <LoaderCircle
                  aria-hidden="true"
                  className="size-4 animate-spin"
                />
                Creating…
              </>
            ) : (
              "Create task"
            )}
          </button>
        </div>
      </form>
    </Modal>
  );
}
