import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import {
  Activity,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Download,
  FileText,
  Fingerprint,
  Gauge,
  Receipt,
  ScrollText,
  Timer,
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
  adminReportsApi,
  getImageQualityReport,
  tenantReportsApi,
  type ReportKind,
  type ReportsApi,
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
import { Badge, statusTone, verdictTone } from "@/components/ui/Badge";
import { Select } from "@/components/ui/Select";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { EmptyState } from "@/components/ui/EmptyState";
import { PageLoader } from "@/components/ui/Spinner";
import { MetricCard } from "@/components/viz/MetricCard";
import { formatDate, formatMoneyMinor, formatNumber, shortId } from "@/lib/format";

type ReportType = "summary" | "details";

const DETAILS_PAGE_SIZE = 50;

/** Share of `part` in `whole` as a whole percentage (0 when `whole` is 0). */
function pct(part: number, whole: number): number {
  return whole > 0 ? Math.round((part / whole) * 100) : 0;
}

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
  return <ReportsView scope="admin" />;
}

/**
 * The Reports page. `admin`: any bank (bank picker) plus the cross-bank
 * fingerprint-quality table. `tenant`: the bank's own portal — its own data
 * only, amounts shown as "charged" rather than revenue.
 */
export function ReportsView({ scope }: { scope: "admin" | "tenant" }) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const isAdmin = scope === "admin";
  const reportsApi: ReportsApi = isAdmin ? adminReportsApi : tenantReportsApi;
  const tenantsQ = useQuery({ queryKey: ["tenants"], queryFn: listTenants, enabled: isAdmin });

  const [tenantId, setTenantId] = useState<string | null>(null);
  const [reportType, setReportType] = useState<ReportType>("summary");
  const [groupBy, setGroupBy] = useState<ReportGroupBy>("daily");
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("ALL");
  const [from, setFrom] = useState<string>(isoMinusDays(30));
  const [to, setTo] = useState<string>(todayIso());
  const [exporting, setExporting] = useState(false);
  const [page, setPage] = useState(0);

  // Any filter change starts the details listing from its first page again.
  useEffect(() => {
    setPage(0);
  }, [tenantId, statusFilter, from, to, reportType]);

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

  // The bank portal is always scoped to the signed-in bank; the admin picks one.
  const ready = (!isAdmin || !!tenantId) && !!from && !!to;
  const params = { tenantId: tenantId ?? undefined, from, to, status: statusFilter };

  const reportQ = useQuery({
    queryKey: ["reports", scope, groupBy, tenantId, from, to, statusFilter],
    queryFn: () => reportsApi.summary(groupBy, params),
    enabled: ready,
  });

  const detailsQ = useQuery({
    queryKey: ["reports", scope, "details", tenantId, from, to, statusFilter, page],
    queryFn: () => reportsApi.details({ ...params, page, size: DETAILS_PAGE_SIZE }),
    enabled: reportType === "details" && ready,
    placeholderData: (prev) => prev,
  });

  const qualityQ = useQuery({
    queryKey: ["reports", "image-quality", from, to],
    queryFn: () => getImageQualityReport(from, to),
    enabled: isAdmin && !!from && !!to,
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

  const exportKind: ReportKind = reportType === "details" ? "details" : groupBy;

  async function handleExportCsv() {
    if (!ready) return;
    setExporting(true);
    try {
      await reportsApi.downloadCsv(exportKind, params);
      toast.success(t("reports.exportStarted"));
    } catch (e) {
      toast.error((e as Error).message);
    } finally {
      setExporting(false);
    }
  }

  async function handleExportPdf() {
    if (!ready) return;
    setExporting(true);
    try {
      await reportsApi.downloadPdf(exportKind, params);
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

  const breakdown = reportQ.data?.breakdown;
  const totalPages = detailsQ.data?.totalPages ?? 0;

  const canExport = ready && !!reportQ.data?.rows.length;
  const filterCols = isAdmin
    ? reportType === "summary" ? "md:grid-cols-6" : "md:grid-cols-5"
    : reportType === "summary" ? "md:grid-cols-5" : "md:grid-cols-4";

  return (
    <div>
      <PageHeader
        title={t("reports.title")}
        description={isAdmin ? t("reports.subtitle") : t("reports.tenantSubtitle")}
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
        <CardBody className={`grid grid-cols-1 gap-4 ${filterCols}`}>
          {isAdmin && (
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
          )}
          <div>
            <Label>{t("reports.reportType")}</Label>
            <Select<ReportType>
              value={reportType}
              onChange={(v) => setReportType(v)}
              options={[
                { value: "summary", label: t("reports.summary") },
                { value: "details", label: t("reports.details") },
              ]}
            />
          </div>
          {reportType === "summary" && (
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
          )}
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
          label={isAdmin ? t("reports.metric.revenue") : t("reports.metric.charged")}
          value={formatMoneyMinor(totals?.amountMinor ?? 0, totals?.currency || "YER")}
          icon={<TrendingUp className="h-4 w-4" />}
          accentClass="from-accent-amber to-accent-violet"
        />
      </div>

      {reportType === "summary" ? (
        <>
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

          {/* Verification results + failure reasons */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
            <Card>
              <CardHeader>
                <CardTitle>{t("reports.results.title")}</CardTitle>
              </CardHeader>
              <CardBody className="space-y-3">
                <p className="text-xs text-text-muted">{t("reports.results.subtitle")}</p>
                {breakdown && breakdown.verdicts.length > 0 ? (
                  breakdown.verdicts.map((v) => {
                    const share = pct(v.count, totals?.successCount ?? 0);
                    return (
                      <div key={v.verdict ?? "none"}>
                        <div className="flex items-center justify-between gap-3 text-sm">
                          {v.verdict ? (
                            <Badge tone={verdictTone(v.verdict)}>{t(`verdict.${v.verdict}`, v.verdict)}</Badge>
                          ) : (
                            <span className="text-text-muted">{t("reports.results.notRecorded")}</span>
                          )}
                          <span className="tabular-nums">
                            {formatNumber(v.count)} <span className="text-text-muted text-xs">· {share}%</span>
                          </span>
                        </div>
                        <div className="mt-1.5 h-1.5 rounded-full bg-bg-elevated/50 overflow-hidden">
                          <div
                            className="h-full bg-gradient-to-r from-accent-violet to-accent-cyan"
                            style={{ width: `${share}%` }}
                          />
                        </div>
                      </div>
                    );
                  })
                ) : (
                  <div className="py-6 text-center text-xs text-text-muted">{t("reports.noData")}</div>
                )}
              </CardBody>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>{t("reports.failures.title")}</CardTitle>
              </CardHeader>
              <CardBody className="p-0">
                <p className="px-4 pt-3 pb-2 text-xs text-text-muted">{t("reports.failures.subtitle")}</p>
                {breakdown && breakdown.failureReasons.length > 0 ? (
                  <Table>
                    <THead>
                      <Tr>
                        <Th>{t("reports.failures.code")}</Th>
                        <Th>{t("reports.failures.reason")}</Th>
                        <Th>{t("reports.count")}</Th>
                        <Th>{t("reports.share")}</Th>
                      </Tr>
                    </THead>
                    <TBody>
                      {breakdown.failureReasons.map((f) => (
                        <Tr key={f.errorCode ?? "none"}>
                          <Td className="font-mono text-xs text-accent-rose">{f.errorCode ?? "—"}</Td>
                          <Td className="text-xs">
                            <div className="font-medium">{f.error ?? "—"}</div>
                            {f.message && <div className="text-text-muted line-clamp-2">{f.message}</div>}
                          </Td>
                          <Td className="tabular-nums">{formatNumber(f.count)}</Td>
                          <Td className="tabular-nums">{pct(f.count, totals?.failedCount ?? 0)}%</Td>
                        </Tr>
                      ))}
                    </TBody>
                  </Table>
                ) : (
                  <div className="py-8 text-center text-xs text-text-muted">{t("reports.failures.none")}</div>
                )}
              </CardBody>
            </Card>
          </div>

          {/* Performance & billing */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
            <MetricCard
              label={t("reports.perf.avg")}
              value={breakdown?.avgLatencyMs != null ? `${formatNumber(breakdown.avgLatencyMs)} ms` : "—"}
              icon={<Gauge className="h-4 w-4" />}
              accentClass="from-accent-cyan to-accent-violet"
            />
            <MetricCard
              label={t("reports.perf.max")}
              value={breakdown?.maxLatencyMs != null ? `${formatNumber(breakdown.maxLatencyMs)} ms` : "—"}
              icon={<Timer className="h-4 w-4" />}
              accentClass="from-accent-amber to-accent-rose"
            />
            <MetricCard
              label={t("reports.perf.charged")}
              value={formatNumber(breakdown?.billableCount ?? 0)}
              icon={<Receipt className="h-4 w-4" />}
              accentClass="from-accent-emerald to-accent-cyan"
            />
            <MetricCard
              label={t("reports.perf.exceptions")}
              value={formatNumber(breakdown?.exceptionCount ?? 0)}
              icon={<Fingerprint className="h-4 w-4" />}
              accentClass="from-accent-violet to-accent-amber"
            />
          </div>

          {/* Fingerprint quality by bank (all clients, same date range) — admin only */}
          {isAdmin && (
          <Card className="mb-6">
            <CardHeader>
              <CardTitle>{t("reports.quality.title")}</CardTitle>
            </CardHeader>
            <CardBody className="p-0">
              <p className="px-4 pt-3 pb-2 text-xs text-text-muted">{t("reports.quality.subtitle")}</p>
              {qualityQ.isLoading ? (
                <PageLoader />
              ) : (qualityQ.data?.length ?? 0) > 0 ? (
                <div className="overflow-x-auto">
                  <Table>
                    <THead>
                      <Tr>
                        <Th>{t("reports.quality.bank")}</Th>
                        <Th>{t("reports.quality.images")}</Th>
                        <Th>{t("reports.quality.scored")}</Th>
                        <Th>{t("reports.quality.avg")}</Th>
                        <Th>{t("reports.quality.lowest")}</Th>
                        <Th>{t("reports.quality.rejected")}</Th>
                        <Th>{t("reports.quality.rejectRate")}</Th>
                        <Th>{t("reports.quality.bands")}</Th>
                      </Tr>
                    </THead>
                    <TBody>
                      {qualityQ.data!.map((row) => (
                        <Tr key={row.tenantId}>
                          <Td className="font-medium">{row.tenantName}</Td>
                          <Td>{formatNumber(row.images)}</Td>
                          <Td>{formatNumber(row.scored)}</Td>
                          <Td className={row.avgNfiq2 != null && row.avgNfiq2 < 40 ? "text-accent-rose" : "text-accent-emerald"}>
                            {row.avgNfiq2 ?? "—"}
                          </Td>
                          <Td>{row.minNfiq2 ?? "—"}</Td>
                          <Td className="text-accent-rose">{formatNumber(row.rejected)}</Td>
                          <Td>{Math.round(row.rejectRate * 100)}%</Td>
                          <Td>
                            <div className="flex items-end gap-0.5 h-6" dir="ltr" title="0–19 · 20–39 · 40–59 · 60–79 · 80–100">
                              {row.buckets.map((n, i) => {
                                const max = Math.max(1, ...row.buckets);
                                return (
                                  <div
                                    key={i}
                                    className={i < 2 ? "w-3 rounded-sm bg-accent-rose/70" : "w-3 rounded-sm bg-accent-emerald/70"}
                                    style={{ height: `${Math.max(2, (n / max) * 24)}px` }}
                                    title={`${n}`}
                                  />
                                );
                              })}
                            </div>
                          </Td>
                        </Tr>
                      ))}
                    </TBody>
                  </Table>
                </div>
              ) : (
                <div className="py-8 text-center text-xs text-text-muted">{t("reports.quality.noData")}</div>
              )}
            </CardBody>
          </Card>
          )}

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
                      <Th>{isAdmin ? t("reports.revenue") : t("reports.amount")}</Th>
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
        </>
      ) : (
          <Card>
            <CardHeader>
              <CardTitle>{t("reports.detail.title")}</CardTitle>
            </CardHeader>
            <CardBody className="p-0">
              <p className="px-4 pt-3 pb-2 text-xs text-text-muted">{isAdmin ? t("reports.detail.subtitle") : t("reports.detail.subtitleTenant")}</p>
              {detailsQ.isLoading ? (
                <PageLoader />
              ) : (detailsQ.data?.content.length ?? 0) > 0 ? (
                <>
                  <div className="overflow-x-auto">
                    <Table>
                      <THead>
                        <Tr>
                          <Th>{t("reports.detail.time")}</Th>
                          <Th>{t("reports.detail.transaction")}</Th>
                          <Th>{t("reports.detail.type")}</Th>
                          <Th>{t("common.status")}</Th>
                          <Th>{t("reports.detail.code")}</Th>
                          <Th>{t("reports.detail.device")}</Th>
                          <Th className="whitespace-nowrap">{t("reports.detail.nfiq2")}</Th>
                          <Th>{t("reports.detail.latency")}</Th>
                          <Th>{t("reports.detail.amount")}</Th>
                        </Tr>
                      </THead>
                      <TBody>
                        {detailsQ.data!.content.map((row) => (
                          <Tr
                            key={row.transactionId}
                            onClick={isAdmin ? () => navigate(`/transactions/${row.transactionId}`) : undefined}
                            className={isAdmin ? "cursor-pointer" : undefined}
                          >
                            <Td className="text-xs text-text-muted whitespace-nowrap">{formatDate(row.createdAt)}</Td>
                            <Td className="font-mono text-xs" title={row.transactionId}>
                              {shortId(row.transactionId, 8)}
                            </Td>
                            <Td className="text-xs">
                              {row.type === "EXCEPTION" ? (
                                <Badge tone="violet">
                                  {t("reports.detail.exception")}
                                  {row.exceptionReason
                                    ? ` · ${t(`exceptionReason.${row.exceptionReason}`, row.exceptionReason)}`
                                    : ""}
                                </Badge>
                              ) : (
                                t("reports.detail.fingerprint")
                              )}
                            </Td>
                            <Td>
                              <div className="flex flex-col items-start gap-1">
                                <Badge tone={statusTone(row.status)}>{t(`status.${row.status}`, row.status)}</Badge>
                                {row.verdict && (
                                  <Badge tone={verdictTone(row.verdict)}>{t(`verdict.${row.verdict}`, row.verdict)}</Badge>
                                )}
                              </div>
                            </Td>
                            <Td className="text-xs">
                              {row.errorCode != null ? (
                                <>
                                  <span className="font-mono text-accent-rose">{row.errorCode}</span>
                                  {row.errorMessage && (
                                    <div className="text-text-muted line-clamp-1 max-w-[16rem]" title={row.errorMessage}>
                                      {row.errorMessage}
                                    </div>
                                  )}
                                </>
                              ) : (
                                <span className="text-text-dim">—</span>
                              )}
                            </Td>
                            <Td className="font-mono text-xs whitespace-nowrap">{row.deviceId ?? "—"}</Td>
                            <Td className="tabular-nums text-xs">{row.imageNfiq2 ?? "—"}</Td>
                            <Td className="tabular-nums text-xs text-text-muted whitespace-nowrap">
                              {row.latencyMs != null ? `${row.latencyMs} ms` : "—"}
                            </Td>
                            <Td className="text-xs whitespace-nowrap">
                              {row.amountMinor != null ? formatMoneyMinor(row.amountMinor, row.currency || "YER") : "—"}
                            </Td>
                          </Tr>
                        ))}
                      </TBody>
                    </Table>
                  </div>
                  <div className="px-4 py-3 border-t border-border/10 flex items-center justify-between text-xs text-text-muted">
                    <div>
                      {t("common.page")} {page + 1} {t("common.of")} {totalPages || 1} ·{" "}
                      {formatNumber(detailsQ.data!.totalElements)} {t("common.total")}
                    </div>
                    <div className="flex items-center gap-1">
                      <Button
                        size="sm"
                        variant="ghost"
                        disabled={page === 0}
                        onClick={() => setPage((p) => Math.max(0, p - 1))}
                        leftIcon={<ChevronLeft className="h-3.5 w-3.5 rtl-flip" />}
                      >
                        {t("common.prev")}
                      </Button>
                      <Button
                        size="sm"
                        variant="ghost"
                        disabled={page + 1 >= totalPages}
                        onClick={() => setPage((p) => p + 1)}
                        rightIcon={<ChevronRight className="h-3.5 w-3.5 rtl-flip" />}
                      >
                        {t("common.next")}
                      </Button>
                    </div>
                  </div>
                </>
              ) : (
                <EmptyState
                  icon={<ScrollText className="h-5 w-5" />}
                  title={t("reports.noData")}
                  description={t("reports.adjustFilters")}
                />
              )}
            </CardBody>
          </Card>
      )}
    </div>
  );
}
