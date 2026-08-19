import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { ShieldAlert, ChevronLeft, ChevronRight, X } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { listTransactions } from "@/api/transactions";
import { listTenants } from "@/api/tenants";
import { PageHeader } from "@/components/ui/PageHeader";
import { Card, CardBody } from "@/components/ui/Card";
import { Table, TBody, THead, Th, Td, Tr } from "@/components/ui/Table";
import { Badge, statusTone } from "@/components/ui/Badge";
import { Select } from "@/components/ui/Select";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { PageLoader } from "@/components/ui/Spinner";
import { PinGate } from "@/components/PinGate";
import { cn } from "@/lib/cn";
import { formatDate, shortId } from "@/lib/format";

const EXCEPTION_REASONS = ["HAND_INJURY", "AMPUTATION", "WORN_PRINTS", "MEDICAL", "OTHER"];

export function SettingsPage() {
  const { t } = useTranslation();
  const [tab, setTab] = useState("exceptions");

  const tabs = [
    { key: "exceptions", label: t("settings.tabs.exceptions"), icon: ShieldAlert },
  ];

  return (
    <div>
      <PageHeader title={t("settings.title")} description={t("settings.subtitle")} />

      {/* Tab bar */}
      <div className="mb-6 flex items-center gap-1 border-b border-border/10">
        {tabs.map(({ key, label, icon: Icon }) => (
          <button
            key={key}
            type="button"
            onClick={() => setTab(key)}
            className={cn(
              "relative flex items-center gap-2 px-4 py-2.5 text-sm font-medium transition-colors -mb-px border-b-2",
              tab === key
                ? "text-text border-accent-cyan"
                : "text-text-muted border-transparent hover:text-text",
            )}
          >
            <Icon className="h-4 w-4" />
            {label}
          </button>
        ))}
      </div>

      {tab === "exceptions" && (
        <PinGate>
          <ExceptionLookup />
        </PinGate>
      )}
    </div>
  );
}

