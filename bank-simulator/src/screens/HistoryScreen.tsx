import { useState } from "react";
import {
  History,
  Trash2,
  CheckCircle2,
  XCircle,
  ChevronDown,
  ChevronRight,
  Copy,
  ArrowRight,
  ArrowLeft,
  Clock,
  Hash,
  Download,
} from "lucide-react";
import { toast } from "sonner";
import { useTranslation } from "react-i18next";
import { useApp } from "../store";
import { FINGER_LABELS, type HistoryEntry } from "../types";
import { cn } from "../lib/cn";
import { historyToCsv, downloadCsv } from "../lib/csv";

export function HistoryScreen() {
  const history = useApp((s) => s.history);
  const clearHistory = useApp((s) => s.clearHistory);
  const [expanded, setExpanded] = useState<string | null>(null);
  const { t } = useTranslation();

  const exportCsv = () => {
    if (history.length === 0) return;
    const csv = historyToCsv(history);
    const stamp = new Date().toISOString().replace(/[:.]/g, "-").slice(0, 19);
    downloadCsv(`sanad-history-${stamp}.csv`, csv);
    toast.success(t("history.exportedToast", { n: history.length }));
  };

  return (
    <div className="max-w-5xl mx-auto p-8 space-y-6">
      <header className="flex items-end justify-between">
        <div>
          <h1 className="text-2xl font-extrabold text-ink flex items-center gap-2">
            <History className="h-6 w-6 text-moi-blue" />
            {t("history.title")}
          </h1>
          <p className="text-sm text-ink-muted mt-1">{t("history.subtitle")}</p>
        </div>
        {history.length > 0 && (
          <div className="flex items-center gap-2">
            <button onClick={exportCsv} className="btn-secondary">
              <Download className="h-4 w-4" /> {t("history.export")}
            </button>
            <button
              onClick={() => {
                if (confirm(t("history.clearConfirm"))) {
                  clearHistory();
                  setExpanded(null);
                }
              }}
              className="btn-ghost text-moi-red hover:text-moi-red"
            >
              <Trash2 className="h-4 w-4" /> {t("history.clear")}
            </button>
          </div>
        )}
      </header>

      {history.length === 0 ? (
        <div className="card p-12 text-center">
          <History className="h-10 w-10 text-ink-dim mx-auto" />
          <p className="text-ink-muted mt-3">{t("history.empty")}</p>
          <p className="text-xs text-ink-dim mt-1">{t("history.emptyHint")}</p>
        </div>
      ) : (
        <div className="card overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-paper-raised text-xs font-bold text-ink-muted uppercase tracking-wider">
              <tr>
                <th className="w-8" />
                <th className="text-start px-4 py-3">{t("history.status")}</th>
                <th className="text-start px-4 py-3">{t("history.nationalNumber")}</th>
                <th className="text-start px-4 py-3">{t("history.finger")}</th>
                <th className="text-start px-4 py-3">{t("history.http")}</th>
                <th className="text-start px-4 py-3">{t("history.duration")}</th>
                <th className="text-start px-4 py-3">{t("history.time")}</th>
              </tr>
            </thead>
            <tbody>
              {history.map((e) => {
                const isOpen = expanded === e.id;
                return (
                  <>
                    <tr
                      key={e.id}
                      onClick={() => setExpanded(isOpen ? null : e.id)}
                      className={cn(
                        "border-t border-ink-faint/60 cursor-pointer transition",
                        isOpen ? "bg-moi-blue/5" : "hover:bg-paper-soft",
                      )}
                    >
                      <td className="px-2 text-ink-muted">
                        {isOpen ? (
                          <ChevronDown className="h-4 w-4" />
                        ) : (
                          <ChevronRight className="h-4 w-4" />
                        )}
                      </td>
                      <td className="px-4 py-3">
                        {e.success ? (
                          <span className="inline-flex items-center gap-1.5 text-moi-green font-semibold">
                            <CheckCircle2 className="h-4 w-4" />
                            {e.verificationStatus ?? "OK"}
                          </span>
                        ) : (
                          <span
                            className="inline-flex items-center gap-1.5 text-moi-red font-semibold"
                            title={e.error}
                          >
                            <XCircle className="h-4 w-4" />
                            {t("history.failed")}
                          </span>
                        )}
                      </td>
                      <td className="px-4 py-3 font-mono">{e.nationalNumber}</td>
                      <td className="px-4 py-3 text-ink-muted">
                        {e.fingerPosition
                          ? `${e.fingerPosition} · ${FINGER_LABELS[e.fingerPosition]}`
                          : "—"}
                      </td>
                      <td className="px-4 py-3 font-mono text-ink-muted">
                        {e.httpStatus || "—"}
                      </td>
                      <td className="px-4 py-3 text-ink-muted font-mono text-xs">
                        {e.durationMs != null ? `${e.durationMs}ms` : "—"}
                      </td>
                      <td className="px-4 py-3 text-ink-muted text-xs">
                        {new Date(e.ts).toLocaleString()}
                      </td>
                    </tr>
                    {isOpen && (
                      <tr key={`${e.id}-detail`} className="bg-paper-soft">
                        <td colSpan={7} className="p-5 border-t border-moi-blue/20">
                          <DetailPanel entry={e} />
                        </td>
                      </tr>
                    )}
                  </>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

function DetailPanel({ entry }: { entry: HistoryEntry }) {
  const { t } = useTranslation();
  const copy = (obj: unknown, label: string) => {
    const text = typeof obj === "string" ? obj : JSON.stringify(obj, null, 2);
    navigator.clipboard.writeText(text);
    toast.success(`${label} ${t("result.copied").toLowerCase()}`);
  };

  return (
    <div className="space-y-4">
      {/* Meta strip */}
      <div className="flex items-center flex-wrap gap-x-5 gap-y-2 text-[11px] text-ink-muted">
        <div className="flex items-center gap-1.5">
          <Hash className="h-3.5 w-3.5 text-ink-dim" />
          <span className="font-bold text-ink-dim uppercase tracking-wider">{t("history.txn")}</span>
          <span className="font-mono text-ink">{entry.id}</span>
        </div>
        <div className="flex items-center gap-1.5">
          <Clock className="h-3.5 w-3.5 text-ink-dim" />
          <span className="font-bold text-ink-dim uppercase tracking-wider">{t("history.endpoint")}</span>
          <span className="font-mono text-ink">{entry.endpoint}</span>
        </div>
        <div className="flex items-center gap-1.5">
          <span className="font-bold text-ink-dim uppercase tracking-wider">{t("history.host")}</span>
          <span className="font-mono text-ink">{entry.baseUrl}</span>
        </div>
      </div>

      <div className="grid lg:grid-cols-2 gap-4">
        {/* Request */}
        <Panel
          title={t("history.request")}
          subtitle={t("history.sentToSanad")}
          direction="out"
          onCopy={() => copy(entry.requestBody, t("history.request"))}
        >
          <Code json={entry.requestBody} />
        </Panel>

        {/* Response */}
        <Panel
          title={entry.success ? t("history.response") : t("history.errorResponse")}
          subtitle={
            entry.success
              ? `HTTP ${entry.httpStatus ?? "?"} · ${entry.durationMs ?? "?"}ms`
              : entry.error ?? t("history.failed")
          }
          direction="in"
          tone={entry.success ? "ok" : "error"}
          onCopy={() => copy(entry.responseBody ?? entry.error ?? "", t("history.response"))}
        >
          {entry.responseBody !== undefined ? (
            <Code json={entry.responseBody} />
          ) : (
            <div className="text-[11px] font-mono text-ink-muted p-3 bg-paper rounded-md">
              {entry.error ?? t("history.noResponseBody")}
            </div>
          )}
        </Panel>
      </div>
    </div>
  );
}

function Panel({
  title,
  subtitle,
  direction,
  tone = "neutral",
  children,
  onCopy,
}: {
  title: string;
  subtitle?: string;
  direction: "in" | "out";
  tone?: "ok" | "error" | "neutral";
  children: React.ReactNode;
  onCopy: () => void;
}) {
  const ToneIcon = direction === "out" ? ArrowRight : ArrowLeft;
  const toneClasses =
    tone === "error"
      ? "text-moi-red"
      : tone === "ok"
      ? "text-moi-green"
      : "text-moi-blue";
  return (
    <div className="card p-0 overflow-hidden">
      <div className="flex items-center justify-between px-3 py-2 border-b border-ink-faint/60 bg-paper-raised/50">
        <div className="flex items-center gap-2">
          <div className={cn("h-5 w-5 rounded-md flex items-center justify-center", toneClasses)}>
            <ToneIcon className="h-3.5 w-3.5" />
          </div>
          <div>
            <div className="text-xs font-bold text-ink">{title}</div>
            {subtitle && <div className="text-[10px] text-ink-dim">{subtitle}</div>}
          </div>
        </div>
        <button
          type="button"
          onClick={onCopy}
          className="btn-ghost !text-[11px] !px-2 !py-1"
        >
          <Copy className="h-3 w-3" /> Copy
        </button>
      </div>
      <div className="p-3">{children}</div>
    </div>
  );
}

function Code({ json }: { json: unknown }) {
  const text = (() => {
    try {
      return JSON.stringify(json, null, 2);
    } catch {
      return String(json);
    }
  })();
  return (
    <pre className="text-[11px] font-mono bg-slate-900 text-slate-200 rounded-md p-3 overflow-auto max-h-80 whitespace-pre-wrap break-all leading-relaxed">
      {text}
    </pre>
  );
}
