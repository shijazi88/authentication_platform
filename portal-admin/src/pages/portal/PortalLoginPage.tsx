import { useState } from "react";
import { Navigate, useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { tenantLogin } from "@/api/tenant";
import { useTenantAuth } from "@/lib/tenantAuth";
import { Card, CardBody } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { BrandLogo } from "@/components/ui/BrandLogo";
import { ThemeToggle } from "@/components/ui/ThemeToggle";
import { LangToggle } from "@/components/ui/LangToggle";

export function PortalLoginPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const isAuthed = useTenantAuth((s) => s.isAuthenticated());
  const setSession = useTenantAuth((s) => s.setSession);

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (isAuthed) return <Navigate to="/portal" replace />;

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      const res = await tenantLogin(email.trim(), password);
      setSession({
        token: res.accessToken,
        email: res.email,
        tenantId: res.tenantId,
        tenantName: res.tenantName,
        expiresInSeconds: res.expiresInSeconds,
      });
      navigate("/portal");
    } catch (err: any) {
      setError(err?.response?.data?.message ?? t("portal.login.failed"));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="min-h-screen bg-bg text-text flex flex-col">
      <div className="flex justify-end p-4 gap-2">
        <LangToggle />
        <ThemeToggle />
      </div>
      <div className="flex-1 flex items-center justify-center p-6">
        <Card className="w-full max-w-sm">
          <CardBody className="p-8">
            <div className="flex flex-col items-center text-center mb-6">
              <BrandLogo size={48} />
              <h1 className="text-xl font-bold mt-3">{t("portal.login.title")}</h1>
              <p className="text-xs text-text-muted mt-1">{t("portal.login.subtitle")}</p>
            </div>
            <form onSubmit={onSubmit} className="space-y-4">
              <div>
                <Label htmlFor="email">{t("portal.login.email")}</Label>
                <Input
                  id="email"
                  type="email"
                  autoComplete="username"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  autoFocus
                />
              </div>
              <div>
                <Label htmlFor="password">{t("portal.login.password")}</Label>
                <Input
                  id="password"
                  type="password"
                  autoComplete="current-password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                />
              </div>
              {error && <p className="text-xs text-accent-rose">{error}</p>}
              <Button type="submit" className="w-full" loading={busy} disabled={!email || !password}>
                {t("portal.login.submit")}
              </Button>
            </form>
          </CardBody>
        </Card>
      </div>
    </div>
  );
}
