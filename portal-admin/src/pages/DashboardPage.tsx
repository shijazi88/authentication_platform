import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import {
  Activity,
  Building2,
  CreditCard,
  ScrollText,
  TrendingUp,
} from "lucide-react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { Link } from "react-router-dom";
import { listTenants } from "@/api/tenants";
import { listPlans } from "@/api/plans";
import { listTransactions } from "@/api/transactions";
import { getBillingSummary } from "@/api/billing";
import { PageHeader } from "@/components/ui/PageHeader";
import { MetricCard } from "@/components/viz/MetricCard";
import { Card, CardBody, CardHeader, CardTitle } from "@/components/ui/Card";
import { formatDate, formatMoneyMinor, currentPeriod, shortId } from "@/lib/format";
import { Badge, statusTone } from "@/components/ui/Badge";
import { PageLoader } from "@/components/ui/Spinner";

export function DashboardPage() {
  const { t } = useTranslation();
  const tenantsQ = useQuery({ queryKey: ["tenants"], queryFn: listTenants });
  const plansQ = useQuery({ queryKey: ["plans"], queryFn: listPlans });

  const firstTenantId = tenantsQ.data?.[0]?.id;
  const period = currentPeriod();

  const txQ = useQuery({
    queryKey: ["transactions", firstTenantId],
    queryFn: () =>
      listTransactions({ tenantId: firstTenantId!, page: 0, size: 10 }),
    enabled: !!firstTenantId,
  });

  const summaryQ = useQuery({
    queryKey: ["billing-summary", firstTenantId, period],
    queryFn: () => getBillingSummary({ tenantId: firstTenantId!, period }),
    enabled: !!firstTenantId,
  });

  if (tenantsQ.isLoading) return <PageLoader />;

  const totalRevenueMinor =
    summaryQ.data?.reduce((acc, s) => acc + s.totalAmountMinor, 0) ?? 0;
  const totalTxCount =
    summaryQ.data?.reduce((acc, s) => acc + Number(s.transactionCount), 0) ?? 0;

  const txByStatus =
    txQ.data?.content.reduce<Record<string, number>>((acc, tx) => {
      acc[tx.status] = (acc[tx.status] ?? 0) + 1;
      return acc;
    }, {}) ?? {};
  const chartData = Object.entries(txByStatus).map(([name, value]) => ({
    name: t(`status.${name}`, name),
    value,
  }));

  return (
    <div>
      <PageHeader
        title={t("dashboard.title")}
        description={t("dashboard.subtitle")}
      />

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <MetricCard
          label={t("dashboard.metric.tenants")}
          value={tenantsQ.data?.length ?? 0}
          icon={<Building2 className="h-4 w-4" />}
          accentClass="from-accent-violet to-accent-cyan"
        />
        <MetricCard
          label={t("dashboard.metric.plans")}
          value={plansQ.data?.length ?? 0}
          icon={<CreditCard className="h-4 w-4" />}
          accentClass="from-accent-cyan to-accent-emerald"
        />
        <MetricCard
          label={t("dashboard.metric.txThisPeriod", { period })}
          value={totalTxCount}
          icon={<Activity className="h-4 w-4" />}
          accentClass="from-accent-emerald to-accent-cyan"
        />
        <MetricCard
          label={t("dashboard.metric.revenueThisPeriod")}
          value={formatMoneyMinor(
            totalRevenueMinor,
            summaryQ.data?.[0]?.currency ?? "YER",
          )}
          icon={<TrendingUp className="h-4 w-4" />}
          accentClass="from-accent-amber to-accent-violet"
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <Card className="lg:col-span-2">
          <CardHeader>
            <div>
              <CardTitle>{t("dashboard.recent.title")}</CardTitle>
              <div className="text-xs text-text-muted mt-0.5">
                {t("dashboard.recent.subtitle", {
                  tenant: tenantsQ.data?.[0]?.code ?? "—",
                })}
              </div>
            </div>
            <Link
              to="/transactions"
              className="text-xs text-accent-cyan hover:underline"
            >
              {t("common.viewAll")} →
            </Link>
          </CardHeader>
          <div className="px-2 pb-2">
            {txQ.isLoading ? (
              <PageLoader />
            ) : txQ.data?.content.length ? (
              <div className="divide-y divide-border/10">
                {txQ.data.content.map((tx) => (
                  <div
                    key={tx.id}
                    className="flex items-center justify-between px-3 py-2.5 text-xs"
                  >
                    <div className="flex items-center gap-3 min-w-0">
                      <ScrollText className="h-4 w-4 text-text-dim shrink-0" />
                      <div className="min-w-0">
                        <div className="font-mono text-text truncate">
                          {shortId(tx.id, 14)}
                        </div>
                        <div className="text-text-muted">
                          {formatDate(tx.createdAt)}
                        </div>
                      </div>
                    </div>
                    <div className="flex items-center gap-3 shrink-0">
                      {tx.unitPriceMinor != null && tx.currency && (
                        <span className="text-text-muted">
                          {formatMoneyMinor(tx.unitPriceMinor, tx.currency)}
                        </span>
                      )}
                      <Badge tone={statusTone(tx.status)}>
                        {t(`status.${tx.status}`, tx.status)}
                      </Badge>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="px-4 py-8 text-center text-xs text-text-muted">
                {t("transactions.empty.title")}
              </div>
            )}
          </div>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>{t("dashboard.statusDistribution")}</CardTitle>
          </CardHeader>
          <CardBody>
            {chartData.length > 0 ? (
              <ResponsiveContainer width="100%" height={220}>
                <BarChart data={chartData}>
                  <defs>
                    <linearGradient id="bar" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%" stopColor="#1e4e8c" stopOpacity={0.95} />
                      <stop offset="100%" stopColor="#d4a017" stopOpacity={0.7} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid
                    strokeDasharray="3 3"
                    stroke="rgb(var(--border) / 0.08)"
                  />
                  <XAxis
                    dataKey="name"
                    stroke="rgb(var(--text-dim))"
                    fontSize={10}
                    tickLine={false}
                    axisLine={false}
                  />
                  <YAxis
                    stroke="rgb(var(--text-dim))"
                    fontSize={10}
                    tickLine={false}
                    axisLine={false}
                    allowDecimals={false}
                  />
                  <Tooltip
                    contentStyle={{
                      background: "rgb(var(--bg-surface) / 0.95)",
                      border: "1px solid rgb(var(--border) / 0.12)",
                      borderRadius: 8,
                      fontSize: 12,
                      color: "rgb(var(--text))",
                    }}
                    cursor={{ fill: "rgb(var(--border) / 0.08)" }}
                  />
                  <Bar dataKey="value" fill="url(#bar)" radius={[6, 6, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <div className="text-xs text-text-muted text-center py-12">
                {t("common.noData")}
              </div>
            )}
          </CardBody>
        </Card>
      </div>
    </div>
  );
}
