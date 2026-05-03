import { Sun, Moon, Languages } from "lucide-react";
import { useApp } from "../store";
import { cn } from "../lib/cn";

export function ThemeLangToggles() {
  const theme = useApp((s) => s.theme);
  const toggleTheme = useApp((s) => s.toggleTheme);
  const lang = useApp((s) => s.lang);
  const setLang = useApp((s) => s.setLang);

  return (
    <div className="flex items-center gap-1">
      <button
        type="button"
        onClick={toggleTheme}
        className="h-8 w-8 rounded-md grid place-items-center text-ink-muted hover:text-ink hover:bg-paper-raised transition"
        title={theme === "dark" ? "Light mode" : "Dark mode"}
        aria-label={theme === "dark" ? "Switch to light mode" : "Switch to dark mode"}
      >
        {theme === "dark" ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
      </button>
      <button
        type="button"
        onClick={() => setLang(lang === "en" ? "ar" : "en")}
        className={cn(
          "h-8 px-2 rounded-md grid place-items-center text-[11px] font-bold transition",
          "text-ink-muted hover:text-ink hover:bg-paper-raised",
        )}
        title={lang === "en" ? "العربية" : "English"}
        aria-label="Switch language"
      >
        <span className="inline-flex items-center gap-1">
          <Languages className="h-3.5 w-3.5" />
          {lang === "en" ? "AR" : "EN"}
        </span>
      </button>
    </div>
  );
}
