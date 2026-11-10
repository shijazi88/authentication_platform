import { forwardRef, type TextareaHTMLAttributes } from "react";
import { cn } from "@/lib/cn";

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaHTMLAttributes<HTMLTextAreaElement>>(
  ({ className, rows = 4, ...rest }, ref) => (
    <textarea
      ref={ref}
      rows={rows}
      className={cn(
        "w-full rounded-lg bg-bg-elevated/40 border border-border/15",
        "px-3 py-2 text-sm text-text placeholder:text-text-dim leading-relaxed",
        "transition-all resize-y",
        "focus:outline-none focus:border-accent-violet/50 focus:bg-bg-elevated/60",
        "focus:ring-2 focus:ring-accent-violet/20",
        "disabled:opacity-50 disabled:pointer-events-none",
        className,
      )}
      {...rest}
    />
  ),
);
Textarea.displayName = "Textarea";
