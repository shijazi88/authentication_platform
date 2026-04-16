import { useEffect, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Receipt, TrendingUp } from "lucide-react";
import { useTranslation, Trans } from "react-i18next";
import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { listTenants } from "@/api/tenants";
import { getBillingSummary, listBillingEvents } from "@/api/billing";
import { PageHeader } from "@/components/ui/PageHeader";
import {
  Card,
  CardBody,
  CardHeader,
  CardTitle,
} from "@/components/ui/Card";
import { Table, TBody, THead, Th, Td, Tr } from "@/components/ui/Table";
import { Select } from "@/components/ui/Select";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { EmptyState } from "@/components/ui/EmptyState";
import { PageLoader } from "@/components/ui/Spinner";
import { MetricCard } from "@/components/viz/MetricCard";
import {
  currentPeriod,
  formatDate,
  formatMoneyMinor,
  formatNumber,
  shortId,
} from "@/lib/format";

export function BillingPage() {
  const { t } = useTranslation();
  const tenantsQ = useQuery({ queryKey: ["tenants"], queryFn: listTenants });
  const [tenantId, setTenantId] = useState<string | null>(null);
  const [period, setPeriod] = useState(currentPeriod());

  useEffect(() => {
    if (!tenantId && tenantsQ.data?.length) {
      setTenantId(tenantsQ.data[0].id);
    }
  }, [tenantId, tenantsQ.data]);

  const summaryQ = useQuery({
    queryKey: ["billing-summary", tenantId, period],
    queryFn: () => getBillingSummary({ tenantId: tenantId!, period }),
    enabled: !!tenantId,
  });

  const eventsQ = useQuery({
    queryKey: ["billing-events", tenantId, period],
    queryFn: () =>
      listBillingEvents({ tenantId: tenantId!, period, page: 0, size: 100 }),
    enabled: !!tenantId,
  });

  const totalAmount =
    summaryQ.data?.reduce((acc, s) => acc + s.totalAmountMinor, 0) ?? 0;
  const totalCount =
    summaryQ.data?.reduce((acc, s) => acc + Number(s.transactionCount), 0) ?? 0;
  const currency = summaryQ.data?.[0]?.currency ?? "YER";

  const perDay = new Map<string, number>();
  for (const e of eventsQ.data?.content ?? []) {
    const day = e.occurredAt.slice(0, 10);
    perDay.set(day, (perDay.get(day) ?? 0) + e.amountMinor);
  }
  const chartData = Array.from(perDay.entries())
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([day, amount]) => ({ day: day.slice(5), amount: amount / 100 }));

  const tenantCode =
    tenantsQ.data?.find((tenant) => tenant.id === tenantId)?.code ?? "—";

  return (
    <div>
      <PageHeader
        title={t("billing.title")}
        description={t("billing.subtitle")}
      />

      <Card className="mb-6">
        <CardBody className="grid grid-cols-1 md:grid-cols-3 gap-4 items-end">
          <div>
            <Label>{t("subscriptions.fields.tenant")}</Label>
            <Select
              value={tenantId}
              onChange={setTenantId}
              placeholder={t("common.selectTenant")}
              options={
                tenantsQ.data?.map((tenant) => ({
                  value: tenant.id,
                  label: tenant.legalName,
                  description: tenant.code,
                })) ?? []
              }
            />
          </div>
          <div>
            <Label htmlFor="period">{t("billing.period")}</Label>
            <Input
              id="period"
              value={period}
              onChange={(e) => setPeriod(e.target.value)}
              placeholder={currentPeriod()}
            />
          </div>
          <div className="text-xs text-text-muted">
            <Trans
              i18nKey="billing.showingFor"
              values={{ code: tenantCode, period }}
              components={{
                1: <span className="font-mono text-text" />,
                3: <span className="font-mono text-text" />,
              }}
            />
          </div>
        </CardBody>
      </Card>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-6">
        <MetricCard
          label={t("billing.metric.periodRevenue")}
          value={formatMoneyMinor(totalAmount, currency)}
          icon={<TrendingUp className="h-4 w-4" />}
          accentClass="from-accent-violet to-accent-cyan"
        />
        <MetricCard
          label={t("billing.metric.billableTx")}
          value={formatNumber(totalCount)}
          icon={<Receipt className="h-4 w-4" />}
          accentClass="from-accent-emerald to-accent-cyan"
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle>{t("billing.dailyRevenue")}</CardTitle>
          </CardHeader>
          <CardBody>
            {chartData.length > 0 ? (
              <ResponsiveContainer width="100%" height={260}>
                <BarChart data={chartData}>
                  <defs>
                    <linearGradient id="rev" x1="0" y1="0" x2="0" y2="1">
                      <stop
                        offset="0%"
                        stopColor="#d4a017"
                        stopOpacity={0.95}
                      />
                      <stop
                        offset="100%"
                        stopColor="#1e4e8c"
                        stopOpacity={0.7}
                      />
                    </linearGradient>
                  </defs>
                  <CartesianGrid
                    strokeDasharray="3 3"
                    stroke="rgb(var(--border) / 0.08)"
                  />
                  <XAxis
                    dataKey="day"
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
                    formatter={(v: number) => [
                      `$${v.toFixed(2)}`,
                      t("billing.metric.periodRevenue"),
                    ]}
                  />
                  <Bar
                    dataKey="amount"
                    fill="url(#rev)"
                    radius={[6, 6, 0, 0]}
                  />
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <div className="py-12 text-center text-xs text-text-muted">
                {t("billing.noPeriodEvents")}
              </div>
            )}
          </CardBody>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>{t("billing.summaryByCurrency")}</CardTitle>
          </CardHeader>
          <CardBody className="space-y-2">
            {summaryQ.data?.length ? (
              summaryQ.data.map((s) => (
                <div
                  key={s.currency}
                  className="p-3 rounded-lg bg-bg-elevated/40 border border-border/10"
                >
                  <div className="text-xs text-text-muted">{s.currency}</div>
                  <div className="text-xl font-bold text-gradient mt-1">
                    {formatMoneyMinor(s.totalAmountMinor, s.currency)}
                  </div>
                  <div className="text-xs text-text-muted mt-1">
                    {formatNumber(Number(s.transactionCount))}{" "}
                    {t("transactions.title")}
                  </div>
                </div>
              ))
            ) : (
              <div className="text-xs text-text-muted py-4 text-center">
                {t("common.noData")}
              </div>
            )}
          </CardBody>
        </Card>
      </div>

      <Card className="mt-6">
        <CardHeader>
          <CardTitle>{t("billing.events")}</CardTitle>
        </CardHeader>
        <CardBody className="p-0">
          {eventsQ.isLoading ? (
            <PageLoader />
          ) : eventsQ.data?.content.length ? (
            <Table>
              <THead>
                <Tr>
                  <Th>{t("billing.fields.eventId")}</Th>
                  <Th>{t("billing.fields.transaction")}</Th>
                  <Th>{t("billing.fields.amount")}</Th>
                  <Th>{t("billing.period")}</Th>
                  <Th>{t("billing.fields.occurred")}</Th>
                </Tr>
              </THead>
              <TBody>
                {eventsQ.data.content.map((e) => (
                  <Tr key={e.id}>
                    <Td className="font-mono text-xs text-text-muted">
                      {shortId(e.id, 14)}
                    </Td>
                    <Td className="font-mono text-xs text-text-muted">
                      {shortId(e.transactionId, 14)}
                    </Td>
                    <Td>{formatMoneyMinor(e.amountMinor, e.currency)}</Td>
                    <Td className="font-mono text-xs">{e.period}</Td>
                    <Td className="text-text-muted text-xs">
                      {formatDate(e.occurredAt)}
                    </Td>
                  </Tr>
                ))}
              </TBody>
            </Table>
          ) : (
            <EmptyState
              icon={<Receipt className="h-5 w-5" />}
              title={t("billing.empty.title")}
              description={t("billing.empty.description")}
            />
          )}
        </CardBody>
      </Card>
    </div>
  );
}
