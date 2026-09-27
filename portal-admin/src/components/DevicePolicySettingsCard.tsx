import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";
import { ScanLine, Save, RotateCcw } from "lucide-react";
import { getDevicePolicy, updateDevicePolicy } from "@/api/settings";
import type { DevicePolicySettings } from "@/types/api";
import { Card, CardBody, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Badge } from "@/components/ui/Badge";
import { PageLoader } from "@/components/ui/Spinner";
import { formatDate } from "@/lib/format";
import { cn } from "@/lib/cn";

/** Policy for the `deviceId` (scanner serial) banks send with each verification. */
export function DevicePolicySettingsCard() {
  const { t } = useTranslation();
  const qc = useQueryClient();
  const q = useQuery({ queryKey: ["settings", "device-policy"], queryFn: getDevicePolicy });
  const [draft, setDraft] = useState<DevicePolicySettings | null>(null);
  useEffect(() => { if (q.data) setDraft(q.data.value); }, [q.data]);

  const saveMut = useMutation({
    mutationFn: (v: DevicePolicySettings) => updateDevicePolicy(v),
    onSuccess: (data) => {
      qc.setQueryData(["settings", "device-policy"], data);
      toast.success(t("settings.devices.saved"));
    },
    onError: (e: any) => toast.error(e?.response?.data?.message ?? t("settings.devices.error")),
  });

  if (q.isLoading || !draft) return <PageLoader />;
  const dirty = JSON.stringify(draft) !== JSON.stringify(q.data?.value);
  const strict = draft.mode === "REGISTERED_ONLY";

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center gap-2">
          <ScanLine className="h-4 w-4 text-accent-cyan" />
          <CardTitle>{t("settings.devices.title")}</CardTitle>
          <Badge tone={strict ? "rose" : "amber"}>
            {strict ? t("settings.devices.modeRegistered") : t("settings.devices.modeAll")}
          </Badge>
        </div>
        <div className="flex items-center gap-1">
          <Button variant="ghost" size="sm" leftIcon={<RotateCcw className="h-3.5 w-3.5" />} disabled={!dirty}
            onClick={() => q.data && setDraft(q.data.value)}>
            {t("common.cancel")}
          </Button>
          <Button size="sm" leftIcon={<Save className="h-3.5 w-3.5" />} loading={saveMut.isPending} disabled={!dirty}
            onClick={() => saveMut.mutate(draft)}>
            {t("settings.devices.save")}
          </Button>
        </div>
      </CardHeader>
      <CardBody className="space-y-6">
        <p className="text-xs text-text-muted">{t("settings.devices.intro")}</p>

        <label className="flex items-center gap-2 text-sm text-text cursor-pointer">
          <input type="checkbox" className="h-4 w-4 accent-[var(--accent-cyan,#22d3ee)]"
            checked={draft.deviceIdRequired} onChange={(e) => setDraft({ ...draft, deviceIdRequired: e.target.checked })} />
          {t("settings.devices.required")}
        </label>
        <p className="-mt-4 text-xs text-text-dim">{t("settings.devices.requiredHint")}</p>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
          <Option active={!strict} title={t("settings.devices.modeAll")} hint={t("settings.devices.modeAllHint")}
            onClick={() => setDraft({ ...draft, mode: "ALLOW_ALL" })} />
          <Option active={strict} title={t("settings.devices.modeRegistered")} hint={t("settings.devices.modeRegisteredHint")}
            onClick={() => setDraft({ ...draft, mode: "REGISTERED_ONLY" })} />
        </div>

        {q.data?.updatedAt && (
          <p className="text-xs text-text-dim">
            {t("settings.image.updatedBy", { who: q.data.updatedBy ?? "—", when: formatDate(q.data.updatedAt) })}
          </p>
        )}
      </CardBody>
    </Card>
  );
}

function Option({ active, title, hint, onClick }: { active: boolean; title: string; hint: string; onClick: () => void }) {
  return (
    <button type="button" onClick={onClick}
      className={cn("text-start rounded-lg border p-3 transition-colors",
        active ? "border-accent-cyan/60 bg-accent-cyan/10" : "border-border/15 bg-bg-elevated/40 hover:bg-bg-hover/50")}>
      <div className="text-sm font-medium text-text">{title}</div>
      <div className="text-xs text-text-muted mt-0.5">{hint}</div>
    </button>
  );
}
