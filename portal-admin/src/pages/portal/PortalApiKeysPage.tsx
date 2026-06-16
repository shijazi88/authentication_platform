import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";
import { KeyRound, Plus, ShieldAlert, Ban } from "lucide-react";
import {
  listCredentials,
  createCredential,
  revokeCredential,
  type IssuedCredential,
} from "@/api/tenant";
import { Card, CardBody } from "@/components/ui/Card";
import { Table, TBody, THead, Th, Td, Tr } from "@/components/ui/Table";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Dialog } from "@/components/ui/Dialog";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { CopyButton } from "@/components/ui/CopyButton";
import { PageLoader } from "@/components/ui/Spinner";
import { formatDate } from "@/lib/format";

export function PortalApiKeysPage() {
  const { t } = useTranslation();
  const qc = useQueryClient();
  const q = useQuery({ queryKey: ["t-credentials"], queryFn: listCredentials });

  const [createOpen, setCreateOpen] = useState(false);
  const [label, setLabel] = useState("");
  const [ipAllowlist, setIpAllowlist] = useState("");
  const [issued, setIssued] = useState<IssuedCredential | null>(null);

  const invalidate = () => qc.invalidateQueries({ queryKey: ["t-credentials"] });

  const createMut = useMutation({
    mutationFn: () => createCredential(label || undefined, ipAllowlist || undefined),
    onSuccess: (data) => {
      invalidate();
      setCreateOpen(false);
      setLabel("");
      setIpAllowlist("");
      setIssued(data); // show the secret once
    },
    onError: (e: any) => toast.error(e?.response?.data?.message ?? t("portal.apiKeys.error")),
  });

  const revokeMut = useMutation({
    mutationFn: (id: string) => revokeCredential(id),
    onSuccess: () => {
      invalidate();
      toast.success(t("portal.apiKeys.revoked"));
    },
    onError: (e: any) => toast.error(e?.response?.data?.message ?? t("portal.apiKeys.error")),
  });

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">{t("portal.apiKeys.title")}</h1>
        <Button leftIcon={<Plus className="h-4 w-4" />} onClick={() => setCreateOpen(true)}>
          {t("portal.apiKeys.generate")}
        </Button>
      </div>

      <p className="text-sm text-text-muted">{t("portal.apiKeys.intro")}</p>

      <Card>
        {q.isLoading ? (
          <PageLoader />
        ) : q.data?.length ? (
          <Table>
            <THead>
              <Tr>
                <Th>{t("portal.apiKeys.clientId")}</Th>
                <Th>{t("portal.apiKeys.label")}</Th>
                <Th>{t("common.status")}</Th>
                <Th>{t("portal.apiKeys.lastUsed")}</Th>
                <Th>{t("common.actions")}</Th>
              </Tr>
            </THead>
            <TBody>
              {q.data.map((c) => (
                <Tr key={c.id}>
                  <Td>
                    <div className="flex items-center gap-2">
                      <span className="font-mono text-xs">{c.clientId}</span>
                      <CopyButton value={c.clientId} />
                    </div>
                  </Td>
                  <Td className="text-sm">{c.label ?? "—"}</Td>
                  <Td>
                    <Badge tone={c.active ? "emerald" : "neutral"}>
                      {c.active ? t("common.active") : t("portal.apiKeys.revokedBadge")}
                    </Badge>
                  </Td>
                  <Td className="text-xs text-text-muted">
                    {c.lastUsedAt ? formatDate(c.lastUsedAt) : t("portal.apiKeys.never")}
                  </Td>
                  <Td>
                    {c.active && (
                      <Button
                        variant="ghost"
                        size="sm"
                        leftIcon={<Ban className="h-3.5 w-3.5" />}
                        loading={revokeMut.isPending && revokeMut.variables === c.id}
                        onClick={() => revokeMut.mutate(c.id)}
                      >
                        {t("portal.apiKeys.revoke")}
                      </Button>
                    )}
                  </Td>
                </Tr>
              ))}
            </TBody>
          </Table>
        ) : (
          <CardBody className="px-6 py-10 text-center text-sm text-text-muted">
            <KeyRound className="h-6 w-6 mx-auto mb-2 text-text-dim" />
            {t("portal.apiKeys.empty")}
          </CardBody>
        )}
      </Card>

      {/* Generate dialog */}
      <Dialog
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        title={t("portal.apiKeys.generate")}
        size="lg"
        footer={
          <>
            <Button variant="ghost" onClick={() => setCreateOpen(false)}>
              {t("common.cancel")}
            </Button>
            <Button loading={createMut.isPending} onClick={() => createMut.mutate()}>
              {t("portal.apiKeys.generate")}
            </Button>
          </>
        }
      >
        <div className="space-y-4">
          <div>
            <Label htmlFor="label">{t("portal.apiKeys.label")}</Label>
            <Input
              id="label"
              placeholder={t("portal.apiKeys.labelPlaceholder")}
              value={label}
              onChange={(e) => setLabel(e.target.value)}
            />
          </div>
          <div>
            <Label htmlFor="ip">{t("portal.apiKeys.ipAllowlist")}</Label>
            <Input
              id="ip"
              placeholder="e.g. 203.0.113.4, 203.0.113.0/24"
              value={ipAllowlist}
              onChange={(e) => setIpAllowlist(e.target.value)}
            />
            <p className="mt-1 text-xs text-text-dim">{t("portal.apiKeys.ipHint")}</p>
          </div>
        </div>
      </Dialog>

      {/* Secret-shown-once dialog */}
      <Dialog
        open={issued !== null}
        onClose={() => setIssued(null)}
        title={t("portal.apiKeys.issuedTitle")}
        size="lg"
        footer={
          <Button onClick={() => setIssued(null)}>{t("common.done")}</Button>
        }
      >
        <div className="space-y-4">
          <div className="flex items-start gap-2 rounded-lg border border-accent-amber/30 bg-accent-amber/[0.06] p-3">
            <ShieldAlert className="h-4 w-4 text-accent-amber shrink-0 mt-0.5" />
            <p className="text-xs text-text-muted">{t("portal.apiKeys.issuedWarning")}</p>
          </div>
          <div>
            <Label>{t("portal.apiKeys.clientId")}</Label>
            <div className="flex items-center gap-2 mt-1">
              <code className="flex-1 font-mono text-xs bg-bg-elevated/60 rounded-lg px-3 py-2 break-all">
                {issued?.clientId}
              </code>
              <CopyButton value={issued?.clientId ?? ""} />
            </div>
          </div>
          <div>
            <Label>{t("portal.apiKeys.clientSecret")}</Label>
            <div className="flex items-center gap-2 mt-1">
              <code className="flex-1 font-mono text-xs bg-bg-elevated/60 rounded-lg px-3 py-2 break-all">
                {issued?.clientSecret}
              </code>
              <CopyButton value={issued?.clientSecret ?? ""} />
            </div>
          </div>
        </div>
      </Dialog>
    </div>
  );
}
