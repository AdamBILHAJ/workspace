"use client";

import { FolderKanban, LoaderCircle, Plus } from "lucide-react";
import Link from "next/link";
import { useCallback, useEffect, useState } from "react";

import { CreateProjectModal } from "@/components/projects/CreateProjectModal";
import { getApiErrorMessage } from "@/lib/auth";
import { createProject, listProjects, type Project } from "@/lib/projects";

interface ProjectListProps {
  canManage: boolean;
  workspaceId: number;
  workspaceSlug: string;
}

export function ProjectList({
  canManage,
  workspaceId,
  workspaceSlug,
}: ProjectListProps) {
  const [projects, setProjects] = useState<Project[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [reloadToken, setReloadToken] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    let isActive = true;

    async function fetchProjects(): Promise<void> {
      try {
        const data = await listProjects(workspaceId, controller.signal);

        if (isActive) {
          setProjects(data);
        }
      } catch (caughtError) {
        if (isActive && !controller.signal.aborted) {
          setError(
            getApiErrorMessage(caughtError, "Unable to load projects."),
          );
        }
      } finally {
        if (isActive) {
          setIsLoading(false);
        }
      }
    }

    void fetchProjects();

    return () => {
      isActive = false;
      controller.abort();
    };
  }, [reloadToken, workspaceId]);

  const handleRefresh = useCallback(() => {
    setIsLoading(true);
    setError(null);
    setReloadToken((token) => token + 1);
  }, []);

  if (isLoading) {
    return (
      <div
        aria-busy="true"
        aria-label="Loading projects"
        className="mt-8 animate-pulse space-y-4"
      >
        <div className="h-10 w-56 rounded-xl bg-slate-200 dark:bg-slate-800" />
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {[0, 1, 2].map((item) => (
            <div
              className="h-40 rounded-2xl bg-slate-200 dark:bg-slate-800"
              key={item}
            />
          ))}
        </div>
      </div>
    );
  }

  return (
    <section aria-label="Projects" className="mt-10">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold tracking-tight">Projects</h2>
          <p className="mt-0.5 text-sm text-slate-500 dark:text-slate-400">
            {projects.length === 0
              ? "No projects in this workspace yet."
              : `${projects.length} project${projects.length === 1 ? "" : "s"} in this workspace.`}
          </p>
        </div>
        {canManage ? (
          <button
            className="inline-flex h-10 items-center gap-2 rounded-xl bg-slate-950 px-4 text-sm font-semibold text-white transition hover:bg-indigo-700 focus:outline-none focus:ring-4 focus:ring-indigo-500/20 dark:bg-white dark:text-slate-950 dark:hover:bg-indigo-600"
            onClick={() => setIsCreateOpen(true)}
            type="button"
          >
            <Plus aria-hidden="true" className="size-4" />
            New project
          </button>
        ) : null}
      </div>

      {error ? (
        <div
          className="mt-6 rounded-2xl border border-red-200 bg-white p-6 text-center shadow-sm dark:border-red-900/60 dark:bg-slate-900"
          role="alert"
        >
          <p className="text-sm text-red-700 dark:text-red-300">{error}</p>
          <button
            className="mt-4 inline-flex h-10 items-center gap-2 rounded-xl border border-slate-200 px-4 text-sm font-semibold text-slate-700 transition hover:bg-slate-100 focus:outline-none focus:ring-4 focus:ring-indigo-500/20 dark:border-slate-700 dark:text-slate-200 dark:hover:bg-slate-800"
            onClick={handleRefresh}
            type="button"
          >
            <LoaderCircle aria-hidden="true" className="size-4" />
            Try again
          </button>
        </div>
      ) : projects.length === 0 ? (
        <div className="mt-6 rounded-2xl border border-dashed border-slate-300 px-6 py-14 text-center dark:border-slate-700">
          <FolderKanban
            aria-hidden="true"
            className="mx-auto size-8 text-slate-300 dark:text-slate-600"
          />
          <p className="mt-3 text-sm font-medium text-slate-700 dark:text-slate-200">
            {canManage
              ? "Create your first project to start a board."
              : "No projects yet. Ask an owner or admin to create one."}
          </p>
        </div>
      ) : (
        <ul className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {projects.map((project) => (
            <li key={project.id}>
              <Link
                className="group flex h-full flex-col rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition hover:-translate-y-0.5 hover:border-indigo-300 hover:shadow-md focus:outline-none focus:ring-4 focus:ring-indigo-500/20 dark:border-slate-800 dark:bg-slate-900 dark:hover:border-indigo-500/60"
                href={`/workspaces/${workspaceSlug}/projects/${project.key}`}
              >
                <div className="flex items-start justify-between gap-3">
                  <span className="inline-flex size-9 shrink-0 items-center justify-center rounded-xl bg-indigo-50 text-xs font-bold text-indigo-700 dark:bg-indigo-950/60 dark:text-indigo-300">
                    {project.key.slice(0, 2)}
                  </span>
                  <span className="break-all font-mono text-xs text-slate-400 dark:text-slate-500">
                    {project.key}
                  </span>
                </div>

                <h3 className="mt-3 break-words text-base font-semibold tracking-tight text-slate-900 group-hover:text-indigo-700 dark:text-white dark:group-hover:text-indigo-300">
                  {project.name}
                </h3>
                {project.description ? (
                  <p className="mt-1 line-clamp-2 break-words text-sm text-slate-500 dark:text-slate-400">
                    {project.description}
                  </p>
                ) : null}

                <div className="mt-4">
                  <div className="flex items-center justify-between text-xs text-slate-500 dark:text-slate-400">
                    <span>
                      {project.totalTasks === 0
                        ? "No tasks"
                        : `${project.completedTasks}/${project.totalTasks} done`}
                    </span>
                    <span>{project.completionPercent}%</span>
                  </div>
                  <div
                    aria-label={`${project.completionPercent}% complete`}
                    className="mt-2 h-2 w-full overflow-hidden rounded-full bg-slate-200 dark:bg-slate-800"
                    role="progressbar"
                    aria-valuemax={100}
                    aria-valuemin={0}
                    aria-valuenow={project.completionPercent}
                  >
                    <div
                      className="h-full rounded-full bg-indigo-500 transition-all"
                      style={{ width: `${project.completionPercent}%` }}
                    />
                  </div>
                </div>
              </Link>
            </li>
          ))}
        </ul>
      )}

      {isCreateOpen ? (
        <CreateProjectModal
          onClose={() => setIsCreateOpen(false)}
          onCreate={async (input) => {
            const created = await createProject(workspaceId, {
              name: input.name,
              description: input.description || undefined,
              key: input.key || undefined,
            });
            setProjects((current) =>
              [...current, created].sort((a, b) =>
                a.name.localeCompare(b.name),
              ),
            );
          }}
        />
      ) : null}
    </section>
  );
}
