import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Wallet as WalletIcon, ScrollText, ListChecks } from "lucide-react";
import { getWallet, listTransactions, listSubscriptions } from "@/api/tenant";
import { Card, CardBody, CardHeader, CardTitle } from "@/components/ui/Card";
import { Badge, statusTone } from "@/components/ui/Badge";
import { PageLoader } from "@/components/ui/Spinner";
import { formatMoneyMinor, formatDate } from "@/lib/format";

export function PortalDashboardPage() {
  const { t } = useTranslation();
  const walletQ = useQuery({ queryKey: ["t-wallet"], queryFn: getWallet });
  const txQ = useQuery({ queryKey: ["t-tx", 0, 5], queryFn: () => listTransactions({ page: 0, size: 5 }) });
  const subsQ = useQuery({ queryKey: ["t-subs"], queryFn: listSubscriptions });

  const balance = walletQ.data?.balanceMinor ?? 0;
  const currency = walletQ.data?.currency ?? "YER";
  const activeSubs = subsQ.data?.filter((s) => s.status === "ACTIVE").length ?? 0;

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">{t("portal.dashboard.title")}</h1>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <Card>
          <CardBody className="p-5">
            <div className="flex items-center gap-2 text-text-muted text-xs mb-2">
              <WalletIcon className="h-4 w-4" /> {t("portal.nav.wallet")}
            </div>
            <div className={`text-2xl font-bold ${balance <= 0 ? "text-accent-rose" : ""}`}>
              {formatMoneyMinor(balance, currency)}
            </div>
            <Link to="/portal/wallet" className="text-xs text-accent-violet hover:underline mt-1 inline-block">
              {t("portal.dashboard.viewWallet")}
            </Link>
          </CardBody>
        </Card>
        <Card>
          <CardBody className="p-5">
            <div className="flex items-center gap-2 text-text-muted text-xs mb-2">
              <ListChecks className="h-4 w-4" /> {t("portal.nav.subscriptions")}
            </div>
            <div className="text-2xl font-bold">{activeSubs}</div>
            <div className="text-xs text-text-dim mt-1">{t("portal.dashboard.activeSubs")}</div>
          </CardBody>
        </Card>
        <Card>
          <CardBody className="p-5">
            <div className="flex items-center gap-2 text-text-muted text-xs mb-2">
              <ScrollText className="h-4 w-4" /> {t("portal.nav.transactions")}
            </div>
            <div className="text-2xl font-bold">{txQ.data?.totalElements ?? 0}</div>
            <div className="text-xs text-text-dim mt-1">{t("portal.dashboard.totalTx")}</div>
          </CardBody>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>{t("portal.dashboard.recentTx")}</CardTitle>
          <Link to="/portal/transactions" className="text-xs text-accent-violet hover:underline">
            {t("common.viewAll")}
          </Link>
        </CardHeader>
        <CardBody className="p-0">
          {txQ.isLoading ? (
            <div className="p-6"><PageLoader /></div>
          ) : txQ.data?.content?.length ? (
            <div className="divide-y divide-border/10">
              {txQ.data.content.map((tx) => (
                <div key={tx.id} className="flex items-center justify-between px-4 py-3">
                  <div className="min-w-0">
                    <div className="font-mono text-xs truncate">{tx.id}</div>
                    <div className="text-xs text-text-muted">{formatDate(tx.createdAt)}</div>
                  </div>
                  <Badge tone={statusTone(tx.status)}>{t(`status.${tx.status}`, tx.status)}</Badge>
                </div>
              ))}
            </div>
          ) : (
            <div className="px-6 py-8 text-center text-xs text-text-muted">
              {t("portal.dashboard.noTx")}
            </div>
          )}
        </CardBody>
      </Card>
    </div>
  );
}
