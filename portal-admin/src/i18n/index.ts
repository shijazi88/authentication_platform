import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import en from "./locales/en.json";
import ar from "./locales/ar.json";
import { usePrefs } from "@/lib/prefs";

void i18n.use(initReactI18next).init({
  resources: {
    en: { translation: en },
    ar: { translation: ar },
  },
  lng: usePrefs.getState().lang,
  fallbackLng: "en",
  interpolation: { escapeValue: false },
  returnNull: false,
});

// Keep i18next's active language in sync with the prefs store.
usePrefs.subscribe((state, prev) => {
  if (state.lang !== prev.lang) {
    void i18n.changeLanguage(state.lang);
  }
});

export default i18n;
