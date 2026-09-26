"use client";

import { Layers3, LogOut } from "lucide-react";
import { useRouter } from "next/navigation";
import type { ReactNode } from "react";

import { WorkspaceSwitcher } from "@/components/workspace/WorkspaceSwitcher";
import { useAuth } from "@/context/AuthContext";

interface WorkspaceShellHeaderProps {
  activeSlug?: string;
  leading?: ReactNode;
  title?: string;
  subtitle?: string;
}

export function WorkspaceShellHeader({
  activeSlug,
  leading,
  title = "Team Workspace",
  subtitle = "Workspace overview",
}: WorkspaceShellHeaderProps) {
  const router = useRouter();
  const { user, logout } = useAuth();

  function handleLogout() {
    logout();
    router.replace("/signin");
  }

  return (
    <header className="border-b border-slate-200 bg-white/90 backdrop-blur dark:border-slate-800 dark:bg-slate-900/90">
      <div className="mx-auto flex max-w-7xl items-center justify-between gap-3 px-4 py-4 sm:gap-4 sm:px-6 lg:px-8">
        <div className="flex min-w-0 items-center gap-3">
          {leading}
          <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-slate-950 text-white dark:bg-white dark:text-slate-950">
            <Layers3 className="size-5" aria-hidden="true" />
          </div>
          <div className="hidden min-w-0 sm:block">
            <p className="truncate font-semibold tracking-tight">{title}</p>
            <p className="truncate text-xs text-slate-500">{subtitle}</p>
          </div>
        </div>

        <div className="flex min-w-0 flex-1 items-center justify-end gap-3 sm:flex-none">
          <div className="w-full max-w-56 sm:w-64">
            <WorkspaceSwitcher activeSlug={activeSlug} />
          </div>

          <div className="hidden min-w-0 text-right lg:block">
            <p className="break-words text-sm font-medium">
              {user?.firstName} {user?.lastName}
            </p>
            <p className="break-all text-xs text-slate-500">{user?.email}</p>
          </div>
          <div className="hidden size-10 shrink-0 items-center justify-center rounded-full bg-indigo-100 text-sm font-semibold text-indigo-700 sm:flex">
            {user?.firstName.charAt(0)}
            {user?.lastName.charAt(0)}
          </div>
          <button
            aria-label="Sign out"
            className="flex size-10 shrink-0 items-center justify-center rounded-xl border border-slate-200 text-slate-600 transition hover:border-red-200 hover:bg-red-50 hover:text-red-600 focus:outline-none focus:ring-4 focus:ring-red-500/10 dark:border-slate-700 dark:text-slate-300 dark:hover:bg-red-950/40"
            onClick={handleLogout}
            type="button"
          >
            <LogOut className="size-4" aria-hidden="true" />
          </button>
        </div>
      </div>
    </header>
  );
}
