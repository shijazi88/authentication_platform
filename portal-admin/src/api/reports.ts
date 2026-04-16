import { useAuth } from "@/lib/auth";
import { api } from "@/lib/api";
import type { ReportGroupBy, ReportSummary } from "@/types/api";

export type StatusFilter = "ALL" | "SUCCESS" | "FAILED";

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

/**
 * Shared helper: fetches a binary blob from an authenticated endpoint and
 * triggers a browser download. Uses the same base URL as the shared axios
 * instance so dev (relative URLs + Vite proxy) and prod (VITE_API_URL) both
 * work without any per-call changes.
 */
async function downloadBlob(path: string, params: ReportParams, ext: string): Promise<void> {
  const base = (import.meta.env.VITE_API_URL || "").replace(/\/+$/, "");
  const token = useAuth.getState().token;
  const qs = new URLSearchParams({
    tenantId: params.tenantId,
    from: params.from,
    to: params.to,
    status: params.status ?? "ALL",
  }).toString();

  const response = await fetch(`${base}${path}?${qs}`, {
    method: "GET",
    headers: token ? { Authorization: `Bearer ${token}` } : undefined,
  });
  if (!response.ok) throw new Error(`Export failed: HTTP ${response.status}`);

  const blob = await response.blob();
  const blobUrl = URL.createObjectURL(blob);
  const filename = `transactions-${params.from}-to-${params.to}.${ext}`;
  const a = document.createElement("a");
  a.href = blobUrl;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(blobUrl);
}

export function downloadReportCsv(groupBy: ReportGroupBy, params: ReportParams) {
  return downloadBlob(`/admin/reports/transactions/${groupBy}/export.csv`, params, "csv");
}

export function downloadReportPdf(groupBy: ReportGroupBy, params: ReportParams) {
  return downloadBlob(`/admin/reports/transactions/${groupBy}/export.pdf`, params, "pdf");
}
