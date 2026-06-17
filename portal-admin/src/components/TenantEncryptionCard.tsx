import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";
import { ShieldCheck, Download, RefreshCw, Ban, Lock } from "lucide-react";
import {
  listEncryptionKeys,
  getActiveCertificate,
  rotateEncryptionKey,
  revokeEncryptionKey,
  setEncryptionPolicy,
} from "@/api/crypto";
import { useAuth } from "@/lib/auth";
import { canWrite } from "@/lib/access";
import type { EncryptionKey } from "@/types/api";
import { Card, CardBody, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Badge } from "@/components/ui/Badge";
import { CopyButton } from "@/components/ui/CopyButton";
import { formatDate } from "@/lib/format";

const STATUS_TONE: Record<string, "emerald" | "amber" | "neutral"> = {
  ACTIVE: "emerald",
  RETIRING: "amber",
  REVOKED: "neutral",
};

/** Admin management of a tenant's PII encryption keys + enforcement policy. */
export function TenantEncryptionCard({
  tenantId,
  requireEncryptedPii,
}: {
  tenantId: string;
  requireEncryptedPii: boolean;
}) {
  const { t } = useTranslation();
  const qc = useQueryClient();
  const writable = canWrite(useAuth((s) => s.role));

  const keysQ = useQuery({
    queryKey: ["tenant-enc-keys", tenantId],
    queryFn: () => listEncryptionKeys(tenantId),
  });

  const invalidate = () => {
    qc.invalidateQueries({ queryKey: ["tenant-enc-keys", tenantId] });
    qc.invalidateQueries({ queryKey: ["tenant", tenantId] });
  };
  const onErr = (e: any) => toast.error(e?.response?.data?.message ?? t("tenantCrypto.error"));

  const rotateMut = useMutation({
    mutationFn: () => rotateEncryptionKey(tenantId),
    onSuccess: () => { invalidate(); toast.success(t("tenantCrypto.rotated")); },
    onError: onErr,
  });
  const revokeMut = useMutation({
    mutationFn: (kid: string) => revokeEncryptionKey(tenantId, kid),
    onSuccess: () => { invalidate(); toast.success(t("tenantCrypto.revoked")); },
    onError: onErr,
  });
  const policyMut = useMutation({
    mutationFn: (required: boolean) => setEncryptionPolicy(tenantId, required),
    onSuccess: () => { invalidate(); toast.success(t("tenantCrypto.policyUpdated")); },
    onError: onErr,
  });

  async function downloadActive() {
    try {
      const cert = await getActiveCertificate(tenantId);
      const blob = new Blob([cert.certificatePem], { type: "application/x-pem-file" });
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `motabiq-${cert.kid}.pem`;
      a.click();
      URL.revokeObjectURL(url);
    } catch (e: any) {
      onErr(e);
    }
  }

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center gap-2">
          <ShieldCheck className="h-4 w-4 text-accent-emerald" />
          <CardTitle>{t("tenantCrypto.title")}</CardTitle>
        </div>
        {writable && (
          <div className="flex items-center gap-1">
            <Button variant="ghost" size="sm" leftIcon={<Download className="h-3.5 w-3.5" />} onClick={downloadActive}>
              {t("tenantCrypto.download")}
            </Button>
            <Button
              variant="ghost"
              size="sm"
              leftIcon={<RefreshCw className="h-3.5 w-3.5" />}
              loading={rotateMut.isPending}
              onClick={() => { if (confirm(t("tenantCrypto.rotateConfirm"))) rotateMut.mutate(); }}
            >
              {t("tenantCrypto.rotate")}
            </Button>
          </div>
        )}
      </CardHeader>
      <CardBody className="space-y-4">
        {/* Enforcement policy */}
        <div className="flex items-center justify-between rounded-lg bg-bg-elevated/40 border border-border/10 p-3">
          <div className="flex items-start gap-2">
            <Lock className="h-4 w-4 text-text-muted mt-0.5" />
            <div>
              <div className="text-sm font-medium">{t("tenantCrypto.enforceTitle")}</div>
              <div className="text-xs text-text-muted">{t("tenantCrypto.enforceHint")}</div>
            </div>
          </div>
          <Button
            variant={requireEncryptedPii ? "primary" : "outline"}
            size="sm"
            disabled={!writable}
            loading={policyMut.isPending}
            onClick={() => policyMut.mutate(!requireEncryptedPii)}
          >
            {requireEncryptedPii ? t("tenantCrypto.enforced") : t("tenantCrypto.enforceOff")}
          </Button>
        </div>

        {/* Keys */}
        {keysQ.data?.length ? (
          <div className="space-y-1.5">
            {keysQ.data.map((k: EncryptionKey) => (
              <div key={k.kid} className="flex items-center justify-between gap-2 text-xs">
                <div className="flex items-center gap-2 min-w-0">
                  <Badge tone={STATUS_TONE[k.status] ?? "neutral"}>
                    {t(`tenantCrypto.status.${k.status}`, k.status)}
                  </Badge>
                  <span className="font-mono truncate">{k.kid}</span>
                  <CopyButton value={k.kid} />
                </div>
                <div className="flex items-center gap-2 shrink-0 text-text-dim">
                  <span>{t("tenantCrypto.expires")}: {k.expiresAt ? formatDate(k.expiresAt) : "—"}</span>
                  {writable && k.status === "RETIRING" && (
                    <Button
                      variant="ghost"
                      size="sm"
                      leftIcon={<Ban className="h-3.5 w-3.5" />}
                      loading={revokeMut.isPending && revokeMut.variables === k.kid}
                      onClick={() => revokeMut.mutate(k.kid)}
                    >
                      {t("tenantCrypto.revoke")}
                    </Button>
                  )}
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="text-xs text-text-muted">{t("tenantCrypto.noKeys")}</div>
        )}
      </CardBody>
    </Card>
  );
}
