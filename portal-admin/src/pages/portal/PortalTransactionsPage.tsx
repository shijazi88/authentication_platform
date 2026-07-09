import { useState } from "react";
import { useQuery, keepPreviousData } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { X } from "lucide-react";
import { listTransactions } from "@/api/tenant";
import { Card, CardBody } from "@/components/ui/Card";
import { Table, TBody, THead, Th, Td, Tr } from "@/components/ui/Table";
import { Badge, statusTone } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Select } from "@/components/ui/Select";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { CopyButton } from "@/components/ui/CopyButton";
import { PageLoader } from "@/components/ui/Spinner";
import { formatMoneyMinor, formatDate } from "@/lib/format";
import type { TransactionStatus } from "@/types/api";

const STATUSES: TransactionStatus[] = ["SUCCESS", "FAILED", "TIMEOUT", "REJECTED", "INITIATED"];

// MOTABIQ response (error) codes — see the ICD error catalog.
const ERROR_CODES: { code: number; label: string }[] = [
  { code: 1001, label: "1001 · Bad request" },
  { code: 1002, label: "1002 · Validation failed" },
  { code: 1101, label: "1101 · Unauthenticated" },
  { code: 1102, label: "1102 · Invalid credentials" },
  { code: 1201, label: "1201 · Forbidden" },
  { code: 1202, label: "1202 · Entitlement denied" },
  { code: 1203, label: "1203 · PIN unlock required" },
  { code: 1204, label: "1204 · Invalid PIN" },
  { code: 1301, label: "1301 · Not found" },
  { code: 1401, label: "1401 · Conflict" },
  { code: 1402, label: "1402 · Quota exceeded" },
  { code: 1403, label: "1403 · Insufficient funds" },
  { code: 2001, label: "2001 · Internal error" },
  { code: 2101, label: "2101 · Connector error" },
  { code: 2102, label: "2102 · Connector timeout" },
  { code: 2103, label: "2103 · Connector unavailable" },
];
const ERROR_CODE_LABEL = new Map(ERROR_CODES.map((e) => [e.code, e.label]));

