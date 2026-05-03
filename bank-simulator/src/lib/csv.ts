import type { HistoryEntry } from "../types";

function esc(v: unknown): string {
  if (v === null || v === undefined) return "";
  const s = typeof v === "string" ? v : JSON.stringify(v);
  // Excel-safe CSV escaping: wrap in quotes if contains comma/quote/newline.
  if (/[",\n\r]/.test(s)) {
    return `"${s.replace(/"/g, '""')}"`;
  }
  return s;
}

export function historyToCsv(entries: HistoryEntry[]): string {
  const header = [
    "timestamp",
    "transaction_id",
    "national_number",
    "finger_position",
    "success",
    "verification_status",
    "http_status",
    "duration_ms",
    "error",
    "endpoint",
    "base_url",
    "request_json",
    "response_json",
  ].join(",");

  const rows = entries.map((e) =>
    [
      esc(e.ts),
      esc(e.id),
      esc(e.nationalNumber),
      esc(e.fingerPosition ?? ""),
      esc(e.success ? "yes" : "no"),
      esc(e.verificationStatus ?? ""),
      esc(e.httpStatus ?? ""),
      esc(e.durationMs ?? ""),
      esc(e.error ?? ""),
      esc(e.endpoint),
      esc(e.baseUrl),
      esc(e.requestBody),
      esc(e.responseBody),
    ].join(","),
  );

  return [header, ...rows].join("\n");
}

export function downloadCsv(filename: string, content: string) {
  // Prepend UTF-8 BOM so Excel opens non-ASCII (Arabic) correctly.
  const blob = new Blob(["\ufeff" + content], {
    type: "text/csv;charset=utf-8",
  });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
