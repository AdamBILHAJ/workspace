"use client";

import { ArrowLeft, LoaderCircle } from "lucide-react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useCallback, useEffect, useMemo, useState } from "react";

import { CreateTaskModal } from "@/components/kanban/CreateTaskModal";
import { KanbanBoard } from "@/components/kanban/KanbanBoard";
import { WorkspaceShellHeader } from "@/components/workspace/WorkspaceShellHeader";
import { useWorkspace } from "@/context/WorkspaceContext";
import { getApiErrorMessage } from "@/lib/auth";
import {
  addColumn,
  createTask,
  getProjectBoard,
  listProjects,
  moveTask,
  type Board,
  type Task,
  type TaskPriority,
} from "@/lib/projects";
import { listWorkspaceMembers, type WorkspaceMember } from "@/lib/workspaces";

interface PendingTaskModal {
  columnId: number;
  columnName: string;
}

function applyMove(
  board: Board,
  taskId: number,
  columnId: number,
  orderIndex: number,
): Board {
  const movedTask = board.columns
    .flatMap((column) => column.tasks)
    .find((task) => task.id === taskId);

  if (!movedTask) {
    return board;
  }

  return {
    ...board,
    columns: board.columns.map((column) => {
      const remaining = column.tasks.filter((task) => task.id !== taskId);
      const nextTasks =
        column.id === columnId
          ? (() => {
              const tasks = [...remaining];
              tasks.splice(
                Math.min(Math.max(orderIndex, 0), tasks.length),
                0,
                { ...movedTask, columnId },
              );
              return tasks;
            })()
          : remaining;

      return {
        ...column,
        tasks: nextTasks.map((task, index) => ({ ...task, orderIndex: index })),
      };
    }),
  };
}

function applyCreatedTask(board: Board, task: Task): Board {
  return {
    ...board,
    columns: board.columns.map((column) =>
      column.id === task.columnId
        ? { ...column, tasks: [...column.tasks, task] }
        : column,
    ),
  };
}

