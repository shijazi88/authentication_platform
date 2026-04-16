import {
  Listbox,
  ListboxButton,
  ListboxOption,
  ListboxOptions,
  Transition,
} from "@headlessui/react";
import { Check, ChevronDown } from "lucide-react";
import { Fragment, type ReactNode } from "react";
import { cn } from "@/lib/cn";

export interface SelectOption<T extends string | number> {
  value: T;
  label: ReactNode;
  description?: ReactNode;
}

export interface SelectProps<T extends string | number> {
  value: T | null;
  onChange: (value: T) => void;
  options: SelectOption<T>[];
  placeholder?: string;
  className?: string;
  disabled?: boolean;
}

export function Select<T extends string | number>({
  value,
  onChange,
  options,
  placeholder = "Select…",
  className,
  disabled,
}: SelectProps<T>) {
  const current = options.find((o) => o.value === value);
  return (
    <Listbox
      value={value ?? undefined}
      onChange={(v: T) => onChange(v)}
      disabled={disabled}
    >
      <div className={cn("relative", className)}>
        <ListboxButton
          className={cn(
            "w-full h-10 rounded-lg bg-bg-elevated/40 border border-border/15",
            "px-3 pe-9 text-sm text-start flex items-center",
            "transition-all hover:border-border/30",
            "focus:outline-none focus:border-accent-violet/50 focus:ring-2 focus:ring-accent-violet/20",
            "disabled:opacity-50",
          )}
        >
          <span className={cn("truncate", !current && "text-text-dim")}>
            {current ? current.label : placeholder}
          </span>
          <ChevronDown className="absolute end-3 top-1/2 -translate-y-1/2 h-4 w-4 text-text-muted" />
        </ListboxButton>
        <Transition
          as={Fragment}
          enter="transition ease-out duration-150"
          enterFrom="opacity-0 translate-y-1"
          enterTo="opacity-100 translate-y-0"
          leave="transition ease-in duration-100"
          leaveFrom="opacity-100"
          leaveTo="opacity-0"
        >
          <ListboxOptions
            anchor="bottom start"
            className={cn(
              "z-50 mt-1 w-[var(--button-width)] glass rounded-lg shadow-card max-h-64 overflow-auto py-1 [--anchor-gap:4px]",
            )}
          >
            {options.map((opt) => (
              <ListboxOption
                key={String(opt.value)}
                value={opt.value}
                className={({ focus, selected }) =>
                  cn(
                    "px-3 py-2 text-sm cursor-pointer flex items-start gap-2",
                    focus && "bg-bg-hover/60",
                    selected && "text-accent-violet",
                  )
                }
              >
                {({ selected }) => (
                  <>
                    <span
                      className={cn(
                        "h-4 w-4 mt-0.5 shrink-0",
                        selected ? "text-accent-violet" : "text-transparent",
                      )}
                    >
                      <Check className="h-4 w-4" />
                    </span>
                    <div>
                      <div className="text-text">{opt.label}</div>
                      {opt.description && (
                        <div className="text-xs text-text-muted">
                          {opt.description}
                        </div>
                      )}
                    </div>
                  </>
                )}
              </ListboxOption>
            ))}
          </ListboxOptions>
        </Transition>
      </div>
    </Listbox>
  );
}
