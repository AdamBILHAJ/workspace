import type { TaskUserSummary } from "@/lib/projects";

interface UserAvatarProps {
  user: TaskUserSummary | null;
  size?: "sm" | "md";
}

const SIZE_STYLES = {
  sm: "size-6 text-[10px]",
  md: "size-8 text-xs",
} as const;

export function getUserInitials(user: TaskUserSummary): string {
  const first = user.firstName?.charAt(0) ?? "";
  const last = user.lastName?.charAt(0) ?? "";
  const initials = `${first}${last}`.trim();

  if (initials.length > 0) {
    return initials.toUpperCase();
  }

  return user.email?.charAt(0).toUpperCase() ?? "?";
}

export function getUserFullName(user: TaskUserSummary): string {
  const fullName = `${user.firstName ?? ""} ${user.lastName ?? ""}`.trim();
  return fullName.length > 0 ? fullName : user.email;
}

export function UserAvatar({ user, size = "sm" }: UserAvatarProps) {
  if (!user) {
    return (
      <span
        aria-label="Unassigned"
        className={`inline-flex shrink-0 items-center justify-center rounded-full border border-dashed border-slate-300 font-semibold text-slate-400 dark:border-slate-600 ${SIZE_STYLES[size]}`}
        title="Unassigned"
      >
        ?
      </span>
    );
  }

  return (
    <span
      aria-label={`Assigned to ${getUserFullName(user)}`}
      className={`inline-flex shrink-0 items-center justify-center rounded-full bg-indigo-100 font-semibold text-indigo-700 dark:bg-indigo-950/60 dark:text-indigo-300 ${SIZE_STYLES[size]}`}
      title={getUserFullName(user)}
    >
      {getUserInitials(user)}
    </span>
  );
}
