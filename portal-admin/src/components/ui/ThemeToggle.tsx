import { Moon, Sun } from "lucide-react";
import { useTranslation } from "react-i18next";
import { usePrefs } from "@/lib/prefs";
import { cn } from "@/lib/cn";

export function ThemeToggle({ className }: { className?: string }) {
  const { t } = useTranslation();
  const theme = usePrefs((s) => s.theme);
  const toggle = usePrefs((s) => s.toggleTheme);
  const isDark = theme === "dark";
  return (
    <button
      type="button"
      onClick={toggle}
      title={isDark ? t("topbar.theme.light") : t("topbar.theme.dark")}
      className={cn(
        "h-9 w-9 rounded-lg flex items-center justify-center",
        "border border-border/20 bg-bg-elevated/40 text-text-muted",
        "hover:text-text hover:border-border/40 transition-colors",
        className,
      )}
    >
      {isDark ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
    </button>
  );
}
