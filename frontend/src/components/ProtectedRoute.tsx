"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";
import type { ReactNode } from "react";

import { useAuth } from "@/context/AuthContext";

export function ProtectedRoute({ children }: { children: ReactNode }) {
  const router = useRouter();
  const { user, isLoading } = useAuth();

  useEffect(() => {
    if (!isLoading && !user) {
      router.replace("/signin");
    }
  }, [isLoading, router, user]);

  if (isLoading || !user) {
    return (
      <main
        className="min-h-screen bg-slate-50 px-4 py-8 dark:bg-slate-950"
        aria-busy="true"
        aria-label="Loading protected page"
      >
        <div className="mx-auto max-w-7xl animate-pulse">
          <div className="mb-8 flex items-center justify-between">
            <div className="h-10 w-44 rounded-xl bg-slate-200 dark:bg-slate-800" />
            <div className="size-10 rounded-full bg-slate-200 dark:bg-slate-800" />
          </div>
          <div className="grid gap-6 lg:grid-cols-3">
            <div className="h-80 rounded-2xl bg-slate-200 dark:bg-slate-800 lg:col-span-2" />
            <div className="h-80 rounded-2xl bg-slate-200 dark:bg-slate-800" />
          </div>
          <div className="mt-6 h-56 rounded-2xl bg-slate-200 dark:bg-slate-800" />
        </div>
      </main>
    );
  }

  return children;
}
