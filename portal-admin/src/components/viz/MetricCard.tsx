import { type ReactNode } from "react";
import { cn } from "@/lib/cn";
import { Card } from "@/components/ui/Card";

export interface MetricCardProps {
  label: string;
  value: ReactNode;
  delta?: { value: string; positive?: boolean };
  icon?: ReactNode;
  accentClass?: string;
  className?: string;
}

export function MetricCard({
  label,
  value,
  delta,
  icon,
  accentClass = "from-accent-violet to-accent-cyan",
  className,
}: MetricCardProps) {
  return (
    <Card className={cn("relative", className)}>
      <div className="p-5">
        <div className="flex items-start justify-between">
          <div className="text-[10px] font-semibold uppercase tracking-wider text-text-muted">
            {label}
          </div>
          {icon && (
            <div
              className={cn(
                "h-8 w-8 rounded-lg flex items-center justify-center bg-gradient-to-br text-white shadow-glow",
                accentClass,
              )}
            >
              {icon}
            </div>
          )}
        </div>
        <div className="mt-3 text-3xl font-bold tracking-tight text-gradient">
          {value}
        </div>
        {delta && (
          <div
            className={cn(
              "mt-2 inline-flex items-center gap-1 text-xs font-medium",
              delta.positive ? "text-accent-emerald" : "text-text-muted",
            )}
          >
            {delta.value}
          </div>
        )}
      </div>
      {/* subtle glow accent at top-right */}
      <div
        className={cn(
          "pointer-events-none absolute top-0 right-0 h-32 w-32 rounded-full blur-3xl opacity-20 bg-gradient-to-br",
          accentClass,
        )}
      />
    </Card>
  );
}
