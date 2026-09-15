import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";
import { Activity, RefreshCw, RotateCcw, Save, Zap } from "lucide-react";
import {
  getResilienceSettings,
  updateResilienceSettings,
  getResilienceStatus,
  resetCircuitBreaker,
} from "@/api/settings";
import type { ResilienceSettings } from "@/types/api";
import { Card, CardBody, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Badge } from "@/components/ui/Badge";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { PageLoader } from "@/components/ui/Spinner";
import { formatDate } from "@/lib/format";
import { cn } from "@/lib/cn";

const STATE_TONE: Record<string, "emerald" | "rose" | "amber" | "neutral"> = {
  CLOSED: "emerald",
  OPEN: "rose",
  HALF_OPEN: "amber",
  DISABLED: "neutral",
  FORCED_OPEN: "rose",
};

/** Circuit breaker + retry policy for the identity-provider connectors, applied live. */
export function ResilienceSettingsCard() {
  const { t } = useTranslation();
  const qc = useQueryClient();
  const q = useQuery({ queryKey: ["settings", "resilience"], queryFn: getResilienceSettings });
  const statusQ = useQuery({
    queryKey: ["settings", "resilience", "status"],
    queryFn: getResilienceStatus,
    refetchInterval: 10_000,
  });

  const [draft, setDraft] = useState<ResilienceSettings | null>(null);
  useEffect(() => {
    if (q.data) setDraft(q.data.value);
  }, [q.data]);

  const saveMut = useMutation({
    mutationFn: (v: ResilienceSettings) => updateResilienceSettings(v),
    onSuccess: (data) => {
      qc.setQueryData(["settings", "resilience"], data);
      qc.invalidateQueries({ queryKey: ["settings", "resilience", "status"] });
      toast.success(t("settings.resilience.saved"));
    },
    onError: (e: any) => toast.error(e?.response?.data?.message ?? t("settings.resilience.error")),
  });
  const resetMut = useMutation({
    mutationFn: (connector?: string) => resetCircuitBreaker(connector),
    onSuccess: (data) => {
      qc.setQueryData(["settings", "resilience", "status"], data);
      toast.success(t("settings.resilience.resetDone"));
    },
    onError: (e: any) => toast.error(e?.response?.data?.message ?? t("settings.resilience.error")),
  });

  if (q.isLoading || !draft) return <PageLoader />;

  const set = <K extends keyof ResilienceSettings>(k: K, v: ResilienceSettings[K]) =>
    setDraft({ ...draft, [k]: v });
  const num = (k: keyof ResilienceSettings) => (e: React.ChangeEvent<HTMLInputElement>) =>
    set(k, Number(e.target.value) as never);
  const dirty = JSON.stringify(draft) !== JSON.stringify(q.data?.value);

  return (
    <div className="space-y-4">
      {/* Live state */}
      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <Activity className="h-4 w-4 text-accent-cyan" />
            <CardTitle>{t("settings.resilience.statusTitle")}</CardTitle>
          </div>
          <Button
            variant="ghost"
            size="sm"
            leftIcon={<RefreshCw className="h-3.5 w-3.5" />}
            onClick={() => statusQ.refetch()}
          >
            {t("settings.resilience.refresh")}
          </Button>
        </CardHeader>
        <CardBody>
          <p className="text-xs text-text-muted mb-3">{t("settings.resilience.statusHint")}</p>
          <div className="space-y-2">
            {(statusQ.data ?? []).map((s) => (
              <div
                key={s.connector}
                className="flex flex-wrap items-center justify-between gap-2 rounded-lg bg-bg-elevated/40 border border-border/10 p-3"
              >
                <div className="flex items-center gap-3">
                  <span className="font-mono text-xs">{s.connector}</span>
                  <Badge tone={STATE_TONE[s.state] ?? "neutral"}>
                    {t(`settings.resilience.state.${s.state}`, s.state)}
                  </Badge>
                </div>
                <div className="text-xs text-text-muted" dir="ltr">
                  {t("settings.resilience.metrics", {
                    rate: s.failureRatePercent == null ? "—" : `${s.failureRatePercent.toFixed(0)}%`,
                    failed: s.failedCalls,
                    buffered: s.bufferedCalls,
                    blocked: s.notPermittedCalls,
                  })}
                </div>
                {(s.state === "OPEN" || s.state === "HALF_OPEN" || s.state === "FORCED_OPEN") && (
                  <Button
                    variant="secondary"
                    size="sm"
                    leftIcon={<Zap className="h-3.5 w-3.5" />}
                    loading={resetMut.isPending}
                    onClick={() => resetMut.mutate(s.connector)}
                  >
                    {t("settings.resilience.reset")}
                  </Button>
                )}
              </div>
            ))}
          </div>
        </CardBody>
      </Card>

      {/* Policy */}
      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <Zap className="h-4 w-4 text-accent-amber" />
            <CardTitle>{t("settings.resilience.title")}</CardTitle>
          </div>
          <div className="flex items-center gap-1">
            <Button
              variant="ghost"
              size="sm"
              leftIcon={<RotateCcw className="h-3.5 w-3.5" />}
              disabled={!dirty}
              onClick={() => q.data && setDraft(q.data.value)}
            >
              {t("common.cancel")}
            </Button>
            <Button
              size="sm"
              leftIcon={<Save className="h-3.5 w-3.5" />}
              loading={saveMut.isPending}
              disabled={!dirty}
              onClick={() => saveMut.mutate(draft)}
            >
              {t("settings.resilience.save")}
            </Button>
          </div>
        </CardHeader>
        <CardBody className="space-y-6">
          <p className="text-xs text-text-muted">{t("settings.resilience.intro")}</p>

          <Section
            title={t("settings.resilience.breaker")}
            hint={t("settings.resilience.breakerHint")}
            toggle={{ checked: draft.circuitBreakerEnabled, onChange: (v) => set("circuitBreakerEnabled", v), label: t("settings.resilience.enabled") }}
          >
            <fieldset disabled={!draft.circuitBreakerEnabled} className="grid grid-cols-2 sm:grid-cols-5 gap-3 disabled:opacity-50">
              <NumField label={t("settings.resilience.window")} value={draft.slidingWindowSize} onChange={num("slidingWindowSize")} />
              <NumField label={t("settings.resilience.minCalls")} value={draft.minimumNumberOfCalls} onChange={num("minimumNumberOfCalls")} />
              <NumField label={t("settings.resilience.threshold")} value={draft.failureRateThresholdPercent} onChange={num("failureRateThresholdPercent")} />
              <NumField label={t("settings.resilience.openSeconds")} value={draft.waitDurationOpenSeconds} onChange={num("waitDurationOpenSeconds")} />
              <NumField label={t("settings.resilience.halfOpen")} value={draft.permittedCallsInHalfOpen} onChange={num("permittedCallsInHalfOpen")} />
            </fieldset>
            <label className="mt-3 flex items-start gap-2 text-sm text-text cursor-pointer">
              <input
                type="checkbox"
                className="mt-0.5 h-4 w-4"
                checked={draft.countOnlyServiceFailures}
                onChange={(e) => set("countOnlyServiceFailures", e.target.checked)}
              />
              <span>
                {t("settings.resilience.serviceOnly")}
                <span className="block text-xs text-text-muted">{t("settings.resilience.serviceOnlyHint")}</span>
              </span>
            </label>
          </Section>

          <Section
            title={t("settings.resilience.retry")}
            hint={t("settings.resilience.retryHint")}
            toggle={{ checked: draft.retryEnabled, onChange: (v) => set("retryEnabled", v), label: t("settings.resilience.enabled") }}
          >
            <fieldset disabled={!draft.retryEnabled} className="grid grid-cols-2 sm:grid-cols-4 gap-3 disabled:opacity-50">
              <NumField label={t("settings.resilience.attempts")} value={draft.retryMaxAttempts} onChange={num("retryMaxAttempts")} />
              <NumField label={t("settings.resilience.waitMs")} value={draft.retryWaitMs} step={100} onChange={num("retryWaitMs")} />
            </fieldset>
          </Section>

          {q.data?.updatedAt && (
            <p className="text-xs text-text-dim">
              {t("settings.resilience.updatedBy", { who: q.data.updatedBy ?? "—", when: formatDate(q.data.updatedAt) })}
            </p>
          )}
        </CardBody>
      </Card>
    </div>
  );
}

