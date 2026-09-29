"use client";

import { LoaderCircle } from "lucide-react";
import { useState } from "react";
import type { FormEvent } from "react";

import { Modal } from "@/components/ui/Modal";
import { getApiErrorMessage } from "@/lib/auth";
import { addWorkspaceMember, type WorkspaceMember } from "@/lib/workspaces";

interface AddMemberModalProps {
  isOpen: boolean;
  onClose: () => void;
  workspaceId: number;
  onMemberAdded: (newMember: WorkspaceMember) => void;
}

const FIELD_CLASS =
  "h-11 w-full rounded-xl border border-slate-200 bg-slate-50 px-3.5 text-sm text-slate-950 outline-none transition placeholder:text-slate-400 focus:border-indigo-500 focus:bg-white focus:ring-4 focus:ring-indigo-500/10 disabled:cursor-not-allowed disabled:opacity-60 dark:border-slate-700 dark:bg-slate-800 dark:text-white";

/**
 * Adds a registered user to the workspace by email. The caller gates this
 * behind OWNER/ADMIN, matching WorkspaceService.addMember, but the server is
 * the authority: a 403 here is surfaced rather than hidden.
 *
 * <p>Render this only while open. Unmounting is what resets the form, so
 * re-opening never shows a stale error or a half-typed address.
 */
export function AddMemberModal({
  isOpen,
  onClose,
  workspaceId,
  onMemberAdded,
}: AddMemberModalProps) {
  const [email, setEmail] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (!isOpen) {
    return null;
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (isSubmitting) {
      return;
    }

    setError(null);
    setIsSubmitting(true);

    try {
      const member = await addWorkspaceMember(workspaceId, { email });
      onMemberAdded(member);
      onClose();
    } catch (caughtError) {
      setError(getApiErrorMessage(caughtError, "Failed to add member"));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <Modal
      description="Enter the email address of an existing account. They join with the Member role."
      isDismissible={!isSubmitting}
      onClose={onClose}
      title="Add a member"
    >
      <form className="mt-6 space-y-4" onSubmit={handleSubmit}>
        <div>
          <label
            className="mb-2 block text-sm font-medium text-slate-700 dark:text-slate-200"
            htmlFor="member-email"
          >
            Email address
          </label>
          <input
            autoComplete="off"
            autoFocus
            className={FIELD_CLASS}
            disabled={isSubmitting}
            id="member-email"
            maxLength={320}
            name="email"
            onChange={(event) => setEmail(event.target.value)}
            placeholder="teammate@example.com"
            required
            type="email"
            value={email}
          />
          <p className="mt-2 text-xs text-slate-500 dark:text-slate-400">
            The account must already be registered. Email matching is not
            case-sensitive.
          </p>
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
                Adding…
              </>
            ) : (
              "Add member"
            )}
          </button>
        </div>
      </form>
    </Modal>
  );
}
