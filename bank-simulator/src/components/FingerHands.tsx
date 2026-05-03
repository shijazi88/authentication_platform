import { Fingerprint } from "lucide-react";
import { cn } from "../lib/cn";
import type { FingerPosition } from "../types";

type FingerDef = {
  position: FingerPosition;
  label: string;
  height: number; // relative height (finger length)
};

// Right hand — thumb on the LEFT (closest to body center), pinky on the RIGHT (outer).
// This matches viewing your right hand palm-up with fingers pointing up.
const RIGHT: FingerDef[] = [
  { position: 1, label: "Thumb", height: 58 },
  { position: 2, label: "Index", height: 80 },
  { position: 3, label: "Middle", height: 88 },
  { position: 4, label: "Ring", height: 80 },
  { position: 5, label: "Pinky", height: 66 },
];

// Left hand — pinky on the LEFT (outer), thumb on the RIGHT (closest to body center).
// Mirror image so thumbs of both hands face each other.
const LEFT: FingerDef[] = [
  { position: 10, label: "Pinky", height: 66 },
  { position: 9, label: "Ring", height: 80 },
  { position: 8, label: "Middle", height: 88 },
  { position: 7, label: "Index", height: 80 },
  { position: 6, label: "Thumb", height: 58 },
];

function FingerPill({
  def,
  isSelected,
  onClick,
}: {
  def: FingerDef;
  isSelected: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={isSelected}
      aria-label={`Finger ${def.position} — ${def.label}`}
      className="group flex flex-col items-end gap-1.5 px-0.5 focus:outline-none"
    >
      <div
        className={cn(
          "relative w-10 rounded-t-full rounded-b-lg border-2 flex flex-col items-center justify-start pt-2.5 transition-all",
          isSelected
            ? "border-moi-blue bg-moi-blue/10 shadow-glow-blue ring-2 ring-moi-blue/20"
            : "border-ink-faint bg-paper group-hover:border-moi-blue/60 group-hover:bg-moi-blue/5",
        )}
        style={{ height: `${def.height}px` }}
      >
        <Fingerprint
          className={cn(
            "h-4 w-4 transition-colors",
            isSelected
              ? "text-moi-blue"
              : "text-ink-dim group-hover:text-moi-blue/70",
          )}
        />
        {isSelected && (
          <span className="absolute -top-1.5 -right-1.5 h-4 w-4 rounded-full bg-moi-gold text-white text-[9px] font-bold flex items-center justify-center ring-2 ring-paper">
            {def.position}
          </span>
        )}
      </div>
      <span
        className={cn(
          "text-[10px] font-semibold uppercase tracking-wider transition-colors",
          isSelected ? "text-moi-blue" : "text-ink-dim group-hover:text-ink-muted",
        )}
      >
        {def.label}
      </span>
    </button>
  );
}

function SingleHand({
  title,
  fingers,
  selected,
  onSelect,
}: {
  title: string;
  fingers: FingerDef[];
  selected: FingerPosition | null;
  onSelect: (p: FingerPosition) => void;
}) {
  return (
    <div className="flex-1 flex flex-col items-center">
      <div className="text-[10px] uppercase tracking-[0.2em] font-bold text-ink-muted mb-3">
        {title}
      </div>

      <div className="relative">
        {/* Fingers */}
        <div className="flex items-end justify-center gap-1.5 relative z-10 pb-1">
          {fingers.map((f) => (
            <FingerPill
              key={f.position}
              def={f}
              isSelected={selected === f.position}
              onClick={() => onSelect(f.position)}
            />
          ))}
        </div>

        {/* Palm silhouette */}
        <div className="relative -mt-2 h-9 w-[16rem] mx-auto rounded-b-[2rem] rounded-t-md bg-paper border-2 border-ink-faint border-t-0" />
      </div>
    </div>
  );
}

type Props = {
  selected: FingerPosition | null;
  onSelect: (p: FingerPosition) => void;
};

export function FingerHands({ selected, onSelect }: Props) {
  return (
    <div className="rounded-xl border border-ink-faint/70 bg-paper-raised p-5">
      <div className="flex items-start justify-center gap-8">
        <SingleHand
          title="Left hand"
          fingers={LEFT}
          selected={selected}
          onSelect={onSelect}
        />
        <div className="w-px self-stretch bg-ink-faint/50" />
        <SingleHand
          title="Right hand"
          fingers={RIGHT}
          selected={selected}
          onSelect={onSelect}
        />
      </div>
    </div>
  );
}
