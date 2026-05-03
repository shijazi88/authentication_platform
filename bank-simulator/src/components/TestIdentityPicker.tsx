import { Users, Check } from "lucide-react";
import { useTranslation } from "react-i18next";
import { TEST_IDENTITIES } from "../testIdentities";
import { cn } from "../lib/cn";

type Props = {
  currentValue: string;
  onPick: (nationalNumber: string) => void;
};

export function TestIdentityPicker({ currentValue, onPick }: Props) {
  const { t } = useTranslation();
  return (
    <div className="rounded-xl border border-moi-gold/40 bg-moi-gold/5 p-3">
      <div className="flex items-center gap-1.5 text-[11px] font-bold text-moi-blue-dark uppercase tracking-wider mb-2">
        <Users className="h-3.5 w-3.5" />
        {t("verify.testIds")}
      </div>

      <div className="grid grid-cols-2 gap-2">
        {TEST_IDENTITIES.map((id) => {
          const raw = id.replace(/-/g, "");
          const isSelected = currentValue === raw;
          return (
            <button
              key={id}
              type="button"
              onClick={() => onPick(raw)}
              className={cn(
                "inline-flex items-center justify-center gap-1.5 px-2 py-1.5 rounded-lg border font-mono text-xs transition",
                isSelected
                  ? "border-moi-blue bg-moi-blue text-white shadow-sm"
                  : "border-ink-faint bg-paper text-ink hover:border-moi-blue/60 hover:bg-moi-blue/5",
              )}
            >
              {isSelected && <Check className="h-3 w-3" />}
              {id}
            </button>
          );
        })}
      </div>
    </div>
  );
}
