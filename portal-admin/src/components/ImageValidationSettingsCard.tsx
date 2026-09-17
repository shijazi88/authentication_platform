import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";
import { Fingerprint, Save, RotateCcw } from "lucide-react";
import { getImageValidationSettings, getQualityServiceStatus, updateImageValidationSettings } from "@/api/settings";
import type { ImageFormat, ImageValidationSettings } from "@/types/api";
import { Card, CardBody, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Badge } from "@/components/ui/Badge";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { PageLoader } from "@/components/ui/Spinner";
import { formatDate } from "@/lib/format";
import { cn } from "@/lib/cn";

const FORMATS: ImageFormat[] = ["WSQ", "PNG"];

/**
 * Runtime rules applied to every fingerprint image the API receives, before
 * any charge or backend call. Saved server-side; effective within seconds.
 */
export function ImageValidationSettingsCard() {
  const { t } = useTranslation();
  const qc = useQueryClient();
  const q = useQuery({ queryKey: ["settings", "image-validation"], queryFn: getImageValidationSettings });
  const svc = useQuery({
    queryKey: ["settings", "quality-service"],
    queryFn: getQualityServiceStatus,
    refetchInterval: 30_000,
    retry: false,
  });

  const [draft, setDraft] = useState<ImageValidationSettings | null>(null);
  useEffect(() => {
    if (q.data) setDraft(q.data.value);
  }, [q.data]);

  const saveMut = useMutation({
    mutationFn: (v: ImageValidationSettings) => updateImageValidationSettings(v),
    onSuccess: (data) => {
      qc.setQueryData(["settings", "image-validation"], data);
      toast.success(t("settings.image.saved"));
    },
    onError: (e: any) => toast.error(e?.response?.data?.message ?? t("settings.image.error")),
  });

  if (q.isLoading || !draft) return <PageLoader />;

  const set = <K extends keyof ImageValidationSettings>(k: K, v: ImageValidationSettings[K]) =>
    setDraft({ ...draft, [k]: v });
  const num = (k: keyof ImageValidationSettings) => (e: React.ChangeEvent<HTMLInputElement>) =>
    set(k, Number(e.target.value) as never);
  const dirty = JSON.stringify(draft) !== JSON.stringify(q.data?.value);
  const enforcing = draft.enabled && draft.mode === "ENFORCE";

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center gap-2">
          <Fingerprint className="h-4 w-4 text-accent-cyan" />
          <CardTitle>{t("settings.image.title")}</CardTitle>
          <Badge tone={!draft.enabled ? "neutral" : enforcing ? "rose" : "amber"}>
            {!draft.enabled
              ? t("settings.image.stateOff")
              : enforcing
                ? t("settings.image.stateEnforce")
                : t("settings.image.stateWarn")}
          </Badge>
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
            {t("settings.image.save")}
          </Button>
        </div>
      </CardHeader>
      <CardBody className="space-y-6">
        <p className="text-xs text-text-muted">{t("settings.image.intro")}</p>

        {/* Master switch + mode */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
          <Option
            active={!draft.enabled}
            title={t("settings.image.stateOff")}
            hint={t("settings.image.offHint")}
            onClick={() => set("enabled", false)}
          />
          <Option
            active={draft.enabled && draft.mode === "WARN"}
            title={t("settings.image.stateWarn")}
            hint={t("settings.image.warnHint")}
            onClick={() => setDraft({ ...draft, enabled: true, mode: "WARN" })}
          />
          <Option
            active={enforcing}
            title={t("settings.image.stateEnforce")}
            hint={t("settings.image.enforceHint")}
            onClick={() => setDraft({ ...draft, enabled: true, mode: "ENFORCE" })}
          />
        </div>

        <fieldset disabled={!draft.enabled} className="space-y-6 disabled:opacity-50">
          {/* Formats */}
          <Section title={t("settings.image.formats")} hint={t("settings.image.formatsHint")}>
            <div className="flex gap-2">
              {FORMATS.map((f) => {
                const on = draft.allowedFormats.includes(f);
                return (
                  <button
                    key={f}
                    type="button"
                    onClick={() =>
                      set(
                        "allowedFormats",
                        on ? draft.allowedFormats.filter((x) => x !== f) : [...draft.allowedFormats, f],
                      )
                    }
                    className={cn(
                      "rounded-lg border px-4 py-2 text-sm font-mono transition-colors",
                      on
                        ? "border-accent-cyan/60 bg-accent-cyan/10 text-text"
                        : "border-border/15 bg-bg-elevated/40 text-text-muted hover:bg-bg-hover/50",
                    )}
                  >
                    {f}
                  </button>
                );
              })}
            </div>
          </Section>

          {/* Dimensions */}
          <Section title={t("settings.image.dimensions")} hint={t("settings.image.dimensionsHint")}>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
              <NumField label={t("settings.image.minWidth")} value={draft.minWidth} onChange={num("minWidth")} />
              <NumField label={t("settings.image.minHeight")} value={draft.minHeight} onChange={num("minHeight")} />
              <NumField label={t("settings.image.maxWidth")} value={draft.maxWidth} onChange={num("maxWidth")} />
              <NumField label={t("settings.image.maxHeight")} value={draft.maxHeight} onChange={num("maxHeight")} />
            </div>
          </Section>

          {/* Resolution */}
          <Section title={t("settings.image.resolution")} hint={t("settings.image.resolutionHint")}>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 items-end">
              <NumField label={t("settings.image.ppiMin")} value={draft.ppiMin} onChange={num("ppiMin")} />
              <NumField label={t("settings.image.ppiMax")} value={draft.ppiMax} onChange={num("ppiMax")} />
              <Check
                label={t("settings.image.requirePpi")}
                checked={draft.requirePpi}
                onChange={(v) => set("requirePpi", v)}
              />
            </div>
          </Section>

          {/* Size + encoding */}
          <Section title={t("settings.image.sizeEncoding")} hint={t("settings.image.sizeEncodingHint")}>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 items-end">
              <NumField
                label={t("settings.image.maxKb")}
                value={Math.round(draft.maxImageBytes / 1024)}
                onChange={(e) => set("maxImageBytes", Number(e.target.value) * 1024)}
              />
              <NumField
                label={t("settings.image.wsqRatio")}
                value={draft.wsqMaxCompressionRatio}
                step={0.5}
                onChange={num("wsqMaxCompressionRatio")}
              />
              <Check
                label={t("settings.image.gray8")}
                checked={draft.requireGrayscale8Bit}
                onChange={(v) => set("requireGrayscale8Bit", v)}
              />
            </div>
          </Section>

          {/* Blank detection */}
          <Section title={t("settings.image.blank")} hint={t("settings.image.blankHint")}>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 items-end">
              <Check
                label={t("settings.image.checkBlank")}
                checked={draft.checkBlank}
                onChange={(v) => set("checkBlank", v)}
              />
              <NumField
                label={t("settings.image.minStdDev")}
                value={draft.minStdDev}
                step={0.5}
                onChange={num("minStdDev")}
              />
            </div>
          </Section>

          {/* NFIQ 2 quality score */}
          <Section title={t("settings.image.nfiq2")} hint={t("settings.image.nfiq2Hint")}>
            <div className="mb-3 flex flex-wrap items-center gap-2 text-xs">
              <span className="text-text-muted">{t("settings.image.serviceStatus")}</span>
              {svc.isLoading ? (
                <Badge tone="neutral">…</Badge>
              ) : svc.data?.available ? (
                <Badge tone="emerald">
                  {t("settings.image.serviceUp", { version: svc.data.version ?? "", ms: svc.data.latencyMs ?? 0 })}
                </Badge>
              ) : (
                <Badge tone="rose">{t("settings.image.serviceDown")}</Badge>
              )}
              {svc.data?.lastFailureAt && (
                <span className="text-text-dim">
                  {t("settings.image.lastFailure", { when: formatDate(svc.data.lastFailureAt) })}
                </span>
              )}
            </div>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 items-end">
              <Check
                label={t("settings.image.checkNfiq2")}
                checked={draft.checkNfiq2 ?? false}
                onChange={(v) => set("checkNfiq2", v)}
              />
              <NumField label={t("settings.image.minNfiq2")} value={draft.minNfiq2 ?? 40} onChange={num("minNfiq2")} />
              <NumField
                label={t("settings.image.nfiq2Timeout")}
                value={draft.nfiq2TimeoutMs ?? 5000}
                step={500}
                onChange={num("nfiq2TimeoutMs")}
              />
              <Check
                label={t("settings.image.nfiq2FailOpen")}
                checked={draft.nfiq2FailOpen ?? true}
                onChange={(v) => set("nfiq2FailOpen", v)}
              />
            </div>
            <div className="mt-3 grid grid-cols-1 sm:grid-cols-2 gap-2">
              <Option
                active={(draft.nfiq2Mode ?? "ENFORCE") === "ENFORCE"}
                title={t("settings.image.nfiq2Reject")}
                hint={t("settings.image.nfiq2RejectHint")}
                onClick={() => set("nfiq2Mode", "ENFORCE")}
              />
              <Option
                active={draft.nfiq2Mode === "WARN"}
                title={t("settings.image.nfiq2WarnOnly")}
                hint={t("settings.image.nfiq2WarnOnlyHint")}
                onClick={() => set("nfiq2Mode", "WARN")}
              />
            </div>
          </Section>

          {/* Fingerprint coverage */}
          <Section title={t("settings.image.coverage")} hint={t("settings.image.coverageHint")}>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 items-end">
              <Check
                label={t("settings.image.checkCoverage")}
                checked={draft.checkCoverage ?? false}
                onChange={(v) => set("checkCoverage", v)}
              />
              <NumField
                label={t("settings.image.minCoveragePct")}
                value={Math.round((draft.minForegroundRatio ?? 0) * 100)}
                onChange={(e) => set("minForegroundRatio", Number(e.target.value) / 100)}
              />
            </div>
          </Section>
          {/* What the bank receives */}
          <Section title={t("settings.image.messages")} hint={t("settings.image.messagesHint")}>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              <div>
                <Label htmlFor="quality-msg">{t("settings.image.qualityMessage")}</Label>
                <textarea
                  id="quality-msg"
                  rows={3}
                  maxLength={300}
                  className="w-full text-sm rounded-lg bg-bg-elevated/60 border border-border/15 px-3 py-2 text-text focus:outline-none focus:ring-2 focus:ring-accent-cyan/40"
                  value={draft.qualityMessage ?? ""}
                  onChange={(e) => set("qualityMessage", e.target.value)}
                />
                <p className="mt-1 text-xs text-text-dim">{t("settings.image.qualityMessageHint")}</p>
              </div>
              <div>
                <Label htmlFor="format-msg">{t("settings.image.formatMessage")}</Label>
                <textarea
                  id="format-msg"
                  rows={3}
                  maxLength={300}
                  className="w-full text-sm rounded-lg bg-bg-elevated/60 border border-border/15 px-3 py-2 text-text focus:outline-none focus:ring-2 focus:ring-accent-cyan/40"
                  value={draft.formatMessage ?? ""}
                  onChange={(e) => set("formatMessage", e.target.value)}
                />
                <p className="mt-1 text-xs text-text-dim">{t("settings.image.formatMessageHint")}</p>
              </div>
            </div>
            <div className="mt-3">
              <Check
                label={t("settings.image.returnScore")}
                checked={draft.returnScoreToBank ?? true}
                onChange={(v) => set("returnScoreToBank", v)}
              />
              <p className="text-xs text-text-dim">{t("settings.image.returnScoreHint")}</p>
            </div>
          </Section>
        </fieldset>

        {q.data?.updatedAt && (
          <p className="text-xs text-text-dim">
            {t("settings.image.updatedBy", { who: q.data.updatedBy ?? "—", when: formatDate(q.data.updatedAt) })}
          </p>
        )}
      </CardBody>
    </Card>
  );
}

function Section({ title, hint, children }: { title: string; hint: string; children: React.ReactNode }) {
  return (
    <div>
      <div className="text-sm font-medium text-text">{title}</div>
      <div className="text-xs text-text-muted mb-2">{hint}</div>
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

function Check({ label, checked, onChange }: { label: string; checked: boolean; onChange: (v: boolean) => void }) {
  return (
    <label className="flex items-center gap-2 text-sm text-text cursor-pointer h-9">
      <input
        type="checkbox"
        className="h-4 w-4 accent-[var(--accent-cyan,#22d3ee)]"
        checked={checked}
        onChange={(e) => onChange(e.target.checked)}
      />
      {label}
    </label>
  );
}

function Option({ active, title, hint, onClick }: { active: boolean; title: string; hint: string; onClick: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        "text-start rounded-lg border p-3 transition-colors",
        active ? "border-accent-cyan/60 bg-accent-cyan/10" : "border-border/15 bg-bg-elevated/40 hover:bg-bg-hover/50",
      )}
    >
      <div className="text-sm font-medium text-text">{title}</div>
      <div className="text-xs text-text-muted mt-0.5">{hint}</div>
    </button>
  );
}
