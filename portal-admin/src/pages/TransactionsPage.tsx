import { useEffect, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { ScrollText, ChevronLeft, ChevronRight } from "lucide-react";
import { useTranslation } from "react-i18next";
import { listTenants } from "@/api/tenants";
import { listTransactions } from "@/api/transactions";
import { PageHeader } from "@/components/ui/PageHeader";
import {
  Card,
  CardBody,
  CardHeader,
  CardTitle,
} from "@/components/ui/Card";
import { Table, TBody, THead, Th, Td, Tr } from "@/components/ui/Table";
import { Badge, statusTone } from "@/components/ui/Badge";
import { Select } from "@/components/ui/Select";
import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { PageLoader } from "@/components/ui/Spinner";
import { formatDate, formatMoneyMinor, shortId } from "@/lib/format";

export function TransactionsPage() {
  const { t } = useTranslation();
  const tenantsQ = useQuery({ queryKey: ["tenants"], queryFn: listTenants });
  const [tenantId, setTenantId] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const size = 50;

  useEffect(() => {
    if (!tenantId && tenantsQ.data?.length) {
      setTenantId(tenantsQ.data[0].id);
    }
  }, [tenantId, tenantsQ.data]);

  const txQ = useQuery({
    queryKey: ["transactions", tenantId, page, size],
    queryFn: () => listTransactions({ tenantId: tenantId!, page, size }),
    enabled: !!tenantId,
  });

  const totalPages = txQ.data?.totalPages ?? 0;

  return (
    <div>
      <PageHeader
        title={t("transactions.title")}
        description={t("transactions.subtitle")}
      />

      <Card>
        <CardHeader>
          <CardTitle>{t("common.filter")}</CardTitle>
          <div className="w-72">
            <Select
              value={tenantId}
              onChange={(v) => {
                setTenantId(v);
                setPage(0);
              }}
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
        </CardHeader>
        <CardBody className="p-0">
          {txQ.isLoading ? (
            <PageLoader />
          ) : txQ.data?.content.length ? (
            <>
              <Table>
                <THead>
                  <Tr>
                    <Th>{t("transactions.fields.transactionId")}</Th>
                    <Th>{t("common.status")}</Th>
                    <Th>{t("transactions.fields.latency")}</Th>
                    <Th>{t("transactions.fields.price")}</Th>
                    <Th>{t("transactions.fields.billable")}</Th>
                    <Th>{t("common.createdAt")}</Th>
                  </Tr>
                </THead>
                <TBody>
                  {txQ.data.content.map((tx) => (
                    <Tr key={tx.id}>
                      <Td>
                        <div className="font-mono text-xs text-text">
                          {shortId(tx.id, 14)}
                        </div>
                        {tx.errorMessage && (
                          <div className="text-xs text-accent-rose mt-0.5 line-clamp-1">
                            {tx.errorMessage}
                          </div>
                        )}
                      </Td>
                      <Td>
                        <Badge tone={statusTone(tx.status)}>
                          {t(`status.${tx.status}`, tx.status)}
                        </Badge>
                      </Td>
                      <Td className="text-text-muted text-xs">
                        {tx.latencyMs != null ? `${tx.latencyMs} ms` : "—"}
                      </Td>
                      <Td className="text-text-muted text-xs">
                        {tx.unitPriceMinor != null && tx.currency
                          ? formatMoneyMinor(tx.unitPriceMinor, tx.currency)
                          : "—"}
                      </Td>
                      <Td>
                        {tx.billable ? (
                          <Badge tone="emerald">{t("common.yes")}</Badge>
                        ) : (
                          <Badge tone="neutral">{t("common.no")}</Badge>
                        )}
                      </Td>
                      <Td className="text-text-muted text-xs">
                        {formatDate(tx.createdAt)}
                      </Td>
                    </Tr>
                  ))}
                </TBody>
              </Table>
              <div className="px-4 py-3 border-t border-border/10 flex items-center justify-between text-xs text-text-muted">
                <div>
                  {t("common.page")} {page + 1} {t("common.of")}{" "}
                  {totalPages || 1} · {txQ.data.totalElements}{" "}
                  {t("common.total")}
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
              title={t("transactions.empty.title")}
              description={t("transactions.empty.description")}
            />
          )}
        </CardBody>
      </Card>
    </div>
  );
}
