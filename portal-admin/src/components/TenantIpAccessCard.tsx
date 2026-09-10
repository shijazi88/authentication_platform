import { useEffect, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";
import { Globe, Pencil, Save, X } from "lucide-react";
import { setIpPolicy } from "@/api/tenants";
import { useAuth } from "@/lib/auth";
import { canWrite } from "@/lib/access";
import type { IpPolicy, Tenant } from "@/types/api";
import { Card, CardBody, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Badge } from "@/components/ui/Badge";
import { Label } from "@/components/ui/Label";
import { cn } from "@/lib/cn";

/**
 * Admin approval of the source IPs a client may call the bank API from:
 * either every IP ("ALL") or a specific allowlist ("RESTRICTED").
 */
export function TenantIpAccessCard({ tenant }: { tenant: Tenant }) {
  const { t } = useTranslation();
  const qc = useQueryClient();
  const writable = canWrite(useAuth((s) => s.role));

  const [editing, setEditing] = useState(false);
  const [mode, setMode] = useState<IpPolicy>(tenant.ipPolicy ?? "ALL");
  const [text, setText] = useState((tenant.ipAllowlist ?? []).join("\n"));

  // Re-sync the draft whenever the server copy changes (after save / refetch).
  useEffect(() => {
    if (!editing) {
      setMode(tenant.ipPolicy ?? "ALL");
      setText((tenant.ipAllowlist ?? []).join("\n"));
    }
  }, [tenant.ipPolicy, tenant.ipAllowlist, editing]);

  const entries = text
    .split(/[\n,\s]+/)
    .map((s) => s.trim())
    .filter(Boolean);

  const saveMut = useMutation({
    mutationFn: () => setIpPolicy(tenant.id, mode, mode === "RESTRICTED" ? entries : []),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["tenant", tenant.id] });
      qc.invalidateQueries({ queryKey: ["tenants"] });
      setEditing(false);
      toast.success(t("tenantIp.saved"));
    },
    onError: (e: any) => toast.error(e?.response?.data?.message ?? t("tenantIp.error")),
  });

  const restricted = tenant.ipPolicy === "RESTRICTED";

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center gap-2">
          <Globe className="h-4 w-4 text-accent-cyan" />
          <CardTitle>{t("tenantIp.title")}</CardTitle>
          <Badge tone={restricted ? "amber" : "emerald"}>
            {restricted ? t("tenantIp.modeRestricted") : t("tenantIp.modeAll")}
          </Badge>
        </div>
        {writable && !editing && (
          <Button
            variant="ghost"
            size="sm"
            leftIcon={<Pencil className="h-3.5 w-3.5" />}
            onClick={() => setEditing(true)}
          >
            {t("tenantIp.edit")}
          </Button>
        )}
      </CardHeader>
      <CardBody className="space-y-4">
        <p className="text-xs text-text-muted">{t("tenantIp.subtitle")}</p>

        {!editing ? (
          restricted ? (
            <div>
              <div className="text-xs text-text-dim mb-1.5">
                {t("tenantIp.count", { count: tenant.ipAllowlist?.length ?? 0 })}
              </div>
              <div className="flex flex-wrap gap-1.5" dir="ltr">
                {(tenant.ipAllowlist ?? []).map((ip) => (
                  <code
                    key={ip}
                    className="font-mono text-xs bg-bg-elevated/60 border border-border/10 rounded-md px-2 py-1"
                  >
                    {ip}
                  </code>
                ))}
                {!tenant.ipAllowlist?.length && (
                  <span className="text-xs text-text-muted">{t("tenantIp.noneApproved")}</span>
                )}
              </div>
            </div>
          ) : (
            <p className="text-sm text-text">{t("tenantIp.modeAllHint")}</p>
          )
        ) : (
          <div className="space-y-4">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
              <ModeOption
                active={mode === "ALL"}
                title={t("tenantIp.modeAll")}
                hint={t("tenantIp.modeAllHint")}
                onClick={() => setMode("ALL")}
              />
              <ModeOption
                active={mode === "RESTRICTED"}
                title={t("tenantIp.modeRestricted")}
                hint={t("tenantIp.modeRestrictedHint")}
                onClick={() => setMode("RESTRICTED")}
              />
            </div>

            {mode === "RESTRICTED" && (
              <div>
                <Label htmlFor="ip-list">{t("tenantIp.listLabel")}</Label>
                <textarea
                  id="ip-list"
                  dir="ltr"
                  rows={6}
                  className="w-full font-mono text-xs rounded-lg bg-bg-elevated/60 border border-border/15 px-3 py-2 text-text placeholder:text-text-dim focus:outline-none focus:ring-2 focus:ring-accent-cyan/40"
                  placeholder={t("tenantIp.listPlaceholder")}
                  value={text}
                  onChange={(e) => setText(e.target.value)}
                />
                <p className="mt-1 text-xs text-text-dim">{t("tenantIp.listHint")}</p>
                {entries.length === 0 && (
                  <p className="mt-1 text-xs text-accent-amber">{t("tenantIp.emptyList")}</p>
                )}
              </div>
            )}

            <div className="flex items-center justify-end gap-2">
              <Button
                variant="ghost"
                size="sm"
                leftIcon={<X className="h-3.5 w-3.5" />}
                onClick={() => setEditing(false)}
              >
                {t("common.cancel")}
              </Button>
              <Button
                size="sm"
                leftIcon={<Save className="h-3.5 w-3.5" />}
                loading={saveMut.isPending}
                disabled={mode === "RESTRICTED" && entries.length === 0}
                onClick={() => saveMut.mutate()}
              >
                {t("tenantIp.save")}
              </Button>
            </div>
          </div>
        )}
      </CardBody>
    </Card>
  );
}

function ModeOption({
  active,
  title,
  hint,
  onClick,
}: {
  active: boolean;
  title: string;
  hint: string;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        "text-start rounded-lg border p-3 transition-colors",
        active
          ? "border-accent-cyan/60 bg-accent-cyan/10"
          : "border-border/15 bg-bg-elevated/40 hover:bg-bg-hover/50",
      )}
    >
      <div className="text-sm font-medium text-text">{title}</div>
      <div className="text-xs text-text-muted mt-0.5">{hint}</div>
    </button>
  );
}
