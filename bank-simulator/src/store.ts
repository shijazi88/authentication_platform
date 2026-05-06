import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { HistoryEntry } from "./types";
import type { ProviderConfig, ProviderId } from "./providers/types";
import { DEFAULT_PROVIDER_ID, defaultConfigFor } from "./providers/registry";

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
  // Fingerprint device — vendor-agnostic. Selected provider plus per-provider
  // saved config (so switching providers doesn't lose your URL/timeout tweaks).
  selectedProviderId: ProviderId;
  providerConfigs: Record<ProviderId, ProviderConfig>;
  setCredentials: (c: Credentials) => void;
  clearCredentials: () => void;
  addHistory: (entry: HistoryEntry) => void;
  clearHistory: () => void;
  setTheme: (t: Theme) => void;
  toggleTheme: () => void;
  setLang: (l: Lang) => void;
  setSelectedProvider: (id: ProviderId) => void;
  updateProviderConfig: (id: ProviderId, patch: Partial<ProviderConfig>) => void;
  resetProviderConfig: (id: ProviderId) => void;
};

export const useApp = create<AppState>()(
  persist(
    (set, get) => ({
      credentials: null,
      history: [],
      theme: "light",
      lang: "en",
      selectedProviderId: DEFAULT_PROVIDER_ID,
      providerConfigs: {
        secugen: defaultConfigFor("secugen"),
        dermalog: defaultConfigFor("dermalog"),
      },
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
      setSelectedProvider: (id) => set({ selectedProviderId: id }),
      updateProviderConfig: (id, patch) =>
        set((s) => ({
          providerConfigs: {
            ...s.providerConfigs,
            [id]: { ...(s.providerConfigs[id] ?? defaultConfigFor(id)), ...patch },
          },
        })),
      resetProviderConfig: (id) =>
        set((s) => ({
          providerConfigs: { ...s.providerConfigs, [id]: defaultConfigFor(id) },
        })),
    }),
    {
      name: "sanad-bank-sim",
      partialize: (s) => ({
        credentials: s.credentials,
        history: s.history,
        theme: s.theme,
        lang: s.lang,
        selectedProviderId: s.selectedProviderId,
        providerConfigs: s.providerConfigs,
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
