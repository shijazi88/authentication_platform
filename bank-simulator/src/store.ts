import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { HistoryEntry } from "./types";

type Credentials = {
  baseUrl: string;
  clientId: string;
  clientSecret: string;
};

export type Theme = "light" | "dark";
export type Lang = "en" | "ar";

type AppState = {
  credentials: Credentials | null;
  history: HistoryEntry[];
  theme: Theme;
  lang: Lang;
  setCredentials: (c: Credentials) => void;
  clearCredentials: () => void;
  addHistory: (entry: HistoryEntry) => void;
  clearHistory: () => void;
  setTheme: (t: Theme) => void;
  toggleTheme: () => void;
  setLang: (l: Lang) => void;
};

export const useApp = create<AppState>()(
  persist(
    (set, get) => ({
      credentials: null,
      history: [],
      theme: "light",
      lang: "en",
      setCredentials: (c) => set({ credentials: c }),
      clearCredentials: () => set({ credentials: null }),
      addHistory: (entry) =>
        set((s) => ({ history: [entry, ...s.history].slice(0, 50) })),
      clearHistory: () => set({ history: [] }),
      setTheme: (t) => {
        applyTheme(t);
        set({ theme: t });
      },
      toggleTheme: () => {
        const next: Theme = get().theme === "dark" ? "light" : "dark";
        applyTheme(next);
        set({ theme: next });
      },
      setLang: (l) => {
        applyLang(l);
        set({ lang: l });
      },
    }),
    {
      name: "sanad-bank-sim",
      partialize: (s) => ({
        credentials: s.credentials,
        history: s.history,
        theme: s.theme,
        lang: s.lang,
      }),
    },
  ),
);

export function applyTheme(t: Theme) {
  if (typeof document === "undefined") return;
  document.documentElement.classList.toggle("dark", t === "dark");
}

export function applyLang(l: Lang) {
  if (typeof document === "undefined") return;
  document.documentElement.setAttribute("lang", l);
  document.documentElement.setAttribute("dir", l === "ar" ? "rtl" : "ltr");
}

export function syncPrefsToDom() {
  const { theme, lang } = useApp.getState();
  applyTheme(theme);
  applyLang(lang);
}
