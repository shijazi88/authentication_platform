import i18n from "@/i18n";
import { useAuth } from "@/lib/auth";
import { api } from "@/lib/api";
import type { ImageQualityRow, Page, ReportDetailRow, ReportGroupBy, ReportSummary } from "@/types/api";

export type StatusFilter = "ALL" | "SUCCESS" | "FAILED";

/** What an export contains: a daily / monthly summary, or one row per transaction. */
export type ReportKind = ReportGroupBy | "details";

export interface ReportParams {
  tenantId: string;
  from: string;
  to: string;
  status?: StatusFilter;
}

export async function getDailyReport(params: ReportParams): Promise<ReportSummary> {
  const { data } = await api.get<ReportSummary>("/admin/reports/transactions/daily", { params });
  return data;
}

export async function getMonthlyReport(params: ReportParams): Promise<ReportSummary> {
  const { data } = await api.get<ReportSummary>("/admin/reports/transactions/monthly", { params });
  return data;
}

/** One page of the per-transaction report (oldest first); `from`/`to` are inclusive. */
export async function getReportDetails(
  params: ReportParams & { page: number; size: number },
): Promise<Page<ReportDetailRow>> {
  const { data } = await api.get<Page<ReportDetailRow>>("/admin/reports/transactions/details", { params });
  return data;
}

/** Fingerprint-quality figures per bank, inclusive date range (YYYY-MM-DD). */
export async function getImageQualityReport(from: string, to: string): Promise<ImageQualityRow[]> {
  const { data } = await api.get<ImageQualityRow[]>("/admin/reports/image-quality", { params: { from, to } });
  return data;
}

/**
 * Shared helper: fetches a binary blob from an authenticated endpoint and
 * triggers a browser download. Uses the same base URL as the shared axios
 * instance so dev (relative URLs + Vite proxy) and prod (VITE_API_URL) both
 * work without any per-call changes.
 */
async function downloadBlob(
  path: string,
  params: ReportParams,
  prefix: string,
  ext: string,
  extra: Record<string, string> = {},
): Promise<void> {
  const base = (import.meta.env.VITE_API_URL || "").replace(/\/+$/, "");
  const token = useAuth.getState().token;
  const qs = new URLSearchParams({
    tenantId: params.tenantId,
    from: params.from,
    to: params.to,
    status: params.status ?? "ALL",
    ...extra,
  }).toString();

  const response = await fetch(`${base}${path}?${qs}`, {
    method: "GET",
    headers: token ? { Authorization: `Bearer ${token}` } : undefined,
  });
  if (!response.ok) throw new Error(`Export failed: HTTP ${response.status}`);

  const blob = await response.blob();
  const blobUrl = URL.createObjectURL(blob);
  const filename = `${prefix}-${params.from}-to-${params.to}.${ext}`;
  const a = document.createElement("a");
  a.href = blobUrl;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(blobUrl);
}

const FILE_PREFIX: Record<ReportKind, string> = {
  daily: "motabiq-transactions-daily",
  monthly: "motabiq-transactions-monthly",
  details: "motabiq-transaction-details",
};

export function downloadReportCsv(kind: ReportKind, params: ReportParams) {
  return downloadBlob(`/admin/reports/transactions/${kind}/export.csv`, params, FILE_PREFIX[kind], "csv");
}

/** The PDF follows the portal language: Arabic UI → Arabic, right-to-left PDF. */
export function downloadReportPdf(kind: ReportKind, params: ReportParams) {
  const lang = i18n.language?.startsWith("ar") ? "ar" : "en";
  return downloadBlob(`/admin/reports/transactions/${kind}/export.pdf`, params, FILE_PREFIX[kind], "pdf", { lang });
}
