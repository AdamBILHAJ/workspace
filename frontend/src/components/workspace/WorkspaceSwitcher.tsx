"use client";

import {
  Building2,
  Check,
  ChevronDown,
  LoaderCircle,
  Plus,
  RefreshCw,
} from "lucide-react";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import type { KeyboardEvent } from "react";

import { CreateWorkspaceModal } from "@/components/workspace/CreateWorkspaceModal";
import { useWorkspace } from "@/context/WorkspaceContext";

interface WorkspaceSwitcherProps {
  activeSlug?: string;
}

const CREATE_ITEM_KEY = "__create__";

export function WorkspaceSwitcher({ activeSlug }: WorkspaceSwitcherProps) {
  const router = useRouter();
  const { workspaces, isLoading, error, refresh } = useWorkspace();
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState(0);
  const containerRef = useRef<HTMLDivElement>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const menuRef = useRef<HTMLDivElement>(null);
  const itemRefs = useRef<(HTMLButtonElement | null)[]>([]);
  const pendingFocusIndexRef = useRef<number | null>(null);

  const activeWorkspace = useMemo(
    () => workspaces.find((workspace) => workspace.slug === activeSlug),
    [activeSlug, workspaces],
  );

  const items = useMemo(
    () => [
      ...workspaces.map((workspace) => ({
        key: workspace.slug,
        label: workspace.name,
        organization: workspace.organization.name,
        slug: workspace.slug,
      })),
      { key: CREATE_ITEM_KEY, label: "Create workspace", organization: null, slug: null },
    ],
    [workspaces],
  );

  const closeMenu = useCallback((shouldFocusTrigger: boolean) => {
    setIsMenuOpen(false);
    setActiveIndex(0);
    if (shouldFocusTrigger) {
      triggerRef.current?.focus();
    }
  }, []);

  useEffect(() => {
    if (!isMenuOpen) {
      return;
    }

    function handlePointerDown(event: MouseEvent | TouchEvent) {
      if (!containerRef.current?.contains(event.target as Node)) {
        closeMenu(false);
      }
    }

    function handleFocusIn(event: FocusEvent) {
      if (!containerRef.current?.contains(event.target as Node)) {
        closeMenu(false);
      }
    }

    document.addEventListener("mousedown", handlePointerDown);
    document.addEventListener("touchstart", handlePointerDown);
    document.addEventListener("focusin", handleFocusIn);

    return () => {
      document.removeEventListener("mousedown", handlePointerDown);
      document.removeEventListener("touchstart", handlePointerDown);
      document.removeEventListener("focusin", handleFocusIn);
    };
  }, [closeMenu, isMenuOpen]);

  useEffect(() => {
    if (!isMenuOpen) {
      return;
    }

    const pendingIndex = pendingFocusIndexRef.current;

    if (pendingIndex === null) {
      menuRef.current?.focus();
      return;
    }

    pendingFocusIndexRef.current = null;
    const boundedIndex = (pendingIndex + items.length) % items.length;
    setActiveIndex(boundedIndex);
    itemRefs.current[boundedIndex]?.focus();
  }, [isMenuOpen, items.length]);

  function focusItem(index: number) {
    const boundedIndex = (index + items.length) % items.length;
    setActiveIndex(boundedIndex);
    itemRefs.current[boundedIndex]?.focus();
  }

  function selectItem(index: number) {
    const item = items[index];

    if (!item) {
      return;
    }

    closeMenu(true);

    if (item.key === CREATE_ITEM_KEY) {
      setIsCreateOpen(true);
      return;
    }

    router.push(`/workspaces/${item.slug}`);
  }

  function handleTriggerKeyDown(event: KeyboardEvent<HTMLButtonElement>) {
    if (event.key === "ArrowDown" || event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      pendingFocusIndexRef.current = 0;
      setIsMenuOpen(true);
      return;
    }

    if (event.key === "ArrowUp") {
      event.preventDefault();
      pendingFocusIndexRef.current = items.length - 1;
      setIsMenuOpen(true);
    }
  }

  function handleMenuKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    if (event.key === "Escape") {
      event.preventDefault();
      closeMenu(true);
      return;
    }

    if (event.key === "Tab") {
      closeMenu(false);
      return;
    }

    if (event.key === "ArrowDown") {
      event.preventDefault();
      focusItem(activeIndex + 1);
      return;
    }

    if (event.key === "ArrowUp") {
      event.preventDefault();
      focusItem(activeIndex - 1);
      return;
    }

    if (event.key === "Home") {
      event.preventDefault();
      focusItem(0);
      return;
    }

    if (event.key === "End") {
      event.preventDefault();
      focusItem(items.length - 1);
    }
  }

  const triggerLabel = activeWorkspace?.name ?? "Select workspace";

  return (
    <div className="relative min-w-0" ref={containerRef}>
      <button
        aria-expanded={isMenuOpen}
        aria-haspopup="menu"
        className="flex h-10 w-full min-w-0 items-center justify-between gap-2 rounded-xl border border-slate-200 bg-white px-3 text-sm font-medium text-slate-700 transition hover:border-slate-300 focus:outline-none focus:ring-4 focus:ring-indigo-500/20 disabled:cursor-not-allowed disabled:opacity-60 sm:w-64 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200 dark:hover:border-slate-600"
        disabled={isLoading}
        onClick={() => setIsMenuOpen((open) => !open)}
        onKeyDown={handleTriggerKeyDown}
        ref={triggerRef}
        type="button"
      >
        <span className="flex min-w-0 items-center gap-2">
          {isLoading ? (
            <LoaderCircle
              aria-hidden="true"
              className="size-4 shrink-0 animate-spin text-slate-400"
            />
          ) : (
            <Building2 aria-hidden="true" className="size-4 shrink-0 text-slate-400" />
          )}
          <span className="truncate">{isLoading ? "Loading workspaces…" : triggerLabel}</span>
        </span>
        <ChevronDown
          aria-hidden="true"
          className={`size-4 shrink-0 text-slate-400 transition ${
            isMenuOpen ? "rotate-180" : ""
          }`}
        />
      </button>

      {isMenuOpen ? (
        <div
          className="absolute left-0 right-0 z-40 mt-2 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-xl shadow-slate-950/10 sm:right-auto sm:w-72 dark:border-slate-800 dark:bg-slate-900"
          onKeyDown={handleMenuKeyDown}
          ref={menuRef}
          role="menu"
          tabIndex={-1}
        >
          {workspaces.length === 0 ? (
            <p className="px-4 py-3 text-sm text-slate-500">
              You do not belong to any workspace yet.
            </p>
          ) : (
            <ul className="max-h-72 overflow-y-auto py-1">
              {items.slice(0, -1).map((item, index) => {
                const isCurrent = item.slug === activeSlug;

                return (
                  <li key={item.key} role="none">
                    <button
                      className="flex w-full items-center justify-between gap-3 px-4 py-2.5 text-left text-sm text-slate-700 transition hover:bg-slate-100 focus:bg-slate-100 focus:outline-none dark:text-slate-200 dark:hover:bg-slate-800 dark:focus:bg-slate-800"
                      onClick={() => selectItem(index)}
                      ref={(element) => {
                        itemRefs.current[index] = element;
                      }}
                      role="menuitem"
                      tabIndex={-1}
                      type="button"
                    >
                      <span className="min-w-0">
                        <span className="block truncate font-medium">{item.label}</span>
                        <span className="block truncate text-xs text-slate-500">
                          {item.organization}
                        </span>
                      </span>
                      {isCurrent ? (
                        <Check
                          aria-hidden="true"
                          className="size-4 shrink-0 text-indigo-600"
                        />
                      ) : null}
                    </button>
                  </li>
                );
              })}
            </ul>
          )}

          <div className="border-t border-slate-200 p-1 dark:border-slate-800">
            {error ? (
              <button
                className="flex w-full items-center gap-2 rounded-xl px-3 py-2 text-left text-sm text-red-600 transition hover:bg-red-50 focus:bg-red-50 focus:outline-none dark:text-red-400 dark:hover:bg-red-950/40"
                onClick={() => {
                  closeMenu(true);
                  void refresh();
                }}
                role="menuitem"
                tabIndex={-1}
                type="button"
              >
                <RefreshCw aria-hidden="true" className="size-4 shrink-0" />
                Retry loading workspaces
              </button>
            ) : null}

            <button
              className="flex w-full items-center gap-2 rounded-xl px-3 py-2 text-left text-sm font-medium text-indigo-600 transition hover:bg-indigo-50 focus:bg-indigo-50 focus:outline-none dark:text-indigo-400 dark:hover:bg-indigo-950/40"
              onClick={() => selectItem(items.length - 1)}
              ref={(element) => {
                itemRefs.current[items.length - 1] = element;
              }}
              role="menuitem"
              tabIndex={-1}
              type="button"
            >
              <Plus aria-hidden="true" className="size-4 shrink-0" />
              {items[items.length - 1].label}
            </button>
          </div>
        </div>
      ) : null}

      {isCreateOpen ? (
        <CreateWorkspaceModal
          onClose={() => setIsCreateOpen(false)}
          onCreated={(workspace) => router.push(`/workspaces/${workspace.slug}`)}
        />
      ) : null}
    </div>
  );
}
