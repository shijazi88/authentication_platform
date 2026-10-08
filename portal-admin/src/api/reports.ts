import i18n from "@/i18n";
import { useAuth } from "@/lib/auth";
import { api } from "@/lib/api";
import { useTenantAuth } from "@/lib/tenantAuth";
import { tenantApi } from "@/lib/tenantApi";
import type { ImageQualityRow, Page, ReportDetailRow, ReportGroupBy, ReportSummary } from "@/types/api";

export type StatusFilter = "ALL" | "SUCCESS" | "FAILED";

/** What an export contains: a daily / monthly summary, or one row per transaction. */
export type ReportKind = ReportGroupBy | "details";

export interface ReportParams {
  /** Admin only — the bank portal is always scoped to the signed-in bank. */
  tenantId?: string;
  from: string;
  to: string;
  status?: StatusFilter;
}

/**
 * The report calls the Reports page needs. Two implementations: the admin
 * portal (any bank, `/admin/reports`) and the bank's own portal (`/portal-api/reports`).
 */
export interface ReportsApi {
  summary(groupBy: ReportGroupBy, params: ReportParams): Promise<ReportSummary>;
  details(params: ReportParams & { page: number; size: number }): Promise<Page<ReportDetailRow>>;
  downloadCsv(kind: ReportKind, params: ReportParams): Promise<void>;
  /** The PDF follows the portal language: Arabic UI → Arabic, right-to-left PDF. */
  downloadPdf(kind: ReportKind, params: ReportParams): Promise<void>;
}

/** Fingerprint-quality figures per bank, inclusive date range (YYYY-MM-DD). Admin only. */
export async function getImageQualityReport(from: string, to: string): Promise<ImageQualityRow[]> {
  const { data } = await api.get<ImageQualityRow[]>("/admin/reports/image-quality", { params: { from, to } });
  return data;
}

/**
 * Fetches a binary blob from an authenticated endpoint and triggers a browser
 * download. Uses the same base URL as the axios instances so dev (relative URLs
 * + Vite proxy) and prod (VITE_API_URL) both work without per-call changes.
 */
async function downloadBlob(
  path: string,
  token: string | null,
  params: ReportParams,
  prefix: string,
  ext: string,
  extra: Record<string, string> = {},
): Promise<void> {
  const base = (import.meta.env.VITE_API_URL || "").replace(/\/+$/, "");
  const query: Record<string, string> = {
    from: params.from,
    to: params.to,
    status: params.status ?? "ALL",
    ...extra,
  };
  if (params.tenantId) query.tenantId = params.tenantId;
  const qs = new URLSearchParams(query).toString();

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

function pdfLang(): Record<string, string> {
  return { lang: i18n.language?.startsWith("ar") ? "ar" : "en" };
}

export const adminReportsApi: ReportsApi = {
  async summary(groupBy, params) {
    const { data } = await api.get<ReportSummary>(`/admin/reports/transactions/${groupBy}`, { params });
    return data;
  },
  async details(params) {
    const { data } = await api.get<Page<ReportDetailRow>>("/admin/reports/transactions/details", { params });
    return data;
  },
  downloadCsv: (kind, params) =>
    downloadBlob(`/admin/reports/transactions/${kind}/export.csv`, useAuth.getState().token, params,
      FILE_PREFIX[kind], "csv"),
  downloadPdf: (kind, params) =>
    downloadBlob(`/admin/reports/transactions/${kind}/export.pdf`, useAuth.getState().token, params,
      FILE_PREFIX[kind], "pdf", pdfLang()),
};

export const tenantReportsApi: ReportsApi = {
  async summary(groupBy, { from, to, status }) {
    const { data } = await tenantApi.get<ReportSummary>(`/portal-api/reports/transactions/${groupBy}`, {
      params: { from, to, status },
    });
    return data;
  },
  async details({ from, to, status, page, size }) {
    const { data } = await tenantApi.get<Page<ReportDetailRow>>("/portal-api/reports/transactions/details", {
      params: { from, to, status, page, size },
    });
    return data;
  },
  downloadCsv: (kind, { from, to, status }) =>
    downloadBlob(`/portal-api/reports/transactions/${kind}/export.csv`, useTenantAuth.getState().token,
      { from, to, status }, FILE_PREFIX[kind], "csv"),
  downloadPdf: (kind, { from, to, status }) =>
    downloadBlob(`/portal-api/reports/transactions/${kind}/export.pdf`, useTenantAuth.getState().token,
      { from, to, status }, FILE_PREFIX[kind], "pdf", pdfLang()),
};
