"use client";

import { GripVertical, MessageSquare } from "lucide-react";

import { PriorityBadge } from "@/components/kanban/PriorityBadge";
import { UserAvatar } from "@/components/kanban/UserAvatar";
import type { KanbanColumn, Task } from "@/lib/projects";

interface TaskCardProps {
  task: Task;
  columns: KanbanColumn[];
  isDragging: boolean;
  onDragEnd: () => void;
  onDragStart: () => void;
  onMove: (columnId: number) => void;
  onOpenDetails: (task: Task) => void;
}

export function TaskCard({
  task,
  columns,
  isDragging,
  onDragEnd,
  onDragStart,
  onMove,
  onOpenDetails,
}: TaskCardProps) {
  return (
    <article
      className={`group rounded-xl border border-slate-200 bg-white p-3 shadow-sm transition dark:border-slate-700 dark:bg-slate-800/80 ${
        isDragging
          ? "opacity-40"
          : "hover:border-indigo-300 hover:shadow-md dark:hover:border-indigo-500/60"
      }`}
      draggable
      onDragEnd={onDragEnd}
      onDragStart={onDragStart}
    >
      <div className="flex items-start gap-2">
        <GripVertical
          aria-hidden="true"
          className="mt-0.5 size-4 shrink-0 cursor-grab text-slate-300 group-hover:text-slate-400 dark:text-slate-600"
        />
        <div className="min-w-0 flex-1">
          <button
            className="block w-full text-left focus:outline-none focus-visible:underline"
            onClick={(event) => {
              event.stopPropagation();
              onOpenDetails(task);
            }}
            type="button"
          >
            <h4 className="break-words text-sm font-medium leading-snug text-slate-900 dark:text-slate-100">
              {task.title}
            </h4>
          </button>
          {task.description ? (
            <button
              className="mt-1 block w-full text-left focus:outline-none focus-visible:underline"
              onClick={(event) => {
                event.stopPropagation();
                onOpenDetails(task);
              }}
              type="button"
            >
              <p className="line-clamp-3 break-words text-xs leading-relaxed text-slate-500 dark:text-slate-400">
                {task.description}
              </p>
            </button>
          ) : null}

          <div className="mt-3 flex items-center justify-between gap-2">
            <PriorityBadge priority={task.priority} />
            <UserAvatar user={task.assignee} />
          </div>

          <button
            aria-label={`Open comments and activity for ${task.title}`}
            className="mt-2 inline-flex items-center gap-1.5 rounded-lg px-1.5 py-1 text-xs font-medium text-slate-500 transition hover:bg-indigo-50 hover:text-indigo-700 focus:outline-none focus:ring-4 focus:ring-indigo-500/20 dark:text-slate-400 dark:hover:bg-indigo-950/50 dark:hover:text-indigo-300"
            onClick={(event) => {
              event.stopPropagation();
              onOpenDetails(task);
            }}
            type="button"
          >
            <MessageSquare aria-hidden="true" className="size-3.5" />
            Comments &amp; activity
          </button>

          <div className="mt-3">
            <label
              className="sr-only"
              htmlFor={`move-task-${task.id}`}
            >
              Move {task.title} to another column
            </label>
            <select
              className="h-8 w-full rounded-lg border border-slate-200 bg-slate-50 px-2 text-xs text-slate-600 transition hover:border-slate-300 focus:border-indigo-500 focus:outline-none focus:ring-4 focus:ring-indigo-500/20 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300"
              id={`move-task-${task.id}`}
              onChange={(event) => {
                const targetColumnId = Number(event.target.value);
                event.target.value = task.columnId.toString();
                if (targetColumnId !== task.columnId) {
                  onMove(targetColumnId);
                }
              }}
              value={task.columnId}
            >
              {columns.map((column) => (
                <option key={column.id} value={column.id}>
                  {column.name}
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>
    </article>
  );
}
