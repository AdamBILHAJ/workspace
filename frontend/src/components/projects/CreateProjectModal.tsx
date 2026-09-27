"use client";

import { LoaderCircle } from "lucide-react";
import { useState } from "react";
import type { FormEvent } from "react";

import { Modal } from "@/components/ui/Modal";
import { getApiErrorMessage } from "@/lib/auth";

interface CreateProjectModalProps {
  onClose: () => void;
  onCreate: (input: {
    name: string;
    description: string;
    key: string;
  }) => Promise<void>;
}

const FIELD_CLASS =
  "h-11 w-full rounded-xl border border-slate-200 bg-slate-50 px-3.5 text-sm text-slate-950 outline-none transition placeholder:text-slate-400 focus:border-indigo-500 focus:bg-white focus:ring-4 focus:ring-indigo-500/10 disabled:cursor-not-allowed disabled:opacity-60 dark:border-slate-700 dark:bg-slate-800 dark:text-white";

export function CreateProjectModal({
  onClose,
  onCreate,
}: CreateProjectModalProps) {
  const [name, setName] = useState("");
  const [key, setKey] = useState("");
  const [description, setDescription] = useState("");
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
        name: name.trim(),
        description: description.trim(),
        key: key.trim().toUpperCase(),
      });
      onClose();
    } catch (caughtError) {
      setError(
        getApiErrorMessage(caughtError, "Unable to create the project."),
      );
      setIsSubmitting(false);
    }
  }

  return (
    <Modal
      description="A To Do, In Progress, and Done column is created for you."
      isDismissible={!isSubmitting}
      onClose={onClose}
      title="Create a project"
    >
      <form className="mt-6 space-y-4" onSubmit={handleSubmit}>
        <div>
          <label
            className="mb-2 block text-sm font-medium text-slate-700 dark:text-slate-200"
            htmlFor="project-name"
          >
            Project name
          </label>
          <input
            autoComplete="off"
            autoFocus
            className={FIELD_CLASS}
            disabled={isSubmitting}
            id="project-name"
            maxLength={120}
            onChange={(event) => setName(event.target.value)}
            placeholder="Mobile App"
            required
            value={name}
          />
        </div>

        <div>
          <label
            className="mb-2 block text-sm font-medium text-slate-700 dark:text-slate-200"
            htmlFor="project-key"
          >
            Project key
          </label>
          <input
            autoComplete="off"
            className={FIELD_CLASS}
            disabled={isSubmitting}
            id="project-key"
            maxLength={10}
            onChange={(event) => setKey(event.target.value.toUpperCase())}
            placeholder="MOBILE"
          />
          <p className="mt-2 text-xs text-slate-500 dark:text-slate-400">
            Optional. 2 to 10 letters or digits. We derive one from the name
            when you leave it empty, and the key must be unique in this
            workspace.
          </p>
        </div>

        <div>
          <label
            className="mb-2 block text-sm font-medium text-slate-700 dark:text-slate-200"
            htmlFor="project-description"
          >
            Description
          </label>
          <textarea
            className="min-h-24 w-full rounded-xl border border-slate-200 bg-slate-50 px-3.5 py-2.5 text-sm text-slate-950 outline-none transition placeholder:text-slate-400 focus:border-indigo-500 focus:bg-white focus:ring-4 focus:ring-indigo-500/10 disabled:cursor-not-allowed disabled:opacity-60 dark:border-slate-700 dark:bg-slate-800 dark:text-white"
            disabled={isSubmitting}
            id="project-description"
            maxLength={2000}
            onChange={(event) => setDescription(event.target.value)}
            placeholder="What is this project trying to achieve?"
            value={description}
          />
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
              "Create project"
            )}
          </button>
        </div>
      </form>
    </Modal>
  );
}
