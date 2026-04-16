import { type HTMLAttributes } from "react";
import { cn } from "@/lib/cn";

type BadgeTone =
  | "default"
  | "violet"
  | "cyan"
  | "emerald"
  | "amber"
  | "rose"
  | "neutral";

export interface BadgeProps extends HTMLAttributes<HTMLSpanElement> {
  tone?: BadgeTone;
}

const toneClasses: Record<BadgeTone, string> = {
  default: "bg-bg-elevated/60 text-text border-border/15",
  violet: "bg-accent-violet/15 text-accent-violet border-accent-violet/30",
  cyan: "bg-accent-cyan/15 text-accent-cyan border-accent-cyan/30",
  emerald:
    "bg-accent-emerald/15 text-accent-emerald border-accent-emerald/30",
  amber: "bg-accent-amber/15 text-accent-amber border-accent-amber/30",
  rose: "bg-accent-rose/15 text-accent-rose border-accent-rose/30",
  neutral: "bg-bg-elevated/40 text-text-muted border-border/10",
};

export function Badge({
  className,
  tone = "default",
  ...rest
}: BadgeProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-[10px] font-semibold uppercase tracking-wider border",
        toneClasses[tone],
        className,
      )}
      {...rest}
    />
  );
}

/**
 * Maps a domain status string to a sensible Badge tone.
 */
export function statusTone(status: string): BadgeTone {
  switch (status) {
    case "ACTIVE":
    case "SUCCESS":
      return "emerald";
    case "PENDING":
    case "INITIATED":
      return "amber";
    case "SUSPENDED":
    case "TIMEOUT":
      return "amber";
    case "FAILED":
    case "REJECTED":
    case "TERMINATED":
    case "CANCELED":
    case "EXPIRED":
      return "rose";
    default:
      return "neutral";
  }
}
