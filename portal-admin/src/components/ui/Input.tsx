import { forwardRef, type InputHTMLAttributes } from "react";
import { cn } from "@/lib/cn";

export interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  leftIcon?: React.ReactNode;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ className, leftIcon, ...rest }, ref) => (
    <div className="relative">
      {leftIcon && (
        <div className="absolute start-3 top-1/2 -translate-y-1/2 text-text-muted pointer-events-none">
          {leftIcon}
        </div>
      )}
      <input
        ref={ref}
        className={cn(
          "w-full h-10 rounded-lg bg-bg-elevated/40 border border-border/15",
          "px-3 text-sm text-text placeholder:text-text-dim",
          "transition-all",
          "focus:outline-none focus:border-accent-violet/50 focus:bg-bg-elevated/60",
          "focus:ring-2 focus:ring-accent-violet/20",
          "disabled:opacity-50 disabled:pointer-events-none",
          leftIcon && "ps-10",
          className,
        )}
        {...rest}
      />
    </div>
  ),
);
Input.displayName = "Input";
