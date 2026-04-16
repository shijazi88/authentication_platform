import { useNavigate, useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { ArrowLeft, Eye, EyeOff } from "lucide-react";
import { useTranslation } from "react-i18next";
import {
  getPlan,
  listPlanEntitlements,
  listPlanFieldEntitlements,
} from "@/api/plans";
import { listServices, listOperations, listFields } from "@/api/catalog";
import { PageHeader } from "@/components/ui/PageHeader";
import {
  Card,
  CardBody,
  CardHeader,
  CardTitle,
} from "@/components/ui/Card";
import { Badge } from "@/components/ui/Badge";
import { PageLoader } from "@/components/ui/Spinner";
import { formatMoneyMinor, formatNumber } from "@/lib/format";

export function PlanDetailPage() {
  const { t } = useTranslation();
  const { id = "" } = useParams();
  const navigate = useNavigate();

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
                return (
                  <div
                    key={e.id}
                    className="p-3 rounded-lg bg-bg-elevated/40 border border-border/10"
                  >
                    <div className="flex items-center justify-between">
                      <div className="font-mono text-sm text-text">
                        {op ? `${op.code}` : e.operationId.slice(0, 8)}
                      </div>
                      {e.unitPriceOverrideMinor != null && (
                        <Badge tone="violet">
                          {formatMoneyMinor(
                            e.unitPriceOverrideMinor,
                            op?.currency ?? "YER",
                          )}
                        </Badge>
                      )}
                    </div>
                    <div className="text-xs text-text-muted mt-1.5 flex gap-4 flex-wrap">
                      {e.monthlyQuota != null && (
                        <span>
                          {t("plans.detail.quota", {
                            value: formatNumber(e.monthlyQuota),
                          })}
                        </span>
                      )}
                      {e.rateLimitPerMinute != null && (
                        <span>
                          {t("plans.detail.rate", {
                            value: e.rateLimitPerMinute,
                          })}
                        </span>
                      )}
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
    </div>
  );
}
