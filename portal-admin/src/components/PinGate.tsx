import { useEffect, useState, type ReactNode } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { Lock, ShieldCheck } from "lucide-react";
import { getPinStatus, setPin, verifyPin } from "@/api/pin";
import { setUnlock, getUnlockToken, TXN_LOCKED_EVENT } from "@/lib/txnUnlock";
import { Card, CardBody } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { PageLoader } from "@/components/ui/Spinner";

/**
 * Step-up gate for sensitive pages. Renders children only once the user has
 * exchanged their PIN for an unlock token; otherwise prompts to set or enter
 * the PIN. Re-prompts when the server reports the unlock expired (423).
 */
export function PinGate({ children }: { children: ReactNode }) {
  const [unlocked, setUnlocked] = useState(() => !!getUnlockToken());

  useEffect(() => {
    const onLocked = () => setUnlocked(false);
    window.addEventListener(TXN_LOCKED_EVENT, onLocked);
    return () => window.removeEventListener(TXN_LOCKED_EVENT, onLocked);
  }, []);

  if (unlocked) return <>{children}</>;
  return <PinPrompt onUnlocked={() => setUnlocked(true)} />;
}

function PinPrompt({ onUnlocked }: { onUnlocked: () => void }) {
  const { t } = useTranslation();
  const statusQ = useQuery({ queryKey: ["pin-status"], queryFn: getPinStatus });

  const [pin, setPinValue] = useState("");
  const [confirm, setConfirm] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const pinSet = statusQ.data?.pinSet;

  async function unlock(p: string) {
    const res = await verifyPin(p);
    setUnlock(res.unlockToken, res.expiresInSeconds);
    onUnlocked();
  }

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    if (pin.length < 4) {
      setError(t("pin.tooShort"));
      return;
    }
    setBusy(true);
    try {
      if (!pinSet) {
        if (pin !== confirm) {
          setError(t("pin.mismatch"));
          return;
        }
        await setPin(pin);
      }
      await unlock(pin);
    } catch (err: any) {
      setError(err?.response?.data?.message ?? t("pin.invalid"));
    } finally {
      setBusy(false);
    }
  }

  if (statusQ.isLoading) return <PageLoader />;

  return (
    <div className="flex items-center justify-center py-16">
      <Card className="w-full max-w-sm">
        <CardBody className="p-6">
          <div className="flex flex-col items-center text-center mb-5">
            <div className="h-12 w-12 rounded-xl bg-accent-violet/10 flex items-center justify-center mb-3">
              {pinSet ? (
                <Lock className="h-5 w-5 text-accent-violet" />
              ) : (
                <ShieldCheck className="h-5 w-5 text-accent-violet" />
              )}
            </div>
            <h2 className="text-lg font-bold text-text">
              {pinSet ? t("pin.enterTitle") : t("pin.setTitle")}
            </h2>
            <p className="text-xs text-text-muted mt-1">
              {pinSet ? t("pin.enterSubtitle") : t("pin.setSubtitle")}
            </p>
          </div>

          <form onSubmit={onSubmit} className="space-y-3">
            <div>
              <Label htmlFor="pin">{t("pin.label")}</Label>
              <Input
                id="pin"
                type="password"
                inputMode="numeric"
                autoComplete="off"
                maxLength={8}
                placeholder="••••"
                value={pin}
                onChange={(e) => setPinValue(e.target.value.replace(/\D/g, ""))}
                autoFocus
              />
            </div>
            {!pinSet && (
              <div>
                <Label htmlFor="confirm">{t("pin.confirmLabel")}</Label>
                <Input
                  id="confirm"
                  type="password"
                  inputMode="numeric"
                  autoComplete="off"
                  maxLength={8}
                  placeholder="••••"
                  value={confirm}
                  onChange={(e) => setConfirm(e.target.value.replace(/\D/g, ""))}
                />
              </div>
            )}
            {error && <p className="text-xs text-accent-rose">{error}</p>}
            <Button type="submit" className="w-full" loading={busy}>
              {pinSet ? t("pin.unlock") : t("pin.setAndUnlock")}
            </Button>
          </form>
        </CardBody>
      </Card>
    </div>
  );
}