export function PortalTransactionsPage() {
  const { t } = useTranslation();
  const [status, setStatus] = useState<TransactionStatus | null>(null);
  const [errorCode, setErrorCode] = useState<number | null>(null);
  const [billable, setBillable] = useState<boolean | null>(null);
  const [q, setQ] = useState("");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [page, setPage] = useState(0);
  const size = 20;

  // Any filter change resets to the first page.
  function onFilterChange<T>(setter: (v: T) => void) {
    return (v: T) => {
      setter(v);
      setPage(0);
    };
  }

  const hasFilters =
    status !== null || errorCode !== null || billable !== null ||
    q !== "" || from !== "" || to !== "";
  function clearFilters() {
    setStatus(null);
    setErrorCode(null);
    setBillable(null);
    setQ("");
    setFrom("");
    setTo("");
    setPage(0);
  }

  const txQ = useQuery({
    queryKey: [
      "t-tx", status ?? "any", errorCode ?? "any", billable ?? "any", q, from, to, page, size,
    ],
    queryFn: () =>
      listTransactions({
        status: status ?? undefined,
        errorCode: errorCode ?? undefined,
        billable: billable ?? undefined,
        q: q || undefined,
        from: from || undefined,
        to: to || undefined,
        page,
        size,
      }),
    placeholderData: keepPreviousData,
  });

  const total = txQ.data?.totalElements ?? 0;
  const pages = Math.ceil(total / size);

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold">{t("portal.nav.transactions")}</h1>

      <Card>
        <div className="p-4 border-b border-border/10 flex flex-wrap items-end gap-3">
          <div className="w-44">
            <Label>{t("common.status")}</Label>
            <Select
              value={status ?? "__all__"}
              onChange={onFilterChange((v) =>
                setStatus(v === "__all__" ? null : (v as TransactionStatus)),
              )}
              options={[
                { value: "__all__", label: t("transactions.allStatuses") },
                ...STATUSES.map((s) => ({ value: s, label: t(`status.${s}`, s) })),
              ]}
            />
          </div>
          <div className="w-56">
            <Label>{t("transactions.responseCode")}</Label>
            <Select
              value={errorCode != null ? String(errorCode) : "__all__"}
              onChange={onFilterChange((v) =>
                setErrorCode(v === "__all__" ? null : Number(v)),
              )}
              options={[
                { value: "__all__", label: t("transactions.allResponseCodes") },
                ...ERROR_CODES.map((e) => ({ value: String(e.code), label: e.label })),
              ]}
            />
          </div>
          <div className="w-36">
            <Label>{t("transactions.fields.billable")}</Label>
            <Select
              value={billable === null ? "__all__" : billable ? "true" : "false"}
              onChange={onFilterChange((v) =>
                setBillable(v === "__all__" ? null : v === "true"),
              )}
              options={[
                { value: "__all__", label: t("transactions.allStatuses") },
                { value: "true", label: t("common.yes") },
                { value: "false", label: t("common.no") },
              ]}
            />
          </div>
          <div className="w-64">
            <Label htmlFor="txid">{t("transactions.fields.transactionId")}</Label>
            <Input
              id="txid"
              placeholder={t("transactions.searchIdPlaceholder")}
              value={q}
              onChange={(e) => onFilterChange(setQ)(e.target.value)}
            />
          </div>
          <div>
            <Label htmlFor="from">{t("transactions.fromDate")}</Label>
            <Input
              id="from"
              type="date"
              value={from}
              max={to || undefined}
              onChange={(e) => onFilterChange(setFrom)(e.target.value)}
            />
          </div>
          <div>
            <Label htmlFor="to">{t("transactions.toDate")}</Label>
            <Input
              id="to"
              type="date"
              value={to}
              min={from || undefined}
              onChange={(e) => onFilterChange(setTo)(e.target.value)}
            />
          </div>
          {hasFilters && (
            <Button
              variant="ghost"
              size="sm"
              leftIcon={<X className="h-3.5 w-3.5" />}
              onClick={clearFilters}
            >
              {t("transactions.clearFilters")}
            </Button>
          )}
        </div>

        <CardBody className="p-0">
          {txQ.isLoading ? (
            <PageLoader />
          ) : txQ.data?.content?.length ? (
            <>
              <Table>
                <THead>
                  <Tr>
                    <Th>{t("transactions.fields.transactionId")}</Th>
                    <Th>{t("common.status")}</Th>
                    <Th>{t("transactions.responseCode")}</Th>
                    <Th>{t("portal.tx.amount")}</Th>
                    <Th>{t("transactions.fields.billable")}</Th>
                    <Th>{t("common.createdAt")}</Th>
                  </Tr>
                </THead>
                <TBody>
                  {txQ.data.content.map((tx) => (
                    <Tr key={tx.id}>
                      <Td>
                        <div className="flex items-center gap-1.5">
                          <span className="font-mono text-xs text-text break-all">{tx.id}</span>
                          <CopyButton value={tx.id} />
                        </div>
                        {tx.errorMessage && (
                          <div className="text-xs text-accent-rose mt-0.5 line-clamp-1">
                            {tx.errorMessage}
                          </div>
                        )}
                      </Td>
                      <Td>
                        <Badge tone={statusTone(tx.status)}>{t(`status.${tx.status}`, tx.status)}</Badge>
                      </Td>
                      <Td className="text-xs">
                        {tx.errorCode != null ? (
                          <span
                            className="font-mono text-accent-rose"
                            title={ERROR_CODE_LABEL.get(tx.errorCode) ?? undefined}
                          >
                            {tx.errorCode}
                          </span>
                        ) : (
                          <span className="text-text-dim">—</span>
                        )}
                      </Td>
                      <Td>
                        {tx.unitPriceMinor != null
                          ? formatMoneyMinor(tx.unitPriceMinor, tx.currency ?? "YER")
                          : "—"}
                      </Td>
                      <Td>
                        {tx.billable ? (
                          <Badge tone="emerald">{t("common.yes")}</Badge>
                        ) : (
                          <Badge tone="neutral">{t("common.no")}</Badge>
                        )}
                      </Td>
                      <Td className="text-xs text-text-muted">{formatDate(tx.createdAt)}</Td>
                    </Tr>
                  ))}
                </TBody>
              </Table>
              <div className="px-4 py-3 border-t border-border/10 flex items-center justify-between text-xs text-text-muted">
                <span>
                  {t("common.page")} {page + 1} {t("common.of")} {pages || 1} · {total} {t("common.total")}
                </span>
                <div className="flex gap-2">
                  <Button variant="ghost" size="sm" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
                    {t("common.prev")}
                  </Button>
                  <Button variant="ghost" size="sm" disabled={page + 1 >= pages} onClick={() => setPage((p) => p + 1)}>
                    {t("common.next")}
                  </Button>
                </div>
              </div>
            </>
          ) : (
            <div className="px-6 py-10 text-center text-sm text-text-muted">
              {hasFilters ? t("transactions.empty.description") : t("portal.dashboard.noTx")}
            </div>
          )}
        </CardBody>
      </Card>
    </div>
  );
}
