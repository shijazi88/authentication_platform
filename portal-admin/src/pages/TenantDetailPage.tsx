import { useParams, useNavigate } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { ArrowLeft, KeyRound, ShieldCheck, ShieldAlert } from "lucide-react";
import { toast } from "sonner";
import { useTranslation } from "react-i18next";
import {
  getTenant,
  issueCredential,
  setTenantStatus,
} from "@/api/tenants";
import { listSubscriptionsByTenant } from "@/api/subscriptions";
import { listPlans } from "@/api/plans";
import { PageHeader } from "@/components/ui/PageHeader";
import { Button } from "@/components/ui/Button";
import {
  Card,
  CardBody,
  CardHeader,
  CardTitle,
} from "@/components/ui/Card";
import { Badge, statusTone } from "@/components/ui/Badge";
import { Dialog } from "@/components/ui/Dialog";
import { Label } from "@/components/ui/Label";
import { Input } from "@/components/ui/Input";
import { CopyButton } from "@/components/ui/CopyButton";
import { PageLoader } from "@/components/ui/Spinner";
import { formatDate, shortId } from "@/lib/format";
import type { ApiCredential } from "@/types/api";

export function TenantDetailPage() {
  const { t } = useTranslation();
  const { id = "" } = useParams();
  const navigate = useNavigate();
  const qc = useQueryClient();

  const tenantQ = useQuery({
    queryKey: ["tenant", id],
    queryFn: () => getTenant(id),
    enabled: !!id,
  });

  const subsQ = useQuery({
    queryKey: ["subscriptions", id],
    queryFn: () => listSubscriptionsByTenant(id),
    enabled: !!id,
  });

  const plansQ = useQuery({ queryKey: ["plans"], queryFn: listPlans });

  const [credDialogOpen, setCredDialogOpen] = useState(false);
  const [issuedCred, setIssuedCred] = useState<ApiCredential | null>(null);
  const [credLabel, setCredLabel] = useState("");

  const issueMut = useMutation({
    mutationFn: () => issueCredential(id, { label: credLabel || undefined }),
    onSuccess: (cred) => {
      setIssuedCred(cred);
      setCredLabel("");
    },
  });

  const statusMut = useMutation({
    mutationFn: (status: "ACTIVE" | "SUSPENDED") => setTenantStatus(id, status),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["tenant", id] });
      qc.invalidateQueries({ queryKey: ["tenants"] });
      toast.success(t("common.status"));
    },
  });

  if (tenantQ.isLoading) return <PageLoader />;
  if (!tenantQ.data) return null;

  const tenant = tenantQ.data;
  const planById = (pid: string) => plansQ.data?.find((p) => p.id === pid);

  return (
    <div>
      <button
        onClick={() => navigate(-1)}
        className="inline-flex items-center gap-1 text-xs text-text-muted hover:text-text mb-4 transition-colors"
      >
        <ArrowLeft className="h-3.5 w-3.5 rtl-flip" />
        {t("common.back")}
      </button>

      <PageHeader
        title={tenant.legalName}
        description={
          <div className="flex items-center gap-2 mt-1">
            <span className="font-mono text-text-muted">{tenant.code}</span>
            <span className="text-text-dim">·</span>
            <Badge tone={statusTone(tenant.status)}>
              {t(`status.${tenant.status}`, tenant.status)}
            </Badge>
          </div>
        }
        actions={
          <>
            {tenant.status === "ACTIVE" ? (
              <Button
                variant="secondary"
                leftIcon={<ShieldAlert className="h-4 w-4" />}
                loading={statusMut.isPending}
                onClick={() => statusMut.mutate("SUSPENDED")}
              >
                {t("tenants.suspend")}
              </Button>
            ) : (
              <Button
                variant="secondary"
                leftIcon={<ShieldCheck className="h-4 w-4" />}
                loading={statusMut.isPending}
                onClick={() => statusMut.mutate("ACTIVE")}
              >
                {t("tenants.activate")}
              </Button>
            )}
            <Button
              leftIcon={<KeyRound className="h-4 w-4" />}
              onClick={() => {
                setIssuedCred(null);
                setCredDialogOpen(true);
              }}
            >
              {t("tenants.issueCredential")}
            </Button>
          </>
        }
      />

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        {/* Identity card */}
        <Card>
          <CardHeader>
            <CardTitle>{t("tenants.detail.identity")}</CardTitle>
          </CardHeader>
          <CardBody className="space-y-3 text-sm">
            <Field label={t("common.id")}>
              <span className="font-mono text-xs">{tenant.id}</span>
            </Field>
            <Field label={t("tenants.fields.code")}>
              <span className="font-mono">{tenant.code}</span>
            </Field>
            <Field label={t("tenants.fields.legalName")}>
              {tenant.legalName}
            </Field>
            <Field label={t("tenants.fields.contact")}>
              {tenant.contactEmail ?? "—"}
            </Field>
            <Field label={t("common.status")}>
              <Badge tone={statusTone(tenant.status)}>
                {t(`status.${tenant.status}`, tenant.status)}
              </Badge>
            </Field>
            <Field label={t("common.createdAt")}>
              <span className="text-text-muted">
                {formatDate(tenant.createdAt)}
              </span>
            </Field>
          </CardBody>
        </Card>

        {/* Subscriptions */}
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle>{t("tenants.detail.subscriptions")}</CardTitle>
          </CardHeader>
          <CardBody>
            {subsQ.isLoading ? (
              <PageLoader />
            ) : subsQ.data?.length ? (
              <div className="space-y-2">
                {subsQ.data.map((s) => {
                  const plan = planById(s.planId);
                  return (
                    <div
                      key={s.id}
                      className="flex items-center justify-between p-3 rounded-lg bg-bg-elevated/40 border border-border/10"
                    >
                      <div>
                        <div className="text-sm text-text font-medium">
                          {plan?.name ?? shortId(s.planId)}
                        </div>
                        <div className="text-xs text-text-muted mt-0.5">
                          {plan?.code ?? "—"} · {s.startDate}
                          {s.endDate ? ` → ${s.endDate}` : ""}
                        </div>
                      </div>
                      <Badge tone={statusTone(s.status)}>
                        {t(`status.${s.status}`, s.status)}
                      </Badge>
                    </div>
                  );
                })}
              </div>
            ) : (
              <div className="text-xs text-text-muted py-6 text-center">
                {t("tenants.detail.noSubscriptions")}
              </div>
            )}
          </CardBody>
        </Card>
      </div>

      <Dialog
        open={credDialogOpen}
        onClose={() => setCredDialogOpen(false)}
        title={
          issuedCred
            ? t("tenants.credential.issuedTitle")
            : t("tenants.credential.title")
        }
        description={
          issuedCred
            ? t("tenants.credential.issuedDescription")
            : t("tenants.credential.description")
        }
        size="lg"
        footer={
          issuedCred ? (
            <Button onClick={() => setCredDialogOpen(false)}>
              {t("common.done")}
            </Button>
          ) : (
            <>
              <Button
                variant="ghost"
                onClick={() => setCredDialogOpen(false)}
              >
                {t("common.cancel")}
              </Button>
              <Button
                loading={issueMut.isPending}
                onClick={() => issueMut.mutate()}
              >
                {t("tenants.issueCredential")}
              </Button>
            </>
          )
        }
      >
        {issuedCred ? (
          <div className="space-y-4">
            <SecretRow
              label={t("tenants.credential.clientId")}
              value={issuedCred.clientId}
            />
            <SecretRow
              label={t("tenants.credential.clientSecret")}
              value={issuedCred.clientSecret}
            />
            <div className="px-3 py-2 rounded-lg bg-accent-amber/10 border border-accent-amber/20 text-xs text-accent-amber">
              {t("tenants.credential.warn")}
            </div>
          </div>
        ) : (
          <div>
            <Label htmlFor="label">{t("tenants.credential.label")}</Label>
            <Input
              id="label"
              placeholder={t("tenants.credential.labelPlaceholder")}
              value={credLabel}
              onChange={(e) => setCredLabel(e.target.value)}
            />
          </div>
        )}
      </Dialog>
    </div>
  );
}

function Field({
  label,
  children,
}: {
  label: string;
  children: React.ReactNode;
}) {
  return (
    <div className="grid grid-cols-3 gap-2 items-baseline">
      <div className="text-[10px] uppercase tracking-wider text-text-muted">
        {label}
      </div>
      <div className="col-span-2 text-text">{children}</div>
    </div>
  );
}

function SecretRow({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <Label>{label}</Label>
      <div className="flex items-center gap-2">
        <code
          dir="ltr"
          className="flex-1 px-3 py-2 rounded-lg bg-bg-elevated border border-border/15 text-xs font-mono text-accent-cyan break-all"
        >
          {value}
        </code>
        <CopyButton value={value} label={label} />
      </div>
    </div>
  );
}
