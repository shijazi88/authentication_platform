import { useState } from "react";
import { toast } from "sonner";
import { useTranslation } from "react-i18next";
import { KeyRound, Globe, User, Eye, EyeOff, ShieldCheck } from "lucide-react";
import { useApp } from "../store";
import { BrandHeader } from "../components/BrandHeader";
import { ThemeLangToggles } from "../components/ThemeLangToggles";

export function LoginScreen() {
  const setCredentials = useApp((s) => s.setCredentials);
  const existing = useApp((s) => s.credentials);
  const { t } = useTranslation();

  const [baseUrl, setBaseUrl] = useState(existing?.baseUrl ?? "https://sanad-api.promatrix.ai");
  const [clientId, setClientId] = useState(existing?.clientId ?? "");
  const [clientSecret, setClientSecret] = useState(existing?.clientSecret ?? "");
  const [showSecret, setShowSecret] = useState(false);

  const connect = (e: React.FormEvent) => {
    e.preventDefault();
    if (!/^https?:\/\//.test(baseUrl)) {
      toast.error(t("login.errorUrl"));
      return;
    }
    if (!clientId || !clientSecret) {
      toast.error(t("login.errorCreds"));
      return;
    }
    setCredentials({ baseUrl: baseUrl.trim(), clientId: clientId.trim(), clientSecret: clientSecret.trim() });
    toast.success(t("login.connected"));
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-8">
      <div className="w-full max-w-4xl grid md:grid-cols-5 gap-8 items-stretch">
        {/* Left: brand + feature list */}
        <div className="md:col-span-2 flex flex-col justify-between p-8 rounded-3xl bg-gradient-to-br from-moi-blue via-moi-blue-dark to-moi-blue text-white relative overflow-hidden">
          <div className="absolute -top-20 -right-20 h-60 w-60 rounded-full bg-moi-gold/30 blur-3xl" />
          <div className="absolute -bottom-24 -left-10 h-60 w-60 rounded-full bg-moi-blue-light/40 blur-3xl" />
          <div className="relative">
            <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-white/10 backdrop-blur text-xs font-semibold border border-white/15 mb-6">
              <ShieldCheck className="h-3.5 w-3.5 text-moi-gold" />
              {t("login.badge")}
            </div>
            <h1 className="text-3xl font-extrabold leading-tight">{t("login.title")}</h1>
            <p className="mt-4 text-white/80 leading-relaxed">{t("login.subtitle")}</p>
          </div>
          <ul className="relative mt-8 space-y-3 text-sm">
            {[t("login.feature1"), t("login.feature2"), t("login.feature3")].map((txt) => (
              <li key={txt} className="flex items-start gap-2">
                <div className="mt-1 h-1.5 w-1.5 rounded-full bg-moi-gold flex-shrink-0" />
                <span className="text-white/85">{txt}</span>
              </li>
            ))}
          </ul>
        </div>

        {/* Right: form */}
        <div className="md:col-span-3 card p-8 animate-slide-up">
          <div className="flex items-start justify-between gap-3">
            <BrandHeader subtitle={t("brand.subtitle")} />
            <ThemeLangToggles />
          </div>

          <form onSubmit={connect} className="mt-8 space-y-5">
            <div>
              <label className="label flex items-center gap-1.5">
                <Globe className="h-3.5 w-3.5" /> {t("login.baseUrl")}
              </label>
              <input
                className="input"
                value={baseUrl}
                onChange={(e) => setBaseUrl(e.target.value)}
                placeholder="https://sanad-api.promatrix.ai"
                spellCheck={false}
                dir="ltr"
              />
            </div>

            <div>
              <label className="label flex items-center gap-1.5">
                <User className="h-3.5 w-3.5" /> {t("login.clientId")}
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
                <KeyRound className="h-3.5 w-3.5" /> {t("login.clientSecret")}
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
            </div>

            <button type="submit" className="btn-primary w-full mt-2">
              {t("login.connect")}
            </button>

            <p className="text-[11px] text-ink-dim text-center">{t("login.storedLocally")}</p>
          </form>
        </div>
      </div>
    </div>
  );
}
