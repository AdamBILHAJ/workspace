"use client";

import { Plus, UsersRound } from "lucide-react";
import Link from "next/link";

import type { WorkspaceMember, WorkspaceRole } from "@/lib/workspaces";

interface MembersRosterProps {
  members: WorkspaceMember[];
  canManage: boolean;
  onOpenAddModal: () => void;
  isLoading?: boolean;
}

const ROLE_STYLES: Record<WorkspaceRole, string> = {
  OWNER: "bg-indigo-100 text-indigo-700 dark:bg-indigo-950/60 dark:text-indigo-300",
  ADMIN: "bg-emerald-100 text-emerald-700 dark:bg-emerald-950/60 dark:text-emerald-300",
  MEMBER: "bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300",
};

function displayName(member: WorkspaceMember): string {
  const fullName = `${member.user.firstName} ${member.user.lastName}`.trim();
  return fullName.length > 0 ? fullName : member.user.email;
}

function initials(member: WorkspaceMember): string {
  const first = member.user.firstName.charAt(0);
  const last = member.user.lastName.charAt(0);
  const combined = `${first}${last}`.trim();
  return combined.length > 0 ? combined : member.user.email.charAt(0);
}

export function MembersRoster({
  members,
  canManage,
  onOpenAddModal,
  isLoading = false,
}: MembersRosterProps) {
  return (
    <section aria-label="Workspace members" className="mt-8">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="font-semibold">Workspace Members</h2>
          <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
            {isLoading
              ? "Loading members…"
              : `${members.length} ${members.length === 1 ? "member" : "members"} in this workspace`}
          </p>
        </div>
        {canManage ? (
          <button
            className="inline-flex h-10 items-center justify-center gap-2 rounded-xl bg-slate-950 px-4 text-sm font-semibold text-white transition hover:bg-indigo-700 focus:outline-none focus:ring-4 focus:ring-indigo-500/20 dark:bg-white dark:text-slate-950 dark:hover:bg-indigo-600"
            onClick={onOpenAddModal}
            type="button"
          >
            <Plus aria-hidden="true" className="size-4" />
            Add Member
          </button>
        ) : null}
      </div>

      <div className="mt-4 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900">
        {isLoading ? (
          <ul
            aria-busy="true"
            aria-label="Loading members"
            className="divide-y divide-slate-100 dark:divide-slate-800"
          >
            {[0, 1, 2].map((index) => (
              <li className="flex items-center gap-3 px-5 py-4" key={index}>
                <div className="size-10 shrink-0 animate-pulse rounded-full bg-slate-100 dark:bg-slate-800" />
                <div className="min-w-0 flex-1 space-y-2">
                  <div className="h-4 w-40 animate-pulse rounded-lg bg-slate-100 dark:bg-slate-800" />
                  <div className="h-3 w-56 animate-pulse rounded-lg bg-slate-100 dark:bg-slate-800" />
                </div>
                <div className="h-6 w-16 shrink-0 animate-pulse rounded-full bg-slate-100 dark:bg-slate-800" />
              </li>
            ))}
          </ul>
        ) : members.length === 0 ? (
          <div className="px-5 py-12 text-center">
            <UsersRound
              aria-hidden="true"
              className="mx-auto size-8 text-slate-300 dark:text-slate-600"
            />
            <p className="mt-3 text-sm text-slate-500 dark:text-slate-400">
              No members to show yet.
            </p>
          </div>
        ) : (
          <ul className="divide-y divide-slate-100 dark:divide-slate-800">
            {members.map((member) => (
              <li className="flex items-center gap-3 px-5 py-4" key={member.id}>
                <div className="flex size-10 shrink-0 items-center justify-center rounded-full bg-indigo-100 text-sm font-semibold uppercase text-indigo-700 dark:bg-indigo-950/60 dark:text-indigo-300">
                  {initials(member)}
                </div>
                <div className="min-w-0 flex-1">
                  <p className="break-words text-sm font-medium">
                    {displayName(member)}
                  </p>
                  <Link
                    className="break-all text-xs text-slate-500 underline-offset-2 hover:underline dark:text-slate-400"
                    href={`mailto:${member.user.email}`}
                  >
                    {member.user.email}
                  </Link>
                </div>
                <span
                  className={`shrink-0 rounded-full px-3 py-1 text-xs font-semibold ${ROLE_STYLES[member.role]}`}
                >
                  {member.role}
                </span>
              </li>
            ))}
          </ul>
        )}
      </div>
    </section>
  );
}
