import { useEffect, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";
import { Gauge, Pencil, Save, X } from "lucide-react";
import { setMinNfiq2 } from "@/api/tenants";
import { useAuth } from "@/lib/auth";
import { canWrite } from "@/lib/access";
import type { Tenant } from "@/types/api";
import { Card, CardBody, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Badge } from "@/components/ui/Badge";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";

/**
 * Bank-specific fingerprint-quality threshold (minimum NFIQ 2 score). Empty =
 * the platform default from Settings → Fingerprint image applies.
 */
export function TenantQualityCard({ tenant }: { tenant: Tenant }) {
  const { t } = useTranslation();
  const qc = useQueryClient();
  const writable = canWrite(useAuth((s) => s.role));

  const [editing, setEditing] = useState(false);
  const [useOwn, setUseOwn] = useState(tenant.minNfiq2 != null);
  const [value, setValue] = useState<number>(tenant.minNfiq2 ?? 40);

  useEffect(() => {
    if (!editing) {
      setUseOwn(tenant.minNfiq2 != null);
      setValue(tenant.minNfiq2 ?? 40);
    }
  }, [tenant.minNfiq2, editing]);

  const saveMut = useMutation({
    mutationFn: () => setMinNfiq2(tenant.id, useOwn ? value : null),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["tenant", tenant.id] });
      qc.invalidateQueries({ queryKey: ["tenants"] });
      setEditing(false);
      toast.success(t("tenantQuality.saved"));
    },
    onError: (e: any) => toast.error(e?.response?.data?.message ?? t("tenantQuality.error")),
  });

  const own = tenant.minNfiq2 != null;
  const valid = !useOwn || (Number.isInteger(value) && value >= 0 && value <= 100);

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center gap-2">
          <Gauge className="h-4 w-4 text-accent-cyan" />
          <CardTitle>{t("tenantQuality.title")}</CardTitle>
          <Badge tone={own ? "amber" : "emerald"}>
            {own ? t("tenantQuality.own", { min: tenant.minNfiq2 }) : t("tenantQuality.default")}
          </Badge>
        </div>
        {writable && !editing && (
          <Button variant="ghost" size="sm" leftIcon={<Pencil className="h-3.5 w-3.5" />} onClick={() => setEditing(true)}>
            {t("tenantQuality.edit")}
          </Button>
        )}
      </CardHeader>
      <CardBody className="space-y-4">
        <p className="text-xs text-text-muted">{t("tenantQuality.subtitle")}</p>
        {!editing ? (
          <p className="text-sm text-text">
            {own ? t("tenantQuality.ownHint", { min: tenant.minNfiq2 }) : t("tenantQuality.defaultHint")}
          </p>
        ) : (
          <div className="space-y-4">
            <label className="flex items-center gap-2 text-sm text-text cursor-pointer">
              <input
                type="checkbox"
                className="h-4 w-4 accent-[var(--accent-cyan,#22d3ee)]"
                checked={useOwn}
                onChange={(e) => setUseOwn(e.target.checked)}
              />
              {t("tenantQuality.useOwn")}
            </label>
            {useOwn && (
              <div className="max-w-xs">
                <Label htmlFor="min-nfiq2">{t("tenantQuality.minLabel")}</Label>
                <Input
                  id="min-nfiq2"
                  type="number"
                  min={0}
                  max={100}
                  value={value}
                  dir="ltr"
                  onChange={(e) => setValue(Number(e.target.value))}
                />
                <p className="mt-1 text-xs text-text-dim">{t("tenantQuality.minHint")}</p>
              </div>
            )}
            <div className="flex items-center justify-end gap-2">
              <Button variant="ghost" size="sm" leftIcon={<X className="h-3.5 w-3.5" />} onClick={() => setEditing(false)}>
                {t("common.cancel")}
              </Button>
              <Button
                size="sm"
                leftIcon={<Save className="h-3.5 w-3.5" />}
                loading={saveMut.isPending}
                disabled={!valid}
                onClick={() => saveMut.mutate()}
              >
                {t("tenantQuality.save")}
              </Button>
            </div>
          </div>
        )}
      </CardBody>
    </Card>
  );
}
