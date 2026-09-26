"use client";

import { Building2, FolderKanban, LoaderCircle, X } from "lucide-react";
import { useCallback, useId, useRef, useState } from "react";
import type { FormEvent, KeyboardEvent } from "react";

import { useWorkspace } from "@/context/WorkspaceContext";
import { getApiErrorMessage } from "@/lib/auth";
import type { Workspace } from "@/lib/workspaces";

interface CreateWorkspaceModalProps {
  onClose: () => void;
  onCreated?: (workspace: Workspace) => void;
}

const FOCUSABLE_SELECTOR =
  'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

export function CreateWorkspaceModal({
  onClose,
  onCreated,
}: CreateWorkspaceModalProps) {
  const { organizations, createNewWorkspace } = useWorkspace();
  const [workspaceName, setWorkspaceName] = useState("");
  const [organizationName, setOrganizationName] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const dialogRef = useRef<HTMLDivElement>(null);
  const titleId = useId();
  const descriptionId = useId();
  const organizationListId = useId();

  const handleClose = useCallback(() => {
    if (isSubmitting) {
      return;
    }
    onClose();
  }, [isSubmitting, onClose]);

  const handleKeyDown = useCallback(
    (event: KeyboardEvent<HTMLDivElement>) => {
      if (event.key === "Escape") {
        event.preventDefault();
        handleClose();
        return;
      }

      if (event.key !== "Tab") {
        return;
      }

      const focusableElements = Array.from(
        dialogRef.current?.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR) ??
          [],
      );

      if (focusableElements.length === 0) {
        return;
      }

      const firstElement = focusableElements[0];
      const lastElement = focusableElements[focusableElements.length - 1];

      if (event.shiftKey && document.activeElement === firstElement) {
        event.preventDefault();
        lastElement.focus();
      } else if (!event.shiftKey && document.activeElement === lastElement) {
        event.preventDefault();
        firstElement.focus();
      }
    },
    [handleClose],
  );

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (isSubmitting) {
      return;
    }

    setError(null);
    setIsSubmitting(true);

    try {
      const workspace = await createNewWorkspace({
        name: workspaceName,
        organizationName,
      });
      onCreated?.(workspace);
      onClose();
    } catch (caughtError) {
      setError(
        getApiErrorMessage(
          caughtError,
          "Unable to create the workspace. Please try again.",
        ),
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-end justify-center bg-slate-950/60 p-4 backdrop-blur-sm sm:items-center"
      onKeyDown={handleKeyDown}
    >
      <button
        aria-label="Close create workspace dialog"
        className="absolute inset-0 cursor-default"
        onClick={handleClose}
        tabIndex={-1}
        type="button"
      />

      <div
        aria-describedby={descriptionId}
        aria-labelledby={titleId}
        aria-modal="true"
        className="relative w-full max-w-lg rounded-3xl border border-slate-200 bg-white p-6 shadow-2xl sm:p-8 dark:border-slate-800 dark:bg-slate-900"
        ref={dialogRef}
        role="dialog"
      >
        <div className="flex items-start justify-between gap-4">
          <div className="min-w-0">
            <h2
              className="text-xl font-semibold tracking-tight text-slate-950 dark:text-white"
              id={titleId}
            >
              Create a workspace
            </h2>
            <p
              className="mt-1 text-sm text-slate-500 dark:text-slate-400"
              id={descriptionId}
            >
              Group your team under one organization. Reusing an existing
              organization name keeps everyone in the same place.
            </p>
          </div>
          <button
            className="flex size-9 shrink-0 items-center justify-center rounded-xl border border-slate-200 text-slate-500 transition hover:border-slate-300 hover:text-slate-700 focus:outline-none focus:ring-4 focus:ring-indigo-500/20 dark:border-slate-700 dark:text-slate-400"
            onClick={handleClose}
            type="button"
            aria-label="Close"
          >
            <X className="size-4" aria-hidden="true" />
          </button>
        </div>

        <form className="mt-6 space-y-5" onSubmit={handleSubmit}>
          <div>
            <label
              className="mb-2 block text-sm font-medium text-slate-700 dark:text-slate-200"
              htmlFor="workspace-name"
            >
              Workspace name
            </label>
            <div className="relative">
              <FolderKanban
                aria-hidden="true"
                className="pointer-events-none absolute left-3.5 top-1/2 size-5 -translate-y-1/2 text-slate-400"
              />
              <input
                autoComplete="off"
                autoFocus
                className="h-12 w-full rounded-xl border border-slate-200 bg-slate-50 pl-11 pr-4 text-sm text-slate-950 outline-none transition placeholder:text-slate-400 focus:border-indigo-500 focus:bg-white focus:ring-4 focus:ring-indigo-500/10 disabled:cursor-not-allowed disabled:opacity-60 dark:border-slate-700 dark:bg-slate-800 dark:text-white"
                disabled={isSubmitting}
                id="workspace-name"
                maxLength={120}
                name="workspaceName"
                onChange={(event) => setWorkspaceName(event.target.value)}
                placeholder="Platform Engineering"
                required
                value={workspaceName}
              />
            </div>
          </div>

          <div>
            <label
              className="mb-2 block text-sm font-medium text-slate-700 dark:text-slate-200"
              htmlFor="organization-name"
            >
              Organization name
            </label>
            <div className="relative">
              <Building2
                aria-hidden="true"
                className="pointer-events-none absolute left-3.5 top-1/2 size-5 -translate-y-1/2 text-slate-400"
              />
              <input
                autoComplete="off"
                className="h-12 w-full rounded-xl border border-slate-200 bg-slate-50 pl-11 pr-4 text-sm text-slate-950 outline-none transition placeholder:text-slate-400 focus:border-indigo-500 focus:bg-white focus:ring-4 focus:ring-indigo-500/10 disabled:cursor-not-allowed disabled:opacity-60 dark:border-slate-700 dark:bg-slate-800 dark:text-white"
                disabled={isSubmitting}
                id="organization-name"
                list={organizationListId}
                maxLength={120}
                name="organizationName"
                onChange={(event) => setOrganizationName(event.target.value)}
                placeholder="Engineering Team"
                required
                value={organizationName}
              />
            </div>
            <datalist id={organizationListId}>
              {organizations.map((organization) => (
                <option key={organization.id} value={organization.name} />
              ))}
            </datalist>
            {organizations.length > 0 ? (
              <p className="mt-2 text-xs text-slate-500">
                Reuse one of your organizations to share the workspace with the
                whole team.
              </p>
            ) : null}
          </div>

          {error ? (
            <div
              className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700"
              role="alert"
            >
              {error}
            </div>
          ) : null}

          <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
            <button
              className="h-11 rounded-xl border border-slate-200 px-5 text-sm font-semibold text-slate-700 transition hover:bg-slate-100 focus:outline-none focus:ring-4 focus:ring-indigo-500/20 disabled:cursor-not-allowed disabled:opacity-60 dark:border-slate-700 dark:text-slate-200 dark:hover:bg-slate-800"
              disabled={isSubmitting}
              onClick={handleClose}
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
                "Create workspace"
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
