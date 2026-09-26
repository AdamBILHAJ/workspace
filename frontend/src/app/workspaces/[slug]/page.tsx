"use client";

import { ArrowLeft, FolderKanban, LoaderCircle, UsersRound } from "lucide-react";
import Link from "next/link";
import { useParams } from "next/navigation";

import { WorkspaceShellHeader } from "@/components/workspace/WorkspaceShellHeader";
import { useWorkspace } from "@/context/WorkspaceContext";
import type { WorkspaceRole } from "@/lib/workspaces";

const ROLE_STYLES: Record<WorkspaceRole, string> = {
  OWNER: "bg-indigo-100 text-indigo-700 dark:bg-indigo-950/60 dark:text-indigo-300",
  ADMIN: "bg-emerald-100 text-emerald-700 dark:bg-emerald-950/60 dark:text-emerald-300",
  MEMBER: "bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300",
};

export default function WorkspacePage() {
  const params = useParams<{ slug: string }>();
  const slug = Array.isArray(params?.slug) ? params.slug[0] : params?.slug;
  const { workspaces, isLoading, error, refresh } = useWorkspace();
  const workspace = workspaces.find((candidate) => candidate.slug === slug);

  return (
    <main className="min-h-screen bg-slate-50 text-slate-950 dark:bg-slate-950 dark:text-white">
      <WorkspaceShellHeader
        activeSlug={slug}
        leading={
          <Link
            aria-label="Back to dashboard"
            className="flex size-10 shrink-0 items-center justify-center rounded-xl border border-slate-200 text-slate-600 transition hover:border-slate-300 hover:text-slate-900 focus:outline-none focus:ring-4 focus:ring-indigo-500/20 dark:border-slate-700 dark:text-slate-300 dark:hover:text-white"
            href="/dashboard"
          >
            <ArrowLeft className="size-4" aria-hidden="true" />
          </Link>
        }
        subtitle={workspace?.organization.name ?? "Workspace"}
        title={workspace?.name ?? "Workspace"}
      />

      <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8 lg:py-10">
        {isLoading ? (
          <div
            aria-busy="true"
            aria-label="Loading workspace"
            className="animate-pulse space-y-6"
          >
            <div className="h-10 w-72 rounded-xl bg-slate-200 dark:bg-slate-800" />
            <div className="grid gap-4 sm:grid-cols-3">
              {[0, 1, 2].map((item) => (
                <div
                  className="h-28 rounded-2xl bg-slate-200 dark:bg-slate-800"
                  key={item}
                />
              ))}
            </div>
            <div className="h-64 rounded-2xl bg-slate-200 dark:bg-slate-800" />
          </div>
        ) : error ? (
          <section className="mx-auto max-w-lg rounded-2xl border border-red-200 bg-white p-6 text-center shadow-sm dark:border-red-900/60 dark:bg-slate-900">
            <h1 className="text-lg font-semibold">Workspaces unavailable</h1>
            <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">
              {error}
            </p>
            <button
              className="mt-6 inline-flex h-11 items-center justify-center gap-2 rounded-xl bg-slate-950 px-5 text-sm font-semibold text-white transition hover:bg-indigo-700 focus:outline-none focus:ring-4 focus:ring-indigo-500/20 dark:bg-white dark:text-slate-950"
              onClick={() => void refresh()}
              type="button"
            >
              <LoaderCircle aria-hidden="true" className="size-4" />
              Try again
            </button>
          </section>
        ) : !workspace ? (
          <section className="mx-auto max-w-lg rounded-2xl border border-slate-200 bg-white p-6 text-center shadow-sm dark:border-slate-800 dark:bg-slate-900">
            <h1 className="text-lg font-semibold">Workspace not found</h1>
            <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">
              You may not be a member of this workspace, or it no longer exists.
            </p>
            <Link
              className="mt-6 inline-flex h-11 items-center justify-center rounded-xl bg-slate-950 px-5 text-sm font-semibold text-white transition hover:bg-indigo-700 focus:outline-none focus:ring-4 focus:ring-indigo-500/20 dark:bg-white dark:text-slate-950"
              href="/dashboard"
            >
              Back to dashboard
            </Link>
          </section>
        ) : (
          <>
            <div className="flex flex-wrap items-center gap-3">
              <h1 className="break-words text-3xl font-semibold tracking-tight sm:text-4xl">
                {workspace.name}
              </h1>
              <span
                className={`rounded-full px-3 py-1 text-xs font-semibold ${ROLE_STYLES[workspace.role]}`}
              >
                {workspace.role}
              </span>
            </div>
            <p className="mt-2 break-all text-sm text-slate-500 dark:text-slate-400">
              {workspace.organization.name} · /{workspace.slug}
            </p>

            <section
              aria-label="Workspace summary"
              className="mt-8 grid gap-4 sm:grid-cols-3"
            >
              {[
                { label: "Your role", value: workspace.role, icon: UsersRound },
                {
                  label: "Organization",
                  value: workspace.organization.name,
                  icon: FolderKanban,
                },
                { label: "Workspace slug", value: workspace.slug, icon: FolderKanban },
              ].map(({ label, value, icon: Icon }) => (
                <article
                  className="min-w-0 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm dark:border-slate-800 dark:bg-slate-900"
                  key={label}
                >
                  <div className="flex items-center justify-between gap-3">
                    <p className="text-sm font-medium text-slate-500">{label}</p>
                    <Icon aria-hidden="true" className="size-5 shrink-0 text-indigo-500" />
                  </div>
                  <p className="mt-3 break-words text-lg font-semibold tracking-tight">
                    {value}
                  </p>
                </article>
              ))}
            </section>

            <section className="mt-6 min-w-0 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
              <h2 className="font-semibold">Getting started</h2>
              <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
                Projects, tasks, and members for {workspace.name} will appear
                here. Use the switcher in the header to jump between the
                workspaces you belong to.
              </p>
            </section>
          </>
        )}
      </div>
    </main>
  );
}
