"use client";

import { Plus } from "lucide-react";

import { TaskCard } from "@/components/kanban/TaskCard";
import type { KanbanColumn as KanbanColumnModel, Task } from "@/lib/projects";

interface KanbanColumnProps {
  column: KanbanColumnModel;
  columns: KanbanColumnModel[];
  draggingTask: Task | null;
  dropIndex: number | null;
  isDropTarget: boolean;
  onAddTask: (columnId: number) => void;
  onDragEnd: () => void;
  onDragStart: (task: Task) => void;
  onHoverPosition: (columnId: number, index: number) => void;
  onMoveTask: (taskId: number, columnId: number) => void;
  onTaskDrop: (columnId: number, index: number) => void;
}

export function KanbanColumn({
  column,
  columns,
  draggingTask,
  dropIndex,
  isDropTarget,
  onAddTask,
  onDragEnd,
  onDragStart,
  onHoverPosition,
  onMoveTask,
  onTaskDrop,
}: KanbanColumnProps) {
  return (
    <section
      aria-label={`${column.name} column`}
      className={`flex w-72 shrink-0 flex-col rounded-2xl border p-3 transition sm:w-80 ${
        isDropTarget
          ? "border-indigo-400 bg-indigo-50/60 dark:border-indigo-500/60 dark:bg-indigo-950/20"
          : "border-slate-200 bg-slate-100/70 dark:border-slate-800 dark:bg-slate-900/60"
      }`}
      onDragOver={(event) => {
        if (!draggingTask) {
          return;
        }
        event.preventDefault();
        event.dataTransfer.dropEffect = "move";
        onHoverPosition(column.id, column.tasks.length);
      }}
      onDrop={(event) => {
        event.preventDefault();
        onTaskDrop(column.id, dropIndex ?? column.tasks.length);
      }}
    >
      <header className="mb-3 flex items-center justify-between gap-2 px-1">
        <div className="flex min-w-0 items-center gap-2">
          <h3 className="truncate text-sm font-semibold text-slate-800 dark:text-slate-100">
            {column.name}
          </h3>
          <span className="inline-flex size-5 shrink-0 items-center justify-center rounded-full bg-white text-[11px] font-semibold text-slate-500 dark:bg-slate-800 dark:text-slate-400">
            {column.tasks.length}
          </span>
        </div>
        <button
          aria-label={`Add a task to ${column.name}`}
          className="flex size-8 shrink-0 items-center justify-center rounded-lg text-slate-500 transition hover:bg-white hover:text-indigo-600 focus:outline-none focus:ring-4 focus:ring-indigo-500/20 dark:hover:bg-slate-800 dark:hover:text-indigo-300"
          onClick={() => onAddTask(column.id)}
          type="button"
        >
          <Plus aria-hidden="true" className="size-4" />
        </button>
      </header>

      <div className="flex min-h-24 flex-1 flex-col gap-2">
        {column.tasks.map((task, index) => (
          <div key={task.id}>
            {isDropTarget && dropIndex === index ? (
              <div
                aria-hidden="true"
                className="mb-2 h-1 rounded-full bg-indigo-500"
              />
            ) : null}
            <div
              onDragOver={(event) => {
                if (!draggingTask) {
                  return;
                }
                event.preventDefault();
                event.stopPropagation();
                const bounds = event.currentTarget.getBoundingClientRect();
                const isBelowMidpoint =
                  event.clientY > bounds.top + bounds.height / 2;
                onHoverPosition(column.id, isBelowMidpoint ? index + 1 : index);
              }}
            >
              <TaskCard
                columns={columns}
                isDragging={draggingTask?.id === task.id}
                onDragEnd={onDragEnd}
                onDragStart={() => onDragStart(task)}
                onMove={(columnId) => onMoveTask(task.id, columnId)}
                task={task}
              />
            </div>
          </div>
        ))}

        {isDropTarget && dropIndex === column.tasks.length ? (
          <div aria-hidden="true" className="h-1 rounded-full bg-indigo-500" />
        ) : null}

        {column.tasks.length === 0 && !isDropTarget ? (
          <p className="rounded-xl border border-dashed border-slate-300 px-3 py-6 text-center text-xs text-slate-400 dark:border-slate-700">
            No tasks yet
          </p>
        ) : null}
      </div>
    </section>
  );
}
