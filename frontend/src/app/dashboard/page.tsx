"use client";

import { CheckCircle2, Clock3, FolderKanban, UsersRound } from "lucide-react";
import { useEffect, useState } from "react";

import { WorkspaceShellHeader } from "@/components/workspace/WorkspaceShellHeader";
import { useWorkspace } from "@/context/WorkspaceContext";
import { useAuth } from "@/context/AuthContext";
import { listWorkspaceMembers } from "@/lib/workspaces";
import { listProjects, Project } from "@/lib/projects";

export default function DashboardPage() {
  const { user } = useAuth();
  const { workspaces } = useWorkspace();
  
  const [distinctMemberCount, setDistinctMemberCount] = useState<number | null>(null);
  const [projects, setProjects] = useState<Project[]>([]);
  const [isLoadingData, setIsLoadingData] = useState(true);

  // 1. Fetch team members across all workspaces (deduplicated)
  useEffect(() => {
    if (workspaces.length === 0) {
      setDistinctMemberCount(0);
      return;
    }

    const controller = new AbortController();
    let isActive = true;

    async function countMembers(): Promise<void> {
      try {
        const rosters = await Promise.all(
          workspaces.map((workspace) =>
            listWorkspaceMembers(workspace.id, controller.signal),
          ),
        );

        if (isActive) {
          const unique = new Set<number>();
          rosters.flat().forEach((member) => unique.add(member.user.id));
          setDistinctMemberCount(unique.size);
        }
      } catch {
        if (isActive && !controller.signal.aborted) {
          setDistinctMemberCount(null);
        }
      }
    }

    void countMembers();

    return () => {
      isActive = false;
      controller.abort();
    };
  }, [workspaces]);

  // 2. Fetch projects for all available workspaces using listProjects
  useEffect(() => {
    const controller = new AbortController();
    let isActive = true;

    async function fetchProjects() {
      if (workspaces.length === 0) {
        setProjects([]);
        setIsLoadingData(false);
        return;
      }

      setIsLoadingData(true);
      try {
        const projectResponses = await Promise.all(
          workspaces.map((workspace) => listProjects(workspace.id, controller.signal))
        );

        if (isActive) {
          setProjects(projectResponses.flat());
        }
      } catch (err) {
        if (isActive && !controller.signal.aborted) {
          console.error("Failed to load projects", err);
        }
      } finally {
        if (isActive) {
          setIsLoadingData(false);
        }
      }
    }

    void fetchProjects();

    return () => {
      isActive = false;
      controller.abort();
    };
  }, [workspaces]);

  // Calculations
  const memberTotal = workspaces.length === 0 ? 0 : distinctMemberCount;
  
  // Active projects count
  const activeProjectsCount = projects.length;

  // Calculate overall task completion percentage based on totalTasks vs completedTasks across projects
  const totalTasksSum = projects.reduce((acc, p) => acc + (p.totalTasks || 0), 0);
  const completedTasksSum = projects.reduce((acc, p) => acc + (p.completedTasks || 0), 0);
  const completionRate = totalTasksSum > 0 
    ? Math.round((completedTasksSum / totalTasksSum) * 100) 
    : 0;

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
            { 
              label: "Active projects", 
              value: isLoadingData ? "—" : String(activeProjectsCount), 
              icon: FolderKanban 
            },
            {
              label: "Team members",
              value: memberTotal === null ? "—" : String(memberTotal),
              icon: UsersRound,
            },
            { 
              label: "Tasks completed", 
              value: isLoadingData ? "—" : `${completionRate}%`, 
              icon: CheckCircle2 
            },
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
                <h2 className="font-semibold">Recent projects</h2>
                <p className="mt-1 text-sm text-slate-500">
                  Latest projects across your workspaces
                </p>
              </div>
              <Clock3 className="size-5 text-slate-400" aria-hidden="true" />
            </div>
            
            <div className="mt-8 grid gap-4 sm:grid-cols-3">
              {isLoadingData ? (
                <p className="text-sm text-slate-500 col-span-3">Loading projects...</p>
              ) : projects.length === 0 ? (
                <p className="text-sm text-slate-500 col-span-3">No projects found.</p>
              ) : (
                projects.slice(0, 3).map((project) => (
                  <div
                    className="rounded-xl bg-slate-50 p-4 dark:bg-slate-800/70"
                    key={project.id}
                  >
                    <div className="mb-6 h-2 w-12 rounded-full bg-indigo-200 dark:bg-indigo-900" />
                    <p className="text-sm font-medium truncate">{project.name}</p>
                    <p className="mt-1 text-xs text-slate-500">
                      {project.completionPercent}% completed
                    </p>
                  </div>
                ))
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