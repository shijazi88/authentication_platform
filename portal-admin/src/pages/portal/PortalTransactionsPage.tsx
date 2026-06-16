import { useState } from "react";
import { useQuery, keepPreviousData } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { listTransactions } from "@/api/tenant";
import { Card } from "@/components/ui/Card";
import { Table, TBody, THead, Th, Td, Tr } from "@/components/ui/Table";
import { Badge, statusTone } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { PageLoader } from "@/components/ui/Spinner";
import { formatMoneyMinor, formatDate } from "@/lib/format";

export function PortalTransactionsPage() {
  const { t } = useTranslation();
  const [page, setPage] = useState(0);
  const size = 20;
  const q = useQuery({
    queryKey: ["t-tx", page, size],
    queryFn: () => listTransactions(page, size),
    placeholderData: keepPreviousData,
  });

  const total = q.data?.totalElements ?? 0;
  const pages = Math.ceil(total / size);

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold">{t("portal.nav.transactions")}</h1>
      <Card>
        {q.isLoading ? (
          <PageLoader />
        ) : q.data?.content?.length ? (
          <>
            <Table>
              <THead>
                <Tr>
                  <Th>{t("common.id")}</Th>
                  <Th>{t("common.status")}</Th>
                  <Th>{t("portal.tx.amount")}</Th>
                  <Th>{t("common.createdAt")}</Th>
                </Tr>
              </THead>
              <TBody>
                {q.data.content.map((tx) => (
                  <Tr key={tx.id}>
                    <Td className="font-mono text-xs">{tx.id}</Td>
                    <Td><Badge tone={statusTone(tx.status)}>{t(`status.${tx.status}`, tx.status)}</Badge></Td>
                    <Td>
                      {tx.unitPriceMinor != null
                        ? formatMoneyMinor(tx.unitPriceMinor, tx.currency ?? "YER")
                        : "—"}
                    </Td>
                    <Td className="text-xs text-text-muted">{formatDate(tx.createdAt)}</Td>
                  </Tr>
                ))}
              </TBody>
            </Table>
            {pages > 1 && (
              <div className="flex items-center justify-between p-3 text-xs text-text-muted">
                <span>{t("common.page")} {page + 1} {t("common.of")} {pages}</span>
                <div className="flex gap-2">
                  <Button variant="ghost" size="sm" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
                    {t("common.prev")}
                  </Button>
                  <Button variant="ghost" size="sm" disabled={page + 1 >= pages} onClick={() => setPage((p) => p + 1)}>
                    {t("common.next")}
                  </Button>
                </div>
              </div>
            )}
          </>
        ) : (
          <div className="px-6 py-10 text-center text-sm text-text-muted">
            {t("portal.dashboard.noTx")}
          </div>
        )}
      </Card>
    </div>
  );
}
