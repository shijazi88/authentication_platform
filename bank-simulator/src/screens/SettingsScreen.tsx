import { useState } from "react";
import { toast } from "sonner";
import { useTranslation } from "react-i18next";
import {
  Globe,
  User,
  KeyRound,
  Eye,
  EyeOff,
  Save,
  RotateCcw,
  CheckCircle2,
  AlertTriangle,
  Loader2,
  Settings as SettingsIcon,
  Sun,
  Moon,
  Languages,
} from "lucide-react";
import { useApp } from "../store";
import { verifyIdentity } from "../api";
import { cn } from "../lib/cn";

export function SettingsScreen() {
  const credentials = useApp((s) => s.credentials)!;
  const setCredentials = useApp((s) => s.setCredentials);
  const theme = useApp((s) => s.theme);
  const setTheme = useApp((s) => s.setTheme);
  const lang = useApp((s) => s.lang);
  const setLang = useApp((s) => s.setLang);
  const { t } = useTranslation();

  const [baseUrl, setBaseUrl] = useState(credentials.baseUrl);
  const [clientId, setClientId] = useState(credentials.clientId);
  const [clientSecret, setClientSecret] = useState(credentials.clientSecret);
  const [showSecret, setShowSecret] = useState(false);
  const [testing, setTesting] = useState(false);
  const [testResult, setTestResult] = useState<
    | { ok: true; http: number }
    | { ok: false; http: number; message: string }
    | null
  >(null);

  const isDirty =
    baseUrl !== credentials.baseUrl ||
    clientId !== credentials.clientId ||
    clientSecret !== credentials.clientSecret;

  const validate = () => {
    if (!/^https?:\/\//.test(baseUrl.trim())) {
      toast.error(t("login.errorUrl"));
      return false;
    }
    if (!clientId.trim() || !clientSecret.trim()) {
      toast.error(t("login.errorCreds"));
      return false;
    }
    return true;
  };

  const save = () => {
    if (!validate()) return;
    setCredentials({
      baseUrl: baseUrl.trim(),
      clientId: clientId.trim(),
      clientSecret: clientSecret.trim(),
    });
    toast.success(t("settings.saved"));
    setTestResult(null);
  };

  const reset = () => {
    setBaseUrl(credentials.baseUrl);
    setClientId(credentials.clientId);
    setClientSecret(credentials.clientSecret);
    setTestResult(null);
  };

  const testConnection = async () => {
    if (!validate()) return;
    setTesting(true);
    setTestResult(null);
    // Use a benign bogus national number; we just want to see if auth works.
    // The backend will validate and either 200/400 (auth ok) or 401/403 (auth fail).
    const res = await verifyIdentity(baseUrl.trim(), clientId.trim(), clientSecret.trim(), {
      nationalNumber: "000000",
    });
    if (res.ok) {
      setTestResult({ ok: true, http: res.http });
      toast.success(t("settings.credentialsAccepted"));
    } else if (res.status === 400 || res.status === 422) {
      setTestResult({ ok: true, http: res.status });
      toast.success(t("settings.credentialsAccepted"));
    } else {
      setTestResult({ ok: false, http: res.status, message: res.message });
      toast.error(`${t("settings.connectionFailed")}: ${res.message}`);
    }
    setTesting(false);
  };

  return (
    <div className="max-w-3xl mx-auto p-8 space-y-6">
      <header>
        <h1 className="text-2xl font-extrabold text-ink flex items-center gap-2">
          <SettingsIcon className="h-6 w-6 text-moi-blue" />
          {t("settings.title")}
        </h1>
        <p className="text-sm text-ink-muted mt-1">{t("settings.subtitle")}</p>
      </header>

      {/* Appearance */}
      <div className="card p-6">
        <div className="text-xs font-bold text-ink-muted uppercase tracking-wider mb-4">
          {t("settings.appearance")}
        </div>
        <div className="grid sm:grid-cols-2 gap-6">
          <div>
            <div className="label">{t("settings.theme")}</div>
            <div className="flex gap-2">
              <SegmentButton
                active={theme === "light"}
                onClick={() => setTheme("light")}
                icon={<Sun className="h-4 w-4" />}
                label={t("settings.themeLight")}
              />
              <SegmentButton
                active={theme === "dark"}
                onClick={() => setTheme("dark")}
                icon={<Moon className="h-4 w-4" />}
                label={t("settings.themeDark")}
              />
            </div>
          </div>
          <div>
            <div className="label">{t("settings.language")}</div>
            <div className="flex gap-2">
              <SegmentButton
                active={lang === "en"}
                onClick={() => setLang("en")}
                icon={<Languages className="h-4 w-4" />}
                label={t("settings.langEnglish")}
              />
              <SegmentButton
                active={lang === "ar"}
                onClick={() => setLang("ar")}
                icon={<Languages className="h-4 w-4" />}
                label={t("settings.langArabic")}
              />
            </div>
          </div>
        </div>
      </div>

      <div className="card p-6 space-y-5">
        <div>
          <label className="label flex items-center gap-1.5">
            <Globe className="h-3.5 w-3.5" /> {t("settings.baseUrl")}
          </label>
          <input
            className="input"
            value={baseUrl}
            onChange={(e) => setBaseUrl(e.target.value)}
            placeholder="https://sanad-api.promatrix.ai"
            spellCheck={false}
            dir="ltr"
          />
          <p className="text-[11px] text-ink-dim mt-1">{t("settings.baseUrlHint")}</p>
        </div>

        <div>
          <label className="label flex items-center gap-1.5">
            <User className="h-3.5 w-3.5" /> {t("settings.clientId")}
          </label>
          <input
            className="input font-mono text-sm"
            value={clientId}
            onChange={(e) => setClientId(e.target.value)}
            placeholder="cli_..."
            autoComplete="off"
            spellCheck={false}
            dir="ltr"
          />
        </div>

        <div>
          <label className="label flex items-center gap-1.5">
            <KeyRound className="h-3.5 w-3.5" /> {t("settings.clientSecret")}
          </label>
          <div className="relative">
            <input
              className="input font-mono text-sm pr-11"
              type={showSecret ? "text" : "password"}
              value={clientSecret}
              onChange={(e) => setClientSecret(e.target.value)}
              placeholder="sec_..."
              autoComplete="off"
              spellCheck={false}
              dir="ltr"
            />
            <button
              type="button"
              onClick={() => setShowSecret((v) => !v)}
              className="absolute inset-y-0 end-2 my-auto h-8 w-8 grid place-items-center text-ink-dim hover:text-ink rounded-md hover:bg-paper-raised"
              aria-label={showSecret ? t("login.hideSecret") : t("login.showSecret")}
            >
              {showSecret ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
            </button>
          </div>
          <p className="text-[11px] text-ink-dim mt-1">{t("settings.secretHint")}</p>
        </div>

        {/* Test result banner */}
        {testResult && (
          <div
            className={`rounded-lg border p-3 flex items-start gap-3 ${
              testResult.ok
                ? "bg-moi-green/5 border-moi-green/30 text-moi-green"
                : "bg-moi-red/5 border-moi-red/30 text-moi-red"
            }`}
          >
            {testResult.ok ? (
              <CheckCircle2 className="h-5 w-5 flex-shrink-0" />
            ) : (
              <AlertTriangle className="h-5 w-5 flex-shrink-0" />
            )}
            <div className="text-sm">
              <div className="font-semibold">
                {testResult.ok ? t("settings.credentialsAccepted") : t("settings.connectionFailed")}
              </div>
              <div className="text-xs mt-0.5 opacity-90">
                HTTP {testResult.http}
                {!testResult.ok && ` · ${testResult.message}`}
              </div>
            </div>
          </div>
        )}

        <div className="flex items-center justify-between pt-2 border-t border-ink-faint/60">
          <button
            type="button"
            onClick={testConnection}
            disabled={testing}
            className="btn-secondary"
          >
            {testing ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin" />
                {t("settings.testing")}
              </>
            ) : (
              <>
                <CheckCircle2 className="h-4 w-4" />
                {t("settings.testConnection")}
              </>
            )}
          </button>

          <div className="flex items-center gap-2">
            {isDirty && (
              <button type="button" onClick={reset} className="btn-ghost">
                <RotateCcw className="h-4 w-4" />
                {t("settings.reset")}
              </button>
            )}
            <button
              type="button"
              onClick={save}
              disabled={!isDirty}
              className="btn-primary"
            >
              <Save className="h-4 w-4" />
              {t("settings.save")}
            </button>
          </div>
        </div>
      </div>

      {/* Quick-fill presets */}
      <div className="card p-5">
        <div className="text-xs font-bold text-ink-muted uppercase tracking-wider mb-3">
          {t("settings.quickPresets")}
        </div>
        <div className="grid sm:grid-cols-2 gap-3">
          <PresetButton
            label={t("settings.sanadProduction")}
            description="sanad-api.promatrix.ai"
            onClick={() => setBaseUrl("https://sanad-api.promatrix.ai")}
          />
          <PresetButton
            label={t("settings.localDev")}
            description="localhost:8080"
            onClick={() => setBaseUrl("http://localhost:8080")}
          />
        </div>
      </div>

      <div className="text-[11px] text-ink-dim text-center">
        {t("settings.privacy")}
      </div>

      <div className="pt-6 mt-2 border-t border-ink-faint/60 text-center">
        <div className="text-[11px] text-ink-dim">{t("settings.developedBy")}</div>
        <a
          href="https://www.promatrix.ai/en"
          target="_blank"
          rel="noopener noreferrer"
          className="inline-block mt-1 text-sm font-bold text-moi-blue hover:text-moi-blue-dark hover:underline"
        >
          Professional Matrix
        </a>
        <div className="text-[10px] text-ink-dim mt-0.5">
          promatrix.ai
        </div>
      </div>
    </div>
  );
}

function SegmentButton({
  active,
  onClick,
  icon,
  label,
}: {
  active: boolean;
  onClick: () => void;
  icon: React.ReactNode;
  label: string;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        "flex-1 inline-flex items-center justify-center gap-1.5 px-3 py-2 rounded-lg border text-sm font-semibold transition",
        active
          ? "border-moi-blue bg-moi-blue text-white shadow-sm"
          : "border-ink-faint bg-paper text-ink-muted hover:border-moi-blue/60 hover:text-ink",
      )}
    >
      {icon}
      {label}
    </button>
  );
}

function PresetButton({
  label,
  description,
  onClick,
}: {
  label: string;
  description: string;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="text-start rounded-lg border border-ink-faint bg-paper hover:border-moi-blue hover:bg-moi-blue/5 transition p-3"
    >
      <div className="text-sm font-semibold text-ink">{label}</div>
      <div className="text-[11px] font-mono text-ink-dim mt-0.5">{description}</div>
    </button>
  );
}