/** Oversight view of fingerprint-exception verifications (biometric-exempt). */
function ExceptionLookup() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const tenantsQ = useQuery({ queryKey: ["tenants"], queryFn: listTenants });
  const [tenantId, setTenantId] = useState<string | null>(null);
  const [reason, setReason] = useState<string | null>(null);
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [page, setPage] = useState(0);
  const size = 50;

  function onChange<T>(setter: (v: T) => void) {
    return (v: T) => {
      setter(v);
      setPage(0);
    };
  }

  const hasFilters = tenantId !== null || reason !== null || from !== "" || to !== "";
  function clearFilters() {
    setTenantId(null);
    setReason(null);
    setFrom("");
    setTo("");
    setPage(0);
  }

  const txQ = useQuery({
    queryKey: ["exception-lookup", tenantId ?? "all", from, to, page, size],
    queryFn: () =>
      listTransactions({
        exception: true,
        tenantId: tenantId ?? undefined,
        from: from || undefined,
        to: to || undefined,
        page,
        size,
      }),
  });

  const tenantById = new Map(tenantsQ.data?.map((tn) => [tn.id, tn]) ?? []);
  // Reason is filtered client-side within the fetched page (exceptions are low-volume).
  const rows = (txQ.data?.content ?? []).filter(
    (tx) => !reason || tx.exceptionReason === reason,
  );
  const totalPages = txQ.data?.totalPages ?? 0;

  return (
    <Card>
      <div className="p-4 border-b border-border/10 flex flex-wrap items-end gap-3">
        <div className="w-60">
          <Label>{t("subscriptions.fields.tenant")}</Label>
          <Select
            value={tenantId ?? "__all__"}
            onChange={onChange((v) => setTenantId(v === "__all__" ? null : v))}
            options={[
              { value: "__all__", label: t("transactions.allClients") },
              ...(tenantsQ.data?.map((tenant) => ({
                value: tenant.id,
                label: tenant.legalName,
                description: tenant.code,
              })) ?? []),
            ]}
          />
        </div>
        <div className="w-52">
          <Label>{t("settings.exceptions.reason")}</Label>
          <Select
            value={reason ?? "__all__"}
            onChange={onChange((v) => setReason(v === "__all__" ? null : v))}
            options={[
              { value: "__all__", label: t("settings.exceptions.allReasons") },
              ...EXCEPTION_REASONS.map((r) => ({
                value: r,
                label: t(`exceptionReason.${r}`, r),
              })),
            ]}
          />
        </div>
        <div>
          <Label htmlFor="from">{t("transactions.fromDate")}</Label>
          <Input id="from" type="date" value={from} max={to || undefined}
            onChange={(e) => onChange(setFrom)(e.target.value)} />
        </div>
        <div>
          <Label htmlFor="to">{t("transactions.toDate")}</Label>
          <Input id="to" type="date" value={to} min={from || undefined}
            onChange={(e) => onChange(setTo)(e.target.value)} />
        </div>
        {hasFilters && (
          <Button variant="ghost" size="sm" leftIcon={<X className="h-3.5 w-3.5" />} onClick={clearFilters}>
            {t("transactions.clearFilters")}
          </Button>
        )}
      </div>

      <CardBody className="p-0">
        {txQ.isLoading ? (
          <PageLoader />
        ) : rows.length ? (
          <>
            <Table>
              <THead>
                <Tr>
                  <Th>{t("subscriptions.fields.tenant")}</Th>
                  <Th>{t("settings.exceptions.reason")}</Th>
                  <Th>{t("settings.exceptions.note")}</Th>
                  <Th>{t("common.status")}</Th>
                  <Th>{t("transactions.fields.transactionId")}</Th>
                  <Th>{t("common.createdAt")}</Th>
                </Tr>
              </THead>
              <TBody>
                {rows.map((tx) => (
                  <Tr key={tx.id} onClick={() => navigate(`/transactions/${tx.id}`)} className="cursor-pointer">
                    <Td className="text-xs">
                      {tenantById.get(tx.tenantId)?.legalName ?? (
                        <span className="font-mono text-text-muted">{shortId(tx.tenantId, 8)}</span>
                      )}
                    </Td>
                    <Td>
                      <Badge tone="violet">
                        {tx.exceptionReason ? t(`exceptionReason.${tx.exceptionReason}`, tx.exceptionReason) : "—"}
                      </Badge>
                    </Td>
                    <Td className="text-xs text-text-muted max-w-[240px] truncate" title={tx.exceptionNote ?? undefined}>
                      {tx.exceptionNote || "—"}
                    </Td>
                    <Td>
                      <Badge tone={statusTone(tx.status)}>{t(`status.${tx.status}`, tx.status)}</Badge>
                    </Td>
                    <Td className="font-mono text-xs text-text-muted break-all">{shortId(tx.id, 10)}</Td>
                    <Td className="text-text-muted text-xs">{formatDate(tx.createdAt)}</Td>
                  </Tr>
                ))}
              </TBody>
            </Table>
            <div className="px-4 py-3 border-t border-border/10 flex items-center justify-between text-xs text-text-muted">
              <div>
                {t("common.page")} {page + 1} {t("common.of")} {totalPages || 1} · {txQ.data?.totalElements}{" "}
                {t("settings.exceptions.total")}
              </div>
              <div className="flex items-center gap-1">
                <Button size="sm" variant="ghost" disabled={page === 0} onClick={() => setPage((p) => Math.max(0, p - 1))}
                  leftIcon={<ChevronLeft className="h-3.5 w-3.5 rtl-flip" />}>
                  {t("common.prev")}
                </Button>
                <Button size="sm" variant="ghost" disabled={page + 1 >= totalPages} onClick={() => setPage((p) => p + 1)}
                  rightIcon={<ChevronRight className="h-3.5 w-3.5 rtl-flip" />}>
                  {t("common.next")}
                </Button>
              </div>
            </div>
          </>
        ) : (
          <EmptyState
            icon={<ShieldAlert className="h-5 w-5" />}
            title={t("settings.exceptions.empty.title")}
            description={t("settings.exceptions.empty.description")}
          />
        )}
      </CardBody>
    </Card>
  );
}
