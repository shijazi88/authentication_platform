import { useState } from "react";
import { useTranslation } from "react-i18next";
import {
  LogOut,
  ScanLine,
  History as HistoryIcon,
  Settings as SettingsIcon,
  Fingerprint as FpIcon,
} from "lucide-react";
import { BrandHeader } from "./components/BrandHeader";
import { ConnectionStatus } from "./components/ConnectionStatus";
import { ThemeLangToggles } from "./components/ThemeLangToggles";
import { LoginScreen } from "./screens/LoginScreen";
import { VerifyScreen } from "./screens/VerifyScreen";
import { ResultScreen } from "./screens/ResultScreen";
import { HistoryScreen } from "./screens/HistoryScreen";
import { SettingsScreen } from "./screens/SettingsScreen";
import { DevicesScreen } from "./screens/DevicesScreen";
import { useApp } from "./store";
import type { VerifyResponse } from "./types";
import { cn } from "./lib/cn";

type Tab = "verify" | "history" | "devices" | "settings";
type ResultState =
  | { ok: true; data: VerifyResponse }
  | { ok: false; status: number; message: string; body?: unknown };

export function App() {
  const credentials = useApp((s) => s.credentials);
  const clearCredentials = useApp((s) => s.clearCredentials);
  const [tab, setTab] = useState<Tab>("verify");
  const [result, setResult] = useState<ResultState | null>(null);
  const { t } = useTranslation();

  if (!credentials) return <LoginScreen />;

  return (
    <div className="min-h-screen flex">
      {/* Sidebar */}
      <aside className="w-60 shrink-0 h-screen sticky top-0 bg-paper/80 backdrop-blur border-e border-ink-faint/60 flex flex-col">
        {/* Drag region — whole top section is draggable except the ConnectionStatus pill.
            Extra top padding leaves room for macOS traffic-light buttons. */}
        <div className="drag-region px-5 pt-10 pb-4 border-b border-ink-faint/60 space-y-3">
          <BrandHeader subtitle={t("brand.subtitle")} />
          <div className="no-drag">
            <ConnectionStatus />
          </div>
        </div>

        <nav className="flex-1 px-3 py-4 space-y-1">
          <NavButton
            active={tab === "verify"}
            icon={<ScanLine className="h-4 w-4" />}
            label={t("nav.verify")}
            onClick={() => {
              setResult(null);
              setTab("verify");
            }}
          />
          <NavButton
            active={tab === "history"}
            icon={<HistoryIcon className="h-4 w-4" />}
            label={t("nav.history")}
            onClick={() => {
              setResult(null);
              setTab("history");
            }}
          />
          <NavButton
            active={tab === "devices"}
            icon={<FpIcon className="h-4 w-4" />}
            label={t("nav.devices")}
            onClick={() => {
              setResult(null);
              setTab("devices");
            }}
          />
          <NavButton
            active={tab === "settings"}
            icon={<SettingsIcon className="h-4 w-4" />}
            label={t("nav.settings")}
            onClick={() => {
              setResult(null);
              setTab("settings");
            }}
          />
        </nav>

        <div className="px-5 py-4 border-t border-ink-faint/60 space-y-3">
          <div className="flex items-center justify-between">
            <div className="text-[11px] text-ink-dim min-w-0 flex-1">
              <div className="font-bold text-ink-muted uppercase tracking-wider">
                {t("nav.connectedAs")}
              </div>
              <div className="font-mono mt-0.5 truncate" title={credentials.clientId}>
                {credentials.clientId}
              </div>
            </div>
            <ThemeLangToggles />
          </div>

          <button
            onClick={() => {
              if (confirm("Disconnect and forget credentials?")) {
                clearCredentials();
                setResult(null);
              }
            }}
            className="btn-ghost w-full !justify-start !text-sm"
          >
            <LogOut className="h-4 w-4" /> {t("nav.disconnect")}
          </button>

          <div className="pt-3 border-t border-ink-faint/60 text-center">
            <div className="text-[10px] text-ink-dim">{t("nav.developedBy")}</div>
            <a
              href="https://www.promatrix.ai/en"
              target="_blank"
              rel="noopener noreferrer"
              className="text-[11px] font-bold text-moi-blue hover:text-moi-blue-dark hover:underline"
            >
              Professional Matrix
            </a>
          </div>
        </div>
      </aside>

      {/* Main */}
      <main className="flex-1 animate-fade-in">
        {tab === "verify" &&
          (result ? (
            <ResultScreen result={result} onBack={() => setResult(null)} />
          ) : (
            <VerifyScreen onResult={setResult} />
          ))}
        {tab === "history" && <HistoryScreen />}
        {tab === "devices" && <DevicesScreen />}
        {tab === "settings" && <SettingsScreen />}
      </main>
    </div>
  );
}

function NavButton({
  active,
  icon,
  label,
  onClick,
}: {
  active: boolean;
  icon: React.ReactNode;
  label: string;
  onClick: () => void;
}) {
  return (
    <button
      onClick={onClick}
      className={cn(
        "w-full flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-semibold transition",
        active
          ? "bg-moi-blue/10 text-moi-blue border border-moi-blue/20"
          : "text-ink-muted hover:text-ink hover:bg-paper-raised border border-transparent",
      )}
    >
      {icon}
      {label}
    </button>
  );
}