export default function ProjectBoardPage() {
  const params = useParams<{ slug: string; key: string }>();
  const slug = Array.isArray(params?.slug) ? params.slug[0] : params?.slug;
  const key = Array.isArray(params?.key) ? params.key[0] : params?.key;

  const { workspaces, isLoading: isWorkspaceLoading } = useWorkspace();
  const workspace = useMemo(
    () => workspaces.find((candidate) => candidate.slug === slug),
    [slug, workspaces],
  );
  const canManage =
    workspace?.role === "OWNER" || workspace?.role === "ADMIN";

  const [board, setBoard] = useState<Board | null>(null);
  const [members, setMembers] = useState<WorkspaceMember[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [moveError, setMoveError] = useState<string | null>(null);
  const [isMutating, setIsMutating] = useState(false);
  const [pendingTaskModal, setPendingTaskModal] =
    useState<PendingTaskModal | null>(null);
  const [reloadToken, setReloadToken] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    let isActive = true;

    async function fetchBoard(): Promise<void> {
      if (!workspace || !key) {
        return;
      }

      try {
        const projects = await listProjects(workspace.id, controller.signal);

        const project = projects.find(
          (candidate) => candidate.key.toUpperCase() === key.toUpperCase(),
        );

        if (!project) {
          if (isActive) {
            setError("This project does not exist in this workspace.");
            setBoard(null);
          }
          return;
        }

        const [loadedBoard, loadedMembers] = await Promise.all([
          getProjectBoard(project.id, controller.signal),
          listWorkspaceMembers(workspace.id, controller.signal),
        ]);

        if (isActive) {
          setBoard(loadedBoard);
          setMembers(loadedMembers);
        }
      } catch (caughtError) {
        if (isActive && !controller.signal.aborted) {
          setError(
            getApiErrorMessage(
              caughtError,
              "Unable to load the project board.",
            ),
          );
        }
      } finally {
        if (isActive) {
          setIsLoading(false);
        }
      }
    }

    void fetchBoard();

    return () => {
      isActive = false;
      controller.abort();
    };
  }, [key, reloadToken, workspace]);

  const handleRefresh = useCallback(() => {
    setIsLoading(true);
    setError(null);
    setReloadToken((token) => token + 1);
  }, []);

  const handleMoveTask = useCallback(
    async (taskId: number, columnId: number, orderIndex: number) => {
      if (!board) {
        return;
      }

      const previousBoard = board;
      setBoard(applyMove(board, taskId, columnId, orderIndex));
      setMoveError(null);
      setIsMutating(true);

      try {
        const updated = await moveTask(taskId, columnId, orderIndex);
        setBoard((current) =>
          current ? applyMove(current, updated.id, columnId, updated.orderIndex) : current,
        );
      } catch (caughtError) {
        setBoard(previousBoard);
        setMoveError(
          getApiErrorMessage(caughtError, "Unable to move that task."),
        );
      } finally {
        setIsMutating(false);
      }
    },
    [board],
  );

  const handleCreateTask = useCallback(
    async (input: {
      title: string;
      description: string;
      priority: TaskPriority;
      assigneeId: number | null;
    }) => {
      if (!pendingTaskModal) {
        return;
      }

      const task = await createTask(pendingTaskModal.columnId, input);
      setBoard((current) =>
        current ? applyCreatedTask(current, task) : current,
      );
    },
    [pendingTaskModal],
  );

  const handleAddColumn = useCallback(
    async (name: string) => {
      if (!board) {
        return;
      }

      setIsMutating(true);
      setMoveError(null);

      try {
        const column = await addColumn(board.project.id, name);
        setBoard((current) =>
          current
            ? { ...current, columns: [...current.columns, column] }
            : current,
        );
      } catch (caughtError) {
        setMoveError(
          getApiErrorMessage(caughtError, "Unable to add that column."),
        );
      } finally {
        setIsMutating(false);
      }
    },
    [board],
  );

  return (
    <main className="min-h-screen bg-slate-50 text-slate-950 dark:bg-slate-950 dark:text-white">
      <WorkspaceShellHeader
        activeSlug={slug}
        leading={
          <Link
            aria-label={`Back to ${workspace?.name ?? "workspace"}`}
            className="flex size-10 shrink-0 items-center justify-center rounded-xl border border-slate-200 text-slate-600 transition hover:border-slate-300 hover:text-slate-900 focus:outline-none focus:ring-4 focus:ring-indigo-500/20 dark:border-slate-700 dark:text-slate-300 dark:hover:text-white"
            href={`/workspaces/${slug}`}
          >
            <ArrowLeft className="size-4" aria-hidden="true" />
          </Link>
        }
        subtitle={workspace?.name ?? "Workspace"}
        title={board?.project.name ?? key ?? "Project"}
      />

      <div className="px-4 pb-10 sm:px-6 lg:px-8">
        {isWorkspaceLoading || isLoading ? (
          <div aria-busy="true" aria-label="Loading board" className="mt-8 space-y-4">
            <div className="h-10 w-64 animate-pulse rounded-xl bg-slate-200 dark:bg-slate-800" />
            <div className="flex gap-4 overflow-hidden">
              {[0, 1, 2].map((item) => (
                <div
                  className="h-72 w-72 shrink-0 animate-pulse rounded-2xl bg-slate-200 dark:bg-slate-800"
                  key={item}
                />
              ))}
            </div>
          </div>
        ) : error || !board ? (
          <section className="mx-auto mt-10 max-w-lg rounded-2xl border border-red-200 bg-white p-6 text-center shadow-sm dark:border-red-900/60 dark:bg-slate-900">
            <h1 className="text-lg font-semibold">Board unavailable</h1>
            <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">
              {error ?? "This board could not be loaded."}
            </p>
            <div className="mt-6 flex flex-col gap-3 sm:flex-row sm:justify-center">
              <button
                className="inline-flex h-11 items-center justify-center gap-2 rounded-xl bg-slate-950 px-5 text-sm font-semibold text-white transition hover:bg-indigo-700 focus:outline-none focus:ring-4 focus:ring-indigo-500/20 dark:bg-white dark:text-slate-950"
                onClick={handleRefresh}
                type="button"
              >
                <LoaderCircle aria-hidden="true" className="size-4" />
                Try again
              </button>
              <Link
                className="inline-flex h-11 items-center justify-center rounded-xl border border-slate-200 px-5 text-sm font-semibold text-slate-700 transition hover:bg-slate-100 focus:outline-none focus:ring-4 focus:ring-indigo-500/20 dark:border-slate-700 dark:text-slate-200 dark:hover:bg-slate-800"
                href={`/workspaces/${slug}`}
              >
                Back to workspace
              </Link>
            </div>
          </section>
        ) : (
          <KanbanBoard
            board={board}
            canManage={canManage}
            isMutating={isMutating}
            moveError={moveError}
            onAddColumn={(name) => void handleAddColumn(name)}
            onAddTask={(columnId) => {
              const column = board.columns.find(
                (candidate) => candidate.id === columnId,
              );
              if (column) {
                setPendingTaskModal({ columnId, columnName: column.name });
              }
            }}
            onMoveTask={(taskId, columnId, orderIndex) =>
              void handleMoveTask(taskId, columnId, orderIndex)
            }
          />
        )}
      </div>

      {pendingTaskModal ? (
        <CreateTaskModal
          columnName={pendingTaskModal.columnName}
          members={members}
          onClose={() => setPendingTaskModal(null)}
          onCreate={handleCreateTask}
        />
      ) : null}
    </main>
  );
}
