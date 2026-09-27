"use client";

import { X } from "lucide-react";
import { useCallback, useId, useRef } from "react";
import type { KeyboardEvent, ReactNode } from "react";

const FOCUSABLE_SELECTOR =
  'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

interface ModalProps {
  children: ReactNode;
  description?: string;
  isDismissible?: boolean;
  onClose: () => void;
  title: string;
}

export function Modal({
  children,
  description,
  isDismissible = true,
  onClose,
  title,
}: ModalProps) {
  const dialogRef = useRef<HTMLDivElement>(null);
  const titleId = useId();
  const descriptionId = useId();

  const handleClose = useCallback(() => {
    if (isDismissible) {
      onClose();
    }
  }, [isDismissible, onClose]);

  const handleKeyDown = useCallback(
    (event: KeyboardEvent<HTMLDivElement>) => {
      if (event.key === "Escape") {
        event.preventDefault();
        handleClose();
        return;
      }

      if (event.key !== "Tab") {
        return;
      }

      const focusableElements = Array.from(
        dialogRef.current?.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR) ??
          [],
      );

      if (focusableElements.length === 0) {
        return;
      }

      const firstElement = focusableElements[0];
      const lastElement = focusableElements[focusableElements.length - 1];

      if (event.shiftKey && document.activeElement === firstElement) {
        event.preventDefault();
        lastElement.focus();
      } else if (!event.shiftKey && document.activeElement === lastElement) {
        event.preventDefault();
        firstElement.focus();
      }
    },
    [handleClose],
  );

  return (
    <div
      className="fixed inset-0 z-50 flex items-end justify-center bg-slate-950/60 p-4 backdrop-blur-sm sm:items-center"
      onKeyDown={handleKeyDown}
    >
      <button
        aria-label={`Close ${title.toLowerCase()} dialog`}
        className="absolute inset-0 cursor-default"
        onClick={handleClose}
        tabIndex={-1}
        type="button"
      />

      <div
        aria-describedby={description ? descriptionId : undefined}
        aria-labelledby={titleId}
        aria-modal="true"
        className="relative max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-3xl border border-slate-200 bg-white p-6 shadow-2xl sm:p-8 dark:border-slate-800 dark:bg-slate-900"
        ref={dialogRef}
        role="dialog"
      >
        <div className="flex items-start justify-between gap-4">
          <div className="min-w-0">
            <h2
              className="text-xl font-semibold tracking-tight text-slate-950 dark:text-white"
              id={titleId}
            >
              {title}
            </h2>
            {description ? (
              <p
                className="mt-1 text-sm text-slate-500 dark:text-slate-400"
                id={descriptionId}
              >
                {description}
              </p>
            ) : null}
          </div>
          {isDismissible ? (
            <button
              aria-label="Close"
              className="flex size-9 shrink-0 items-center justify-center rounded-xl border border-slate-200 text-slate-500 transition hover:border-slate-300 hover:text-slate-700 focus:outline-none focus:ring-4 focus:ring-indigo-500/20 dark:border-slate-700 dark:text-slate-400"
              onClick={handleClose}
              type="button"
            >
              <X className="size-4" aria-hidden="true" />
            </button>
          ) : null}
        </div>

        {children}
      </div>
    </div>
  );
}
