"use client";

import { CheckCircle2, Clock3, FolderKanban, UsersRound } from "lucide-react";

import { WorkspaceShellHeader } from "@/components/workspace/WorkspaceShellHeader";
import { useAuth } from "@/context/AuthContext";

export default function DashboardPage() {
  const { user } = useAuth();

  return (
    <main className="min-h-screen bg-slate-50 text-slate-950 dark:bg-slate-950 dark:text-white">
      <WorkspaceShellHeader />

      <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8 lg:py-10">
        <div className="mb-8">
          <div>
            <p className="text-sm font-medium text-indigo-600">Overview</p>
            <h1 className="mt-1 break-words text-3xl font-semibold tracking-tight sm:text-4xl">
              Welcome back, {user?.firstName}
            </h1>
            <p className="mt-2 text-slate-500 dark:text-slate-400">
              Your team workspace is ready for the next task.
            </p>
          </div>
        </div>

        <section className="grid gap-4 sm:grid-cols-3" aria-label="Workspace summary">
          {[
            { label: "Active projects", value: "8", icon: FolderKanban },
            { label: "Team members", value: "24", icon: UsersRound },
            { label: "Tasks completed", value: "96%", icon: CheckCircle2 },
          ].map(({ label, value, icon: Icon }) => (
            <article
              className="min-w-0 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm dark:border-slate-800 dark:bg-slate-900"
              key={label}
            >
              <div className="flex items-center justify-between">
                <p className="text-sm font-medium text-slate-500">{label}</p>
                <Icon className="size-5 text-indigo-500" aria-hidden="true" />
              </div>
              <p className="mt-3 text-3xl font-semibold tracking-tight">{value}</p>
            </article>
          ))}
        </section>

        <section className="mt-6 grid gap-6 lg:grid-cols-3">
          <div className="min-w-0 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900 lg:col-span-2">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="font-semibold">Recent activity</h2>
                <p className="mt-1 text-sm text-slate-500">
                  Latest changes across your workspace
                </p>
              </div>
              <Clock3 className="size-5 text-slate-400" aria-hidden="true" />
            </div>
            <div className="mt-8 grid gap-4 sm:grid-cols-3">
              {["Project roadmap", "Design review", "Sprint planning"].map(
                (item, index) => (
                  <div
                    className="rounded-xl bg-slate-50 p-4 dark:bg-slate-800/70"
                    key={item}
                  >
                    <div className="mb-6 h-2 w-12 rounded-full bg-indigo-200 dark:bg-indigo-900" />
                    <p className="text-sm font-medium">{item}</p>
                    <p className="mt-1 text-xs text-slate-500">
                      Updated {index + 1}h ago
                    </p>
                  </div>
                ),
              )}
            </div>
          </div>

          <div className="min-w-0 rounded-2xl bg-slate-950 p-6 text-white shadow-xl shadow-slate-950/10">
            <p className="text-sm font-medium text-indigo-300">Account</p>
            <h2 className="mt-2 break-all text-xl font-semibold">{user?.email}</h2>
            <p className="mt-1 text-sm text-slate-400">{user?.role}</p>
            <div className="mt-8 rounded-xl border border-white/10 bg-white/5 p-4">
              <p className="text-sm font-medium">JWT session active</p>
              <p className="mt-1 text-xs leading-5 text-slate-400">
                Your access token is attached to authenticated API requests.
              </p>
            </div>
          </div>
        </section>
      </div>
    </main>
  );
}
