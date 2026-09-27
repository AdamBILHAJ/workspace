import type { TaskPriority } from "@/lib/projects";

const PRIORITY_STYLES: Record<TaskPriority, string> = {
  LOW: "bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-300",
  MEDIUM: "bg-sky-100 text-sky-700 dark:bg-sky-950/60 dark:text-sky-300",
  HIGH: "bg-amber-100 text-amber-700 dark:bg-amber-950/60 dark:text-amber-300",
  URGENT: "bg-rose-100 text-rose-700 dark:bg-rose-950/60 dark:text-rose-300",
};

export const TASK_PRIORITY_LABELS: Record<TaskPriority, string> = {
  LOW: "Low",
  MEDIUM: "Medium",
  HIGH: "High",
  URGENT: "Urgent",
};

interface PriorityBadgeProps {
  priority: TaskPriority;
}

export function PriorityBadge({ priority }: PriorityBadgeProps) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2 py-0.5 text-[11px] font-semibold tracking-wide ${PRIORITY_STYLES[priority]}`}
    >
      {TASK_PRIORITY_LABELS[priority]}
    </span>
  );
}
