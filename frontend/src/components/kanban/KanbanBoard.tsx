"use client";

import { LoaderCircle, Plus } from "lucide-react";
import { useCallback, useState } from "react";

import { KanbanColumn } from "@/components/kanban/KanbanColumn";
import type { Board, KanbanColumn as KanbanColumnModel, Task } from "@/lib/projects";

interface KanbanBoardProps {
  board: Board;
  canManage: boolean;
  isMutating: boolean;
  moveError: string | null;
  onAddColumn: (name: string) => void;
  onAddTask: (columnId: number) => void;
  onMoveTask: (taskId: number, columnId: number, orderIndex: number) => void;
}

export function KanbanBoard({
  board,
  canManage,
  isMutating,
  moveError,
  onAddColumn,
  onAddTask,
  onMoveTask,
}: KanbanBoardProps) {
  const [draggingTask, setDraggingTask] = useState<Task | null>(null);
  const [hoverColumnId, setHoverColumnId] = useState<number | null>(null);
  const [hoverIndex, setHoverIndex] = useState<number | null>(null);

  const handleDragStart = useCallback((task: Task) => {
    setDraggingTask(task);
  }, []);

  const handleDragEnd = useCallback(() => {
    setDraggingTask(null);
    setHoverColumnId(null);
    setHoverIndex(null);
  }, []);

  const handleHoverPosition = useCallback(
    (columnId: number, index: number) => {
      setHoverColumnId(columnId);
      setHoverIndex(index);
    },
    [],
  );

  const handleDrop = useCallback(
    (columnId: number, index: number) => {
      const task = draggingTask;

      handleDragEnd();

      if (!task) {
        return;
      }

      const targetColumn = board.columns.find(
        (column: KanbanColumnModel) => column.id === columnId,
      );

      if (!targetColumn) {
        return;
      }

      const remaining = targetColumn.tasks.filter(
        (candidate) => candidate.id !== task.id,
      );
      const position = Math.min(Math.max(index, 0), remaining.length);
      onMoveTask(task.id, columnId, position);
    },
    [board.columns, draggingTask, handleDragEnd, onMoveTask],
  );

  const handleSelectMove = useCallback(
    (taskId: number, columnId: number) => {
      const targetColumn = board.columns.find(
        (column: KanbanColumnModel) => column.id === columnId,
      );

      if (!targetColumn) {
        return;
      }

      onMoveTask(taskId, columnId, targetColumn.tasks.length);
    },
    [board.columns, onMoveTask],
  );

  return (
    <div className="mt-8">
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold tracking-tight">Board</h2>
          <p className="mt-0.5 text-sm text-slate-500 dark:text-slate-400">
            Drag a card to reorder it, or use the column selector on each task
            for keyboard friendly moves.
          </p>
        </div>
        {canManage ? (
          <button
            className="inline-flex h-10 items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 text-sm font-semibold text-slate-700 transition hover:border-indigo-300 hover:text-indigo-700 focus:outline-none focus:ring-4 focus:ring-indigo-500/20 disabled:cursor-not-allowed disabled:opacity-60 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200 dark:hover:border-indigo-500/60"
            disabled={isMutating}
            onClick={() => {
              const name = window.prompt("Name the new column");
              if (name && name.trim().length > 0) {
                onAddColumn(name.trim());
              }
            }}
            type="button"
          >
            {isMutating ? (
              <LoaderCircle aria-hidden="true" className="size-4 animate-spin" />
            ) : (
              <Plus aria-hidden="true" className="size-4" />
            )}
            Add column
          </button>
        ) : null}
      </div>

      {moveError ? (
        <div
          className="mb-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-900/60 dark:bg-red-950/40 dark:text-red-300"
          role="alert"
        >
          {moveError}
        </div>
      ) : null}

      <div className="-mx-4 flex snap-x gap-4 overflow-x-auto px-4 pb-4 sm:mx-0 sm:px-0">
        {board.columns.map((column) => (
          <KanbanColumn
            column={column}
            columns={board.columns}
            draggingTask={draggingTask}
            dropIndex={hoverColumnId === column.id ? hoverIndex : null}
            isDropTarget={hoverColumnId === column.id}
            key={column.id}
            onAddTask={onAddTask}
            onDragEnd={handleDragEnd}
            onDragStart={handleDragStart}
            onHoverPosition={handleHoverPosition}
            onMoveTask={handleSelectMove}
            onTaskDrop={handleDrop}
          />
        ))}

        {board.columns.length === 0 ? (
          <p className="rounded-2xl border border-dashed border-slate-300 px-6 py-10 text-center text-sm text-slate-500 dark:border-slate-700 dark:text-slate-400">
            This board has no columns yet.
          </p>
        ) : null}
      </div>
    </div>
  );
}
