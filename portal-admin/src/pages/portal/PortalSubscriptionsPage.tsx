import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { Infinity as InfinityIcon } from "lucide-react";
import { listSubscriptions, getSubscriptionDetails } from "@/api/tenant";
import { Card, CardBody, CardHeader, CardTitle } from "@/components/ui/Card";
import { Table, TBody, THead, Th, Td, Tr } from "@/components/ui/Table";
import { Badge, statusTone } from "@/components/ui/Badge";
import { PageLoader } from "@/components/ui/Spinner";
import { formatMoneyMinor, formatNumber } from "@/lib/format";
import type { Subscription } from "@/types/api";

export function PortalSubscriptionsPage() {
  const { t } = useTranslation();
  const q = useQuery({ queryKey: ["t-subs"], queryFn: listSubscriptions });

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold">{t("portal.nav.subscriptions")}</h1>
      {q.isLoading ? (
        <PageLoader />
      ) : q.data?.length ? (
        q.data.map((s) => <SubscriptionDetailCard key={s.id} sub={s} />)
      ) : (
        <Card>
          <CardBody className="px-6 py-10 text-center text-sm text-text-muted">
            {t("portal.subs.empty")}
          </CardBody>
        </Card>
      )}
    </div>
  );
}

function SubscriptionDetailCard({ sub }: { sub: Subscription }) {
  const { t } = useTranslation();
  const d = useQuery({
    queryKey: ["t-sub-detail", sub.id],
    queryFn: () => getSubscriptionDetails(sub.id),
  });

  const unlimited = (
    <span className="inline-flex items-center gap-1 text-text-muted">
      <InfinityIcon className="h-3 w-3" />
      {t("portal.subs.unlimited")}
    </span>
  );

  return (
    <Card>
      <CardHeader>
        <div>
          <CardTitle>{d.data?.planName ?? sub.planId}</CardTitle>
          {d.data && (
            <div className="text-xs text-text-muted mt-0.5">
              <span className="font-mono">{d.data.planCode}</span>
              {" · "}
              {t("portal.subs.baseFee")}: {formatMoneyMinor(d.data.baseFeeMinor, d.data.currency)}
            </div>
          )}
        </div>
        <Badge tone={statusTone(sub.status)}>{t(`status.${sub.status}`, sub.status)}</Badge>
      </CardHeader>
      <CardBody className="space-y-5">
        <div className="text-xs text-text-muted">
          {t("subscriptions.fields.startDate")}: {sub.startDate}
          {sub.endDate ? ` · ${t("subscriptions.fields.endDate")}: ${sub.endDate}` : ""}
        </div>

        {d.isLoading ? (
          <PageLoader />
        ) : d.data ? (
          <>
            <div>
              <div className="text-sm font-semibold mb-2">{t("portal.subs.operations")}</div>
              {d.data.operations.length ? (
                <Table>
                  <THead>
                    <Tr>
                      <Th>{t("portal.subs.operation")}</Th>
                      <Th>{t("portal.subs.rate")}</Th>
                      <Th>{t("portal.subs.quota")}</Th>
                      <Th>{t("portal.subs.price")}</Th>
                    </Tr>
                  </THead>
                  <TBody>
                    {d.data.operations.map((op) => (
                      <Tr key={op.operationCode}>
                        <Td>
                          <div className="font-medium text-sm">{op.operationName ?? op.operationCode}</div>
                          <div className="font-mono text-xs text-text-muted">{op.operationCode}</div>
                        </Td>
                        <Td className="text-sm">
                          {op.rateLimitPerMinute != null
                            ? t("portal.subs.perMin", { value: op.rateLimitPerMinute })
                            : unlimited}
                        </Td>
                        <Td className="text-sm">
                          {op.monthlyQuota != null ? formatNumber(op.monthlyQuota) : unlimited}
                        </Td>
                        <Td className="text-sm">{formatMoneyMinor(op.unitPriceMinor, op.currency)}</Td>
                      </Tr>
                    ))}
                  </TBody>
                </Table>
              ) : (
                <div className="text-xs text-text-muted">{t("portal.subs.noOperations")}</div>
              )}
            </div>

            <div>
              <div className="text-sm font-semibold mb-2">
                {t("portal.subs.fields")}{" "}
                <span className="text-text-dim font-normal">({d.data.visibleFields.length})</span>
              </div>
              {d.data.visibleFields.length ? (
                <div className="flex flex-wrap gap-1.5">
                  {d.data.visibleFields.map((f) => (
                    <span
                      key={f}
                      className="font-mono text-xs px-2 py-1 rounded-md bg-accent-emerald/[0.08] border border-accent-emerald/20 text-text"
                    >
                      {f}
                    </span>
                  ))}
                </div>
              ) : (
                <div className="text-xs text-text-muted">{t("portal.subs.noFields")}</div>
              )}
            </div>
          </>
        ) : null}
      </CardBody>
    </Card>
  );
}
