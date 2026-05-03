import { useParams, Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import {
  ArrowLeft,
  ChevronDown,
  ChevronRight,
  AlertCircle,
} from "lucide-react";
import { useTranslation } from "react-i18next";
import { getTransaction } from "@/api/transactions";
import { listMoiCalls, getMoiCall } from "@/api/moi";
import { PageHeader } from "@/components/ui/PageHeader";
import {
  Card,
  CardBody,
  CardHeader,
  CardTitle,
} from "@/components/ui/Card";
import { Badge, statusTone } from "@/components/ui/Badge";
import { CopyButton } from "@/components/ui/CopyButton";
import { PageLoader } from "@/components/ui/Spinner";
import { formatDate, formatMoneyMinor, shortId } from "@/lib/format";
import type { MoiApiCallSummary } from "@/types/api";

/** Pretty-print JSON; pass strings through if they're not parseable. */
function prettyJson(raw: string | null | undefined): string {
  if (!raw) return "";
  try {
    return JSON.stringify(JSON.parse(raw), null, 2);
  } catch {
    return raw;
  }
}

/** Small label/value row used inside the summary card. */
function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex flex-col gap-0.5">
      <span className="text-[11px] uppercase tracking-wide text-text-muted">
        {label}
      </span>
      <span className="text-sm text-text">{children}</span>
    </div>
  );
}

/** Collapsible JSON viewer with a copy button. */
function JsonBlock({
  title,
  body,
  emptyLabel,
}: {
  title: string;
  body: string | null | undefined;
  emptyLabel: string;
}) {
  const pretty = prettyJson(body);
  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between gap-2">
          <CardTitle>{title}</CardTitle>
          {pretty && <CopyButton value={pretty} />}
        </div>
      </CardHeader>
      <CardBody>
        {pretty ? (
          <pre className="text-xs leading-relaxed whitespace-pre-wrap break-all bg-surface-2 rounded-md p-3 max-h-96 overflow-auto font-mono text-text">
            {pretty}
          </pre>
        ) : (
          <div className="text-xs text-text-muted italic">{emptyLabel}</div>
        )}
      </CardBody>
    </Card>
  );
}

/**
 * One row in the MOI calls list. Click to expand and fetch the full audit
 * row (including headers + bodies). The summary endpoint already gives
 * status + latency, so the row is useful even before expansion.
 */
function MoiCallRow({ call }: { call: MoiApiCallSummary }) {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);

  const detailQ = useQuery({
    queryKey: ["moi-call", call.id],
    queryFn: () => getMoiCall(call.id),
    enabled: open,
  });

  const ok = call.httpStatus != null && call.httpStatus >= 200 && call.httpStatus < 300;

  return (
    <div className="border border-border/10 rounded-md bg-surface">
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        className="w-full flex items-center gap-3 px-3 py-2 text-left hover:bg-surface-2 transition-colors"
      >
        {open ? (
          <ChevronDown className="h-4 w-4 text-text-muted shrink-0" />
        ) : (
          <ChevronRight className="h-4 w-4 text-text-muted shrink-0 rtl-flip" />
        )}
        <Badge tone={call.kind === "AUTH" ? "cyan" : "violet"}>
          {call.kind}
        </Badge>
        <Badge tone={ok ? "emerald" : "rose"}>
          {call.httpStatus != null ? `HTTP ${call.httpStatus}` : t("moi.calls.failed")}
        </Badge>
        <span className="text-xs text-text-muted">
          {call.durationMs != null ? `${call.durationMs} ms` : "—"}
        </span>
        <span className="text-xs text-text-muted truncate flex-1">
          {call.url}
        </span>
        <span className="text-xs text-text-muted shrink-0">
          {formatDate(call.createdAt)}
        </span>
      </button>

      {call.errorMessage && (
        <div className="px-3 pb-2 -mt-1 text-xs text-accent-rose flex items-start gap-1.5">
          <AlertCircle className="h-3.5 w-3.5 mt-0.5 shrink-0" />
          <span className="break-words">{call.errorMessage}</span>
        </div>
      )}

      {open && (
        <div className="border-t border-border/10 p-3 space-y-3 bg-surface-2/30">
          {detailQ.isLoading && <div className="text-xs text-text-muted">{t("common.loading")}</div>}
          {detailQ.data && (
            <>
              <div className="grid grid-cols-2 gap-3 text-xs">
                <Field label={t("moi.calls.url")}>
                  <span className="font-mono break-all">{detailQ.data.url}</span>
                </Field>
                <Field label={t("moi.calls.method")}>
                  {detailQ.data.method ?? "POST"}
                </Field>
                {detailQ.data.tokenSnippet && (
                  <Field label={t("moi.calls.tokenSnippet")}>
                    <span className="font-mono">{detailQ.data.tokenSnippet}</span>
                  </Field>
                )}
                {detailQ.data.transactionId && (
                  <Field label={t("transactions.fields.transactionId")}>
                    <span className="font-mono">{shortId(detailQ.data.transactionId, 14)}</span>
                  </Field>
                )}
              </div>
              <JsonBlock
                title={t("moi.calls.requestHeaders")}
                body={detailQ.data.requestHeadersJson}
                emptyLabel={t("moi.calls.empty")}
              />
              <JsonBlock
                title={t("moi.calls.requestBody")}
                body={detailQ.data.requestBodyJson}
                emptyLabel={t("moi.calls.empty")}
              />
              <JsonBlock
                title={t("moi.calls.responseHeaders")}
                body={detailQ.data.responseHeadersJson}
                emptyLabel={t("moi.calls.empty")}
              />
              <JsonBlock
                title={t("moi.calls.responseBody")}
                body={detailQ.data.responseBodyJson}
                emptyLabel={t("moi.calls.empty")}
              />
            </>
          )}
        </div>
      )}
    </div>
  );
}

