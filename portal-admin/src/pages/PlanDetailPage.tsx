import { useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, Eye, EyeOff, Pencil, Infinity as InfinityIcon, Users, Building2 } from "lucide-react";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";
import {
  getPlan,
  listPlanEntitlements,
  listPlanFieldEntitlements,
  listPlanSubscribers,
  updateEntitlementLimits,
} from "@/api/plans";
import { listServices, listOperations, listFields } from "@/api/catalog";
import { PageHeader } from "@/components/ui/PageHeader";
import {
  Card,
  CardBody,
  CardHeader,
  CardTitle,
} from "@/components/ui/Card";
import { Badge, statusTone } from "@/components/ui/Badge";
import { PageLoader } from "@/components/ui/Spinner";
import { Button } from "@/components/ui/Button";
import { Dialog } from "@/components/ui/Dialog";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { formatMoneyMinor, formatNumber } from "@/lib/format";
import type { PlanEntitlement } from "@/types/api";

export function PlanDetailPage() {
  const { t } = useTranslation();
  const { id = "" } = useParams();
  const navigate = useNavigate();
  const qc = useQueryClient();

  const planQ = useQuery({
    queryKey: ["plan", id],
    queryFn: () => getPlan(id),
    enabled: !!id,
  });

  const entitlementsQ = useQuery({
    queryKey: ["plan-entitlements", id],
    queryFn: () => listPlanEntitlements(id),
    enabled: !!id,
  });

  const subscribersQ = useQuery({
    queryKey: ["plan-subscribers", id],
    queryFn: () => listPlanSubscribers(id),
    enabled: !!id,
  });

  // Limits-edit dialog state
  const [editing, setEditing] = useState<PlanEntitlement | null>(null);
  const [rateUnlimited, setRateUnlimited] = useState(false);
  const [quotaUnlimited, setQuotaUnlimited] = useState(false);
  const [rateValue, setRateValue] = useState<string>("");
  const [quotaValue, setQuotaValue] = useState<string>("");

  function openLimits(e: PlanEntitlement) {
    setEditing(e);
    setRateUnlimited(e.rateLimitPerMinute == null);
    setQuotaUnlimited(e.monthlyQuota == null);
    setRateValue(e.rateLimitPerMinute != null ? String(e.rateLimitPerMinute) : "");
    setQuotaValue(e.monthlyQuota != null ? String(e.monthlyQuota) : "");
  }

  const limitsMut = useMutation({
    mutationFn: () => {
      if (!editing) return Promise.reject();
      return updateEntitlementLimits(id, editing.id, {
        rateLimitPerMinute: rateUnlimited ? null : Number(rateValue) || null,
        monthlyQuota: quotaUnlimited ? null : Number(quotaValue) || null,
      });
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["plan-entitlements", id] });
      toast.success(t("plans.detail.limitsSaved"));
      setEditing(null);
    },
  });

  const fieldEntsQ = useQuery({
    queryKey: ["plan-field-entitlements", id],
    queryFn: () => listPlanFieldEntitlements(id),
    enabled: !!id,
  });

  const servicesQ = useQuery({ queryKey: ["services"], queryFn: listServices });

  const firstServiceId = servicesQ.data?.[0]?.id;
  const operationsQ = useQuery({
    queryKey: ["operations", firstServiceId],
    queryFn: () => listOperations(firstServiceId!),
    enabled: !!firstServiceId,
  });
  const fieldsQ = useQuery({
    queryKey: ["fields", operationsQ.data?.[0]?.id],
    queryFn: () => listFields(operationsQ.data![0].id),
    enabled: !!operationsQ.data?.[0]?.id,
  });

  if (planQ.isLoading) return <PageLoader />;
  if (!planQ.data) return null;

  const plan = planQ.data;
  const visiblePathSet = new Set(fieldEntsQ.data?.map((f) => f.fieldPath) ?? []);
  const subscribers = subscribersQ.data ?? [];
  const activeCount = subscribers.filter((s) => s.status === "ACTIVE").length;

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
        title={plan.name}
        description={
          <div className="flex items-center gap-2 mt-1 flex-wrap">
            <span className="font-mono text-text-muted">{plan.code}</span>
            <span className="text-text-dim">·</span>
            <span className="text-text-muted">
              {t("plans.fields.baseFee")}{" "}
              {formatMoneyMinor(plan.baseFeeMinor, plan.currency)}
            </span>
            <span className="text-text-dim">·</span>
            <Badge tone={plan.active ? "emerald" : "neutral"}>
              {plan.active ? t("common.active") : t("common.inactive")}
            </Badge>
          </div>
        }
      />

      {/* Subscribers — who joined this plan */}
      <Card className="mb-4">
        <CardHeader>
          <div className="flex items-center gap-2">
            <Users className="h-4 w-4 text-text-muted" />
            <CardTitle>{t("plans.detail.subscribers")}</CardTitle>
          </div>
          <div className="flex items-center gap-2">
            <Badge tone="emerald">
              {t("plans.detail.activeBadge", { value: activeCount })}
            </Badge>
            <Badge tone="neutral">
              {t("plans.detail.totalBadge", { value: subscribers.length })}
            </Badge>
          </div>
        </CardHeader>
        <CardBody className="p-0">
          {subscribersQ.isLoading ? (
            <div className="p-6">
              <PageLoader />
            </div>
          ) : subscribers.length ? (
            <div className="divide-y divide-border/10">
              {subscribers.map((s) => (
                <button
                  key={s.subscriptionId}
                  type="button"
                  onClick={() => navigate(`/tenants/${s.tenantId}`)}
                  className="w-full flex items-center gap-3 px-4 py-3 text-start hover:bg-bg-elevated/40 transition-colors"
                >
                  <Building2 className="h-4 w-4 text-text-muted shrink-0" />
                  <div className="flex-1 min-w-0">
                    <div className="font-medium truncate">
                      {s.tenantLegalName ?? s.tenantId.slice(0, 8)}
                    </div>
                    <div className="text-xs text-text-muted truncate">
                      {s.tenantCode && (
                        <span className="font-mono">{s.tenantCode}</span>
                      )}
                      {s.tenantCode && <span className="mx-1">·</span>}
                      {t("subscriptions.fields.startDate")}: {s.startDate}
                      {s.endDate && (
                        <>
                          {" · "}
                          {t("subscriptions.fields.endDate")}: {s.endDate}
                        </>
                      )}
                    </div>
                  </div>
                  <Badge tone={statusTone(s.status)}>
                    {t(`status.${s.status}`, s.status)}
                  </Badge>
                </button>
              ))}
            </div>
          ) : (
            <div className="px-6 py-8 text-center text-xs text-text-muted">
              {t("plans.detail.noSubscribers")}
            </div>
          )}
        </CardBody>
      </Card>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        {/* Operation entitlements */}
        <Card>
          <CardHeader>
            <CardTitle>{t("plans.detail.operationEntitlements")}</CardTitle>
          </CardHeader>
          <CardBody className="space-y-3">
            {entitlementsQ.isLoading ? (
              <PageLoader />
            ) : entitlementsQ.data?.length ? (
              entitlementsQ.data.map((e) => {
                const op = operationsQ.data?.find((o) => o.id === e.operationId);
                const unlimited = (
                  <span className="inline-flex items-center gap-1 text-text-muted">
                    <InfinityIcon className="h-3 w-3" />
                    {t("plans.detail.unlimited")}
                  </span>
                );
                return (
                  <div
                    key={e.id}
                    className="p-3 rounded-lg bg-bg-elevated/40 border border-border/10"
                  >
                    <div className="flex items-center justify-between gap-2">
                      <div className="font-mono text-sm text-text">
                        {op ? `${op.code}` : e.operationId.slice(0, 8)}
                      </div>
                      <div className="flex items-center gap-2">
                        {e.unitPriceOverrideMinor != null && (
                          <Badge tone="violet">
                            {formatMoneyMinor(
                              e.unitPriceOverrideMinor,
                              op?.currency ?? "YER",
                            )}
                          </Badge>
                        )}
                        <Button
                          variant="ghost"
                          size="sm"
                          leftIcon={<Pencil className="h-3.5 w-3.5" />}
                          onClick={() => openLimits(e)}
                        >
                          {t("plans.detail.editLimits")}
                        </Button>
                      </div>
                    </div>
                    <div className="text-xs text-text-muted mt-1.5 flex gap-4 flex-wrap">
                      <span>
                        {t("plans.detail.rateLabel")}:{" "}
                        {e.rateLimitPerMinute != null
                          ? t("plans.detail.rate", {
                              value: e.rateLimitPerMinute,
                            })
                          : unlimited}
                      </span>
                      <span>
                        {t("plans.detail.quotaLabel")}:{" "}
                        {e.monthlyQuota != null
                          ? t("plans.detail.quota", {
                              value: formatNumber(e.monthlyQuota),
                            })
                          : unlimited}
                      </span>
                    </div>
                  </div>
                );
              })
            ) : (
              <div className="text-xs text-text-muted py-6 text-center">
                {t("plans.detail.noOperationEntitlements")}
              </div>
            )}
          </CardBody>
        </Card>

        {/* Field entitlements */}
        <Card>
          <CardHeader>
            <div>
              <CardTitle>{t("plans.detail.fieldEntitlements")}</CardTitle>
              <div className="text-xs text-text-muted mt-0.5">
                {t("plans.detail.fieldEntitlementsSubtitle")}
              </div>
            </div>
            <Badge tone="cyan">
              {t("plans.detail.visibleCount", {
                value: fieldEntsQ.data?.length ?? 0,
              })}
            </Badge>
          </CardHeader>
          <CardBody>
            {fieldsQ.isLoading ? (
              <PageLoader />
            ) : fieldsQ.data?.length ? (
              <div className="space-y-1">
                {fieldsQ.data.map((f) => {
                  const visible = visiblePathSet.has(f.path);
                  return (
                    <div
                      key={f.id}
                      className={`flex items-center justify-between px-3 py-2 rounded-lg text-xs transition-colors ${
                        visible
                          ? "bg-accent-emerald/[0.06] border border-accent-emerald/20"
                          : "bg-bg-elevated/30 border border-border/10"
                      }`}
                    >
                      <div>
                        <div className="font-mono text-text">{f.path}</div>
                        {f.description && (
                          <div className="text-text-muted mt-0.5">
                            {f.description}
                          </div>
                        )}
                      </div>
                      {visible ? (
                        <Eye className="h-4 w-4 text-accent-emerald" />
                      ) : (
                        <EyeOff className="h-4 w-4 text-text-dim" />
                      )}
                    </div>
                  );
                })}
              </div>
            ) : (
              <div className="text-xs text-text-muted py-6 text-center">
                {t("plans.detail.noFields")}
              </div>
            )}
          </CardBody>
        </Card>
      </div>

      <Dialog
        open={editing !== null}
        onClose={() => setEditing(null)}
        title={t("plans.detail.editLimits")}
        size="lg"
        footer={
          <>
            <Button variant="ghost" onClick={() => setEditing(null)}>
              {t("common.cancel")}
            </Button>
            <Button
              loading={limitsMut.isPending}
              onClick={() => limitsMut.mutate()}
            >
              {t("common.save")}
            </Button>
          </>
        }
      >
        <div className="space-y-5">
          <div>
            <Label htmlFor="rateLimit">
              {t("plans.detail.rateLabel")}{" "}
              <span className="text-text-dim text-xs">
                ({t("plans.detail.perCredentialPerOp")})
              </span>
            </Label>
            <div className="flex items-center gap-3 mt-1">
              <Input
                id="rateLimit"
                type="number"
                min={1}
                placeholder="60"
                value={rateValue}
                onChange={(e) => setRateValue(e.target.value)}
                disabled={rateUnlimited}
                className="flex-1"
              />
              <label className="inline-flex items-center gap-1.5 text-xs text-text-muted whitespace-nowrap cursor-pointer">
                <input
                  type="checkbox"
                  checked={rateUnlimited}
                  onChange={(e) => setRateUnlimited(e.target.checked)}
                />
                {t("plans.detail.unlimited")}
              </label>
            </div>
            <div className="mt-1 text-[11px] text-text-muted">
              {t("plans.detail.rateHint")}
            </div>
          </div>

          <div>
            <Label htmlFor="monthlyQuota">
              {t("plans.detail.quotaLabel")}{" "}
              <span className="text-text-dim text-xs">
                ({t("plans.detail.perTenantPerMonth")})
              </span>
            </Label>
            <div className="flex items-center gap-3 mt-1">
              <Input
                id="monthlyQuota"
                type="number"
                min={1}
                placeholder="100000"
                value={quotaValue}
                onChange={(e) => setQuotaValue(e.target.value)}
                disabled={quotaUnlimited}
                className="flex-1"
              />
              <label className="inline-flex items-center gap-1.5 text-xs text-text-muted whitespace-nowrap cursor-pointer">
                <input
                  type="checkbox"
                  checked={quotaUnlimited}
                  onChange={(e) => setQuotaUnlimited(e.target.checked)}
                />
                {t("plans.detail.unlimited")}
              </label>
            </div>
            <div className="mt-1 text-[11px] text-text-muted">
              {t("plans.detail.quotaHint")}
            </div>
          </div>
        </div>
      </Dialog>
    </div>
  );
}
