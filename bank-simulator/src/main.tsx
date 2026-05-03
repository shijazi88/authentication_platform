import React from "react";
import ReactDOM from "react-dom/client";
import { Toaster } from "sonner";
import { App } from "./App";
import { syncPrefsToDom } from "./store";
import "./i18n";
import "./index.css";

// Ensure DOM reflects persisted theme/lang before first paint.
syncPrefsToDom();

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <App />
    <Toaster position="top-right" richColors closeButton />
  </React.StrictMode>,
);
