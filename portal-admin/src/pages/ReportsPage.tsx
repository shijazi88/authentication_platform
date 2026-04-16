import { useEffect, useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import {
  Activity,
  CheckCircle2,
  Download,
  FileText,
  ScrollText,
  TrendingUp,
  XCircle,
} from "lucide-react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { toast } from "sonner";
import { listTenants } from "@/api/tenants";
import {
  downloadReportCsv,
  downloadReportPdf,
  getDailyReport,
  getMonthlyReport,
  type StatusFilter,
} from "@/api/reports";
import type { ReportGroupBy } from "@/types/api";
import { PageHeader } from "@/components/ui/PageHeader";
import { Button } from "@/components/ui/Button";
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
import { formatMoneyMinor, formatNumber } from "@/lib/format";

function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

function isoMinusDays(days: number): string {
  const d = new Date();
  d.setDate(d.getDate() - days);
  return d.toISOString().slice(0, 10);
}

function isoFirstOfYear(): string {
  return `${new Date().getFullYear()}-01-01`;
}

export function ReportsPage() {
  const { t } = useTranslation();
  const tenantsQ = useQuery({ queryKey: ["tenants"], queryFn: listTenants });

  const [tenantId, setTenantId] = useState<string | null>(null);
  const [groupBy, setGroupBy] = useState<ReportGroupBy>("daily");
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("ALL");
  const [from, setFrom] = useState<string>(isoMinusDays(30));
  const [to, setTo] = useState<string>(todayIso());
  const [exporting, setExporting] = useState(false);

  useEffect(() => {
    if (!tenantId && tenantsQ.data?.length) {
      setTenantId(tenantsQ.data[0].id);
    }
  }, [tenantId, tenantsQ.data]);

  // When the user switches to monthly mode, widen the default range to YTD
  // so they actually have multiple buckets to look at.
  useEffect(() => {
    if (groupBy === "monthly") {
      setFrom(isoFirstOfYear());
      setTo(todayIso());
    } else {
      setFrom(isoMinusDays(30));
      setTo(todayIso());
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [groupBy]);

  const reportQ = useQuery({
    queryKey: ["reports", groupBy, tenantId, from, to, statusFilter],
    queryFn: () => {
      const params = { tenantId: tenantId!, from, to, status: statusFilter };
      return groupBy === "daily" ? getDailyReport(params) : getMonthlyReport(params);
    },
    enabled: !!tenantId && !!from && !!to,
  });

  const chartData = useMemo(
    () =>
      (reportQ.data?.rows ?? []).map((r) => ({
        period: r.period,
        success: r.successCount,
        failed: r.failedCount,
      })),
    [reportQ.data],
  );

  async function handleExportCsv() {
    if (!tenantId) return;
    setExporting(true);
    try {
      await downloadReportCsv(groupBy, { tenantId, from, to, status: statusFilter });
      toast.success(t("reports.exportStarted"));
    } catch (e) {
      toast.error((e as Error).message);
    } finally {
      setExporting(false);
    }
  }

  async function handleExportPdf() {
    if (!tenantId) return;
    setExporting(true);
    try {
      await downloadReportPdf(groupBy, { tenantId, from, to, status: statusFilter });
      toast.success(t("reports.exportStarted"));
    } catch (e) {
      toast.error((e as Error).message);
    } finally {
      setExporting(false);
    }
  }

  const totals = reportQ.data?.totals;
  const successRatePct =
    totals && totals.totalTransactions > 0
      ? Math.round(totals.successRate * 100)
      : 0;

  const canExport = !!tenantId && !!reportQ.data?.rows.length;

  return (
    <div>
      <PageHeader
        title={t("reports.title")}
        description={t("reports.subtitle")}
        actions={
          <>
            <Button
              variant="secondary"
              leftIcon={<FileText className="h-4 w-4" />}
              onClick={handleExportPdf}
              loading={exporting}
              disabled={!canExport}
            >
              {t("reports.exportPdf")}
            </Button>
            <Button
              leftIcon={<Download className="h-4 w-4" />}
              onClick={handleExportCsv}
              loading={exporting}
              disabled={!canExport}
            >
              {t("reports.exportCsv")}
            </Button>
          </>
        }
      />

      {/* Filter card */}
      <Card className="mb-6">
        <CardBody className="grid grid-cols-1 md:grid-cols-5 gap-4">
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
            <Label>{t("reports.groupBy")}</Label>
            <Select<ReportGroupBy>
              value={groupBy}
              onChange={(v) => setGroupBy(v)}
              options={[
                { value: "daily", label: t("reports.daily") },
                { value: "monthly", label: t("reports.monthly") },
              ]}
            />
          </div>
          <div>
            <Label>{t("common.status")}</Label>
            <Select<StatusFilter>
              value={statusFilter}
              onChange={(v) => setStatusFilter(v)}
              options={[
                { value: "ALL", label: t("reports.statusAll") },
                { value: "SUCCESS", label: t("reports.successCount") },
                { value: "FAILED", label: t("reports.failedCount") },
              ]}
            />
          </div>
          <div>
            <Label htmlFor="from">{t("reports.from")}</Label>
            <Input
              id="from"
              type="date"
              value={from}
              max={to}
              onChange={(e) => setFrom(e.target.value)}
            />
          </div>
          <div>
            <Label htmlFor="to">{t("reports.to")}</Label>
            <Input
              id="to"
              type="date"
              value={to}
              min={from}
              onChange={(e) => setTo(e.target.value)}
            />
          </div>
        </CardBody>
      </Card>

      {/* KPI cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <MetricCard
          label={t("reports.metric.totalTx")}
          value={formatNumber(totals?.totalTransactions ?? 0)}
          icon={<Activity className="h-4 w-4" />}
          accentClass="from-accent-violet to-accent-cyan"
        />
        <MetricCard
          label={t("reports.metric.success")}
          value={formatNumber(totals?.successCount ?? 0)}
          icon={<CheckCircle2 className="h-4 w-4" />}
          accentClass="from-accent-emerald to-accent-cyan"
        />
        <MetricCard
          label={t("reports.metric.failed")}
          value={formatNumber(totals?.failedCount ?? 0)}
          icon={<XCircle className="h-4 w-4" />}
          accentClass="from-accent-rose to-accent-amber"
        />
        <MetricCard
          label={t("reports.metric.revenue")}
          value={formatMoneyMinor(totals?.amountMinor ?? 0, totals?.currency || "YER")}
          icon={<TrendingUp className="h-4 w-4" />}
          accentClass="from-accent-amber to-accent-violet"
        />
      </div>

      {/* Success rate banner */}
      {totals && totals.totalTransactions > 0 && (
        <Card className="mb-6">
          <CardBody className="flex items-center justify-between gap-6">
            <div>
              <div className="text-[10px] uppercase tracking-wider text-text-muted">
                {t("reports.successRate")}
              </div>
              <div className="text-2xl font-bold text-gradient mt-1">
                {successRatePct}%
              </div>
            </div>
            <div className="flex-1 max-w-md">
              <div className="h-2 rounded-full bg-bg-elevated/50 border border-border/15 overflow-hidden">
                <div
                  className="h-full bg-gradient-to-r from-accent-violet to-accent-cyan"
                  style={{ width: `${successRatePct}%` }}
                />
              </div>
            </div>
          </CardBody>
        </Card>
      )}

      {/* Chart */}
      <Card className="mb-6">
        <CardHeader>
          <CardTitle>
            {groupBy === "daily"
              ? t("reports.dailyTransactions")
              : t("reports.monthlyTransactions")}
          </CardTitle>
        </CardHeader>
        <CardBody>
          {reportQ.isLoading ? (
            <PageLoader />
          ) : chartData.length > 0 ? (
            <ResponsiveContainer width="100%" height={320}>
              <BarChart data={chartData}>
                <defs>
                  <linearGradient id="rep-success" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#1f7a4d" stopOpacity={0.95} />
                    <stop offset="100%" stopColor="#1f7a4d" stopOpacity={0.5} />
                  </linearGradient>
                  <linearGradient id="rep-failed" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#dc2626" stopOpacity={0.95} />
                    <stop offset="100%" stopColor="#dc2626" stopOpacity={0.5} />
                  </linearGradient>
                </defs>
                <CartesianGrid
                  strokeDasharray="3 3"
                  stroke="rgb(var(--border) / 0.08)"
                />
                <XAxis
                  dataKey="period"
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
                  cursor={{ fill: "rgb(var(--border) / 0.06)" }}
                />
                <Legend wrapperStyle={{ fontSize: 12, color: "rgb(var(--text-muted))" }} />
                <Bar
                  dataKey="success"
                  name={t("reports.successCount")}
                  stackId="a"
                  fill="url(#rep-success)"
                  radius={[0, 0, 0, 0]}
                />
                <Bar
                  dataKey="failed"
                  name={t("reports.failedCount")}
                  stackId="a"
                  fill="url(#rep-failed)"
                  radius={[6, 6, 0, 0]}
                />
              </BarChart>
            </ResponsiveContainer>
          ) : (
            <div className="py-12 text-center text-xs text-text-muted">
              {t("reports.noData")}
            </div>
          )}
        </CardBody>
      </Card>

      {/* Detailed table */}
      <Card>
        <CardHeader>
          <CardTitle>{t("reports.breakdown")}</CardTitle>
        </CardHeader>
        <CardBody className="p-0">
          {reportQ.isLoading ? (
            <PageLoader />
          ) : (reportQ.data?.rows.length ?? 0) > 0 ? (
            <Table>
              <THead>
                <Tr>
                  <Th>{t("reports.period")}</Th>
                  <Th>{t("reports.totalTx")}</Th>
                  <Th>{t("reports.successCount")}</Th>
                  <Th>{t("reports.failedCount")}</Th>
                  <Th>{t("reports.revenue")}</Th>
                </Tr>
              </THead>
              <TBody>
                {reportQ.data!.rows.map((row) => (
                  <Tr key={row.period}>
                    <Td className="font-mono text-xs">{row.period}</Td>
                    <Td>{formatNumber(row.total)}</Td>
                    <Td className="text-accent-emerald">
                      {formatNumber(row.successCount)}
                    </Td>
                    <Td className="text-accent-rose">
                      {formatNumber(row.failedCount)}
                    </Td>
                    <Td>
                      {row.amountMinor > 0
                        ? formatMoneyMinor(row.amountMinor, row.currency || "YER")
                        : "—"}
                    </Td>
                  </Tr>
                ))}
              </TBody>
            </Table>
          ) : (
            <EmptyState
              icon={<ScrollText className="h-5 w-5" />}
              title={t("reports.noData")}
              description={t("reports.adjustFilters")}
            />
          )}
        </CardBody>
      </Card>
    </div>
  );
}
