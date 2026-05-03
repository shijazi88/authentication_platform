import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import { useApp } from "../store";
import en from "./en.json";
import ar from "./ar.json";

void i18n.use(initReactI18next).init({
  resources: {
    en: { translation: en },
    ar: { translation: ar },
  },
  lng: useApp.getState().lang,
  fallbackLng: "en",
  interpolation: { escapeValue: false },
  returnNull: false,
});

// Keep i18next in sync with the prefs store.
useApp.subscribe((state, prev) => {
  if (state.lang !== prev.lang) {
    void i18n.changeLanguage(state.lang);
  }
});

export default i18n;
