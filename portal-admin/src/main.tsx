import React from "react";
import ReactDOM from "react-dom/client";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter } from "react-router-dom";
import { Toaster } from "sonner";
import App from "./App";
import "./index.css";
import "./i18n";
import { syncPrefsToDom, usePrefs } from "./lib/prefs";

// Ensure the DOM is in sync with the persisted preferences in case the inline
// bootstrap script in index.html missed anything.
syncPrefsToDom();

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
      staleTime: 30_000,
    },
  },
});

function ToasterWithTheme() {
  const theme = usePrefs((s) => s.theme);
  return (
    <Toaster
      theme={theme}
      position="top-right"
      toastOptions={{
        style: {
          background:
            theme === "dark"
              ? "rgba(13, 15, 23, 0.9)"
              : "rgba(255, 255, 255, 0.92)",
          border:
            theme === "dark"
              ? "1px solid rgba(255,255,255,0.08)"
              : "1px solid rgba(15,18,24,0.10)",
          backdropFilter: "blur(12px)",
          color: theme === "dark" ? "#e6e8f2" : "#0f1218",
        },
      }}
    />
  );
}

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <App />
        <ToasterWithTheme />
      </BrowserRouter>
    </QueryClientProvider>
  </React.StrictMode>,
);
