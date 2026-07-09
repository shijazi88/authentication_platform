import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";
import { KeyRound, Plus, ShieldAlert, Ban, ShieldCheck, Download, RefreshCw, BookOpen } from "lucide-react";
import {
  listCredentials,
  createCredential,
  revokeCredential,
  getCertificate,
  rotateCertificate,
  type IssuedCredential,
} from "@/api/tenant";
import { Card, CardBody, CardHeader, CardTitle } from "@/components/ui/Card";
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
  const certQ = useQuery({ queryKey: ["t-certificate"], queryFn: getCertificate });

  // API host mirrors the portal host: portal.<domain> → api.<domain>.
  const apiBase =
    typeof window !== "undefined"
      ? `https://${window.location.hostname.replace("portal", "api")}`
      : "https://api.motabiq.ai";

  const rotateMut = useMutation({
    mutationFn: () => rotateCertificate(),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["t-certificate"] });
      toast.success(t("portal.cert.rotated"));
    },
    onError: (e: any) => toast.error(e?.response?.data?.message ?? t("portal.apiKeys.error")),
  });

  function downloadCert() {
    const cert = certQ.data;
    if (!cert) return;
    const blob = new Blob([cert.certificatePem], { type: "application/x-pem-file" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `motabiq-${cert.kid}.pem`;
    a.click();
    URL.revokeObjectURL(url);
  }

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

      {/* Encryption certificate */}
      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <ShieldCheck className="h-4 w-4 text-accent-emerald" />
            <CardTitle>{t("portal.cert.title")}</CardTitle>
          </div>
          <div className="flex items-center gap-1">
            <Button
              variant="ghost"
              size="sm"
              leftIcon={<Download className="h-3.5 w-3.5" />}
              onClick={downloadCert}
              disabled={!certQ.data}
            >
              {t("portal.cert.download")}
            </Button>
            <Button
              variant="ghost"
              size="sm"
              leftIcon={<RefreshCw className="h-3.5 w-3.5" />}
              loading={rotateMut.isPending}
              onClick={() => {
                if (confirm(t("portal.cert.rotateConfirm"))) rotateMut.mutate();
              }}
            >
              {t("portal.cert.rotate")}
            </Button>
          </div>
        </CardHeader>
        <CardBody>
          <p className="text-xs text-text-muted mb-3">{t("portal.cert.intro")}</p>
          {certQ.data && (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-x-6 gap-y-2 text-sm">
              <Field label={t("portal.cert.kid")} value={certQ.data.kid} mono />
              <Field
                label={t("portal.cert.algorithm")}
                value={`${certQ.data.algorithm} · ${certQ.data.encryption}`}
              />
              <Field
                label={t("portal.cert.fingerprint")}
                value={certQ.data.fingerprintSha256}
                mono
              />
              <Field
                label={t("portal.cert.expires")}
                value={certQ.data.expiresAt ? formatDate(certQ.data.expiresAt) : "—"}
              />
            </div>
          )}
        </CardBody>
      </Card>

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

      {/* Integration guide */}
      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <BookOpen className="h-4 w-4 text-text-muted" />
            <CardTitle>{t("portal.guide.title")}</CardTitle>
          </div>
        </CardHeader>
        <CardBody className="space-y-5 text-sm">
          <p className="text-text-muted">{t("portal.guide.intro")}</p>

          <div>
            <div className="text-xs text-text-dim mb-1">{t("portal.guide.baseUrl")}</div>
            <div className="flex items-center gap-2">
              <code className="font-mono text-xs bg-bg-elevated/60 rounded-lg px-3 py-2">
                {apiBase}
              </code>
              <CopyButton value={apiBase} />
            </div>
          </div>

          <div>
            <div className="font-semibold mb-1">{t("portal.guide.step1Title")}</div>
            <p className="text-text-muted text-xs mb-2">{t("portal.guide.step1")}</p>
            <CodeBlock code={'Authorization: Basic base64(clientId + ":" + clientSecret)'} />
          </div>

          <div>
            <div className="font-semibold mb-1">{t("portal.guide.step2Title")}</div>
            <p className="text-text-muted text-xs mb-2">{t("portal.guide.step2")}</p>
            <CodeBlock
              code={`{
  "nationalNumber": "…",
  "biometrics": { "fingerPosition": 1, "image": "<wsqBase64>" }
}`}
            />
            <p className="text-text-dim text-xs mt-1.5">
              {t("portal.guide.step2Note", { kid: certQ.data?.kid ?? "…" })}
            </p>
          </div>

          <div>
            <div className="font-semibold mb-1">{t("portal.guide.step3Title")}</div>
            <p className="text-text-muted text-xs mb-2">{t("portal.guide.step3")}</p>
            <CodeBlock
              code={`curl -X POST "${apiBase}/api/v1/verify/identity" \\
  -u "CLIENT_ID:CLIENT_SECRET" \\
  -H "Content-Type: application/json" \\
  -d '{"encryptedPayload":"<JWE>"}'`}
            />
          </div>

          <div className="rounded-lg border border-accent-amber/30 bg-accent-amber/[0.06] p-3 text-xs text-text-muted space-y-1">
            <div>• {t("portal.guide.noteSecret")}</div>
            <div>• {t("portal.guide.noteIp")}</div>
            <div>• {t("portal.guide.noteCert")}</div>
          </div>
        </CardBody>
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

function Field({ label, value, mono }: { label: string; value: string; mono?: boolean }) {
  return (
    <div>
      <div className="text-xs text-text-dim">{label}</div>
      <div className={`${mono ? "font-mono text-xs" : "text-sm"} text-text break-all`}>{value}</div>
    </div>
  );
}

function CodeBlock({ code }: { code: string }) {
  return (
    <div className="relative">
      <pre className="font-mono text-xs bg-bg-elevated/60 rounded-lg p-3 overflow-x-auto whitespace-pre text-text">
        {code}
      </pre>
      <div className="absolute top-1.5 end-1.5">
        <CopyButton value={code} />
      </div>
    </div>
  );
}