export function TransactionDetailPage() {
  const { t } = useTranslation();
  const { id = "" } = useParams();

  const txQ = useQuery({
    queryKey: ["transaction", id],
    queryFn: () => getTransaction(id),
    enabled: !!id,
  });

  const moiQ = useQuery({
    queryKey: ["moi-calls", id],
    queryFn: () => listMoiCalls({ transactionId: id }),
    enabled: !!id,
  });

  if (txQ.isLoading) return <PageLoader />;
  if (!txQ.data) return null;

  const tx = txQ.data;

  return (
    <div>
      <Link
        to="/transactions"
        className="inline-flex items-center gap-1.5 text-sm text-text-muted hover:text-text mb-3"
      >
        <ArrowLeft className="h-4 w-4 rtl-flip" />
        {t("transactions.detail.back")}
      </Link>

      <PageHeader
        title={t("transactions.detail.title")}
        description={
          <span className="font-mono text-xs">{shortId(tx.id, 14)}</span>
        }
      />

      {/* Summary */}
      <Card className="mb-4">
        <CardHeader>
          <CardTitle>{t("transactions.detail.summary")}</CardTitle>
        </CardHeader>
        <CardBody>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <Field label={t("common.status")}>
              <Badge tone={statusTone(tx.status)}>
                {t(`status.${tx.status}`, tx.status)}
              </Badge>
            </Field>
            <Field label={t("transactions.fields.latency")}>
              {tx.latencyMs != null ? `${tx.latencyMs} ms` : "—"}
            </Field>
            <Field label={t("transactions.fields.price")}>
              {tx.unitPriceMinor != null && tx.currency
                ? formatMoneyMinor(tx.unitPriceMinor, tx.currency)
                : "—"}
            </Field>
            <Field label={t("transactions.fields.billable")}>
              {tx.billable ? (
                <Badge tone="emerald">{t("common.yes")}</Badge>
              ) : (
                <Badge tone="neutral">{t("common.no")}</Badge>
              )}
            </Field>
            <Field label={t("common.createdAt")}>{formatDate(tx.createdAt)}</Field>
            <Field label={t("transactions.detail.providerRequestId")}>
              <span className="font-mono text-xs">
                {tx.providerRequestId ?? "—"}
              </span>
            </Field>
            <Field label={t("transactions.detail.tenantId")}>
              <Link
                to={`/tenants/${tx.tenantId}`}
                className="font-mono text-xs text-accent-cyan hover:underline"
              >
                {shortId(tx.tenantId, 12)}
              </Link>
            </Field>
            {tx.subscriptionId && (
              <Field label={t("transactions.detail.subscriptionId")}>
                <span className="font-mono text-xs">
                  {shortId(tx.subscriptionId, 12)}
                </span>
              </Field>
            )}
          </div>
          {tx.errorMessage && (
            <div className="mt-4 rounded-md border border-accent-rose/40 bg-accent-rose/5 p-3 text-sm text-accent-rose flex items-start gap-2">
              <AlertCircle className="h-4 w-4 mt-0.5 shrink-0" />
              <div>
                <div className="font-medium">
                  {t("transactions.detail.error")}
                  {tx.errorCode != null ? ` · ${tx.errorCode}` : ""}
                </div>
                <div className="text-xs mt-0.5 break-words">{tx.errorMessage}</div>
              </div>
            </div>
          )}
        </CardBody>
      </Card>

      {/* Bank-facing payloads */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 mb-4">
        <JsonBlock
          title={t("transactions.detail.bankRequest")}
          body={tx.tenantRequestJson}
          emptyLabel={t("transactions.detail.noPayload")}
        />
        <JsonBlock
          title={t("transactions.detail.bankResponse")}
          body={tx.tenantResponseJson}
          emptyLabel={t("transactions.detail.noPayload")}
        />
      </div>

      {/* Provider-facing payloads (canonical, before projection) */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 mb-4">
        <JsonBlock
          title={t("transactions.detail.providerRequest")}
          body={tx.providerRequestJson}
          emptyLabel={t("transactions.detail.noPayload")}
        />
        <JsonBlock
          title={t("transactions.detail.providerResponse")}
          body={tx.providerResponseJson}
          emptyLabel={t("transactions.detail.noPayload")}
        />
      </div>

      {/* MOI raw HTTP audit (per round-trip) */}
      <Card>
        <CardHeader>
          <CardTitle>{t("transactions.detail.moiCalls")}</CardTitle>
          <span className="text-xs text-text-muted">
            {moiQ.data ? moiQ.data.length : 0}
          </span>
        </CardHeader>
        <CardBody className="space-y-2">
          {moiQ.isLoading && <div className="text-xs text-text-muted">{t("common.loading")}</div>}
          {moiQ.data?.length === 0 && (
            <div className="text-sm text-text-muted italic">
              {t("transactions.detail.noMoiCalls")}
            </div>
          )}
          {moiQ.data?.map((call) => (
            <MoiCallRow key={call.id} call={call} />
          ))}
        </CardBody>
      </Card>
    </div>
  );
}
