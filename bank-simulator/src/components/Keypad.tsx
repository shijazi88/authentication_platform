import { Delete } from "lucide-react";
import { cn } from "../lib/cn";

type Props = {
  onKey: (digit: string) => void;
  onBackspace: () => void;
  maxLength?: number;
  currentLength?: number;
};

const KEYS: Array<{ label: string; value?: string; kind?: "digit" | "backspace" | "empty" }> = [
  { label: "1", value: "1", kind: "digit" },
  { label: "2", value: "2", kind: "digit" },
  { label: "3", value: "3", kind: "digit" },
  { label: "4", value: "4", kind: "digit" },
  { label: "5", value: "5", kind: "digit" },
  { label: "6", value: "6", kind: "digit" },
  { label: "7", value: "7", kind: "digit" },
  { label: "8", value: "8", kind: "digit" },
  { label: "9", value: "9", kind: "digit" },
  { label: "", kind: "empty" },
  { label: "0", value: "0", kind: "digit" },
  { label: "", kind: "backspace" },
];

export function Keypad({ onKey, onBackspace, maxLength = 24, currentLength = 0 }: Props) {
  const atMax = currentLength >= maxLength;
  const empty = currentLength === 0;

  return (
    <div className="inline-grid grid-cols-3 gap-2 p-3 rounded-xl bg-paper-raised border border-ink-faint/70">
      {KEYS.map((k, i) => {
        if (k.kind === "empty") {
          return <div key={i} aria-hidden className="h-12 w-14" />;
        }
        if (k.kind === "backspace") {
          return (
            <button
              key={i}
              type="button"
              onClick={onBackspace}
              disabled={empty}
              aria-label="Backspace"
              className={cn(
                "h-12 w-14 rounded-lg border border-ink-faint bg-paper flex items-center justify-center text-ink-muted",
                "transition shadow-sm",
                !empty && "hover:bg-moi-red/5 hover:border-moi-red/40 hover:text-moi-red active:scale-95",
                empty && "opacity-40 cursor-not-allowed",
              )}
            >
              <Delete className="h-4 w-4" />
            </button>
          );
        }
        // digit
        return (
          <button
            key={i}
            type="button"
            onClick={() => onKey(k.value!)}
            disabled={atMax}
            className={cn(
              "h-12 w-14 rounded-lg border border-ink-faint bg-paper text-ink font-bold text-lg",
              "transition shadow-sm",
              !atMax &&
                "hover:bg-moi-blue/5 hover:border-moi-blue/40 hover:text-moi-blue active:scale-95",
              atMax && "opacity-40 cursor-not-allowed",
            )}
          >
            {k.label}
          </button>
        );
      })}
    </div>
  );
}
