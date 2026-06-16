import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { listSubscriptions } from "@/api/tenant";
import { Card, CardBody } from "@/components/ui/Card";
import { Badge, statusTone } from "@/components/ui/Badge";
import { PageLoader } from "@/components/ui/Spinner";

export function PortalSubscriptionsPage() {
  const { t } = useTranslation();
  const q = useQuery({ queryKey: ["t-subs"], queryFn: listSubscriptions });

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold">{t("portal.nav.subscriptions")}</h1>
      {q.isLoading ? (
        <PageLoader />
      ) : q.data?.length ? (
        <div className="space-y-2">
          {q.data.map((s) => (
            <Card key={s.id}>
              <CardBody className="p-4 flex items-center justify-between">
                <div>
                  <div className="text-sm font-mono">{s.planId}</div>
                  <div className="text-xs text-text-muted mt-0.5">
                    {t("subscriptions.fields.startDate")}: {s.startDate}
                    {s.endDate ? ` · ${t("subscriptions.fields.endDate")}: ${s.endDate}` : ""}
                  </div>
                </div>
                <Badge tone={statusTone(s.status)}>{t(`status.${s.status}`, s.status)}</Badge>
              </CardBody>
            </Card>
          ))}
        </div>
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