function Section({
  title, hint, toggle, children,
}: {
  title: string; hint: string;
  toggle?: { checked: boolean; onChange: (v: boolean) => void; label: string };
  children: React.ReactNode;
}) {
  return (
    <div className="rounded-lg border border-border/10 bg-bg-elevated/30 p-4">
      <div className="flex items-start justify-between gap-3 mb-3">
        <div>
          <div className="text-sm font-medium text-text">{title}</div>
          <div className="text-xs text-text-muted">{hint}</div>
        </div>
        {toggle && (
          <button
            type="button"
            onClick={() => toggle.onChange(!toggle.checked)}
            className={cn(
              "shrink-0 rounded-full px-3 py-1 text-xs font-medium border transition-colors",
              toggle.checked
                ? "border-accent-emerald/50 bg-accent-emerald/15 text-accent-emerald"
                : "border-border/20 bg-bg-elevated/40 text-text-muted",
            )}
          >
            {toggle.label}: {toggle.checked ? "ON" : "OFF"}
          </button>
        )}
      </div>
      {children}
    </div>
  );
}

function NumField({
  label, value, onChange, step,
}: { label: string; value: number; onChange: (e: React.ChangeEvent<HTMLInputElement>) => void; step?: number }) {
  return (
    <div>
      <Label>{label}</Label>
      <Input type="number" value={value} step={step ?? 1} min={0} onChange={onChange} dir="ltr" />
    </div>
  );
}
