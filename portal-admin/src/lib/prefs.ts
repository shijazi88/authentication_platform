import { create } from "zustand";
import { persist } from "zustand/middleware";

export type Theme = "light" | "dark";
export type Lang = "en" | "ar";

export type PrefsState = {
  theme: Theme;
  lang: Lang;
  setTheme: (theme: Theme) => void;
  toggleTheme: () => void;
  setLang: (lang: Lang) => void;
};

/**
 * Persists the user's theme and language preferences and keeps the
 * <html> element in sync with them (class="dark", lang, dir).
 *
 * The same key is read by the inline script in index.html before React mounts
 * so the page never flashes the wrong theme/direction on first paint.
 */
export const usePrefs = create<PrefsState>()(
  persist(
    (set, get) => ({
      theme: "light",
      lang: "en",
      setTheme: (theme) => {
        applyTheme(theme);
        set({ theme });
      },
      toggleTheme: () => {
        const next: Theme = get().theme === "dark" ? "light" : "dark";
        applyTheme(next);
        set({ theme: next });
      },
      setLang: (lang) => {
        applyLang(lang);
        set({ lang });
      },
    }),
    {
      name: "middleware-portal-prefs",
    },
  ),
);

export function applyTheme(theme: Theme) {
  if (typeof document === "undefined") return;
  document.documentElement.classList.toggle("dark", theme === "dark");
}

export function applyLang(lang: Lang) {
  if (typeof document === "undefined") return;
  document.documentElement.setAttribute("lang", lang);
  document.documentElement.setAttribute("dir", lang === "ar" ? "rtl" : "ltr");
}

/**
 * Call once on app mount to ensure the DOM matches the persisted store
 * (covers the rare case where the inline bootstrap script's value drifts
 * from what zustand actually loaded).
 */
export function syncPrefsToDom() {
  const { theme, lang } = usePrefs.getState();
  applyTheme(theme);
  applyLang(lang);
}
