import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { ArrowDownRight, ArrowUpRight } from "lucide-react";
import { getWallet, getWalletLedger } from "@/api/tenant";
import { Card, CardBody, CardHeader, CardTitle } from "@/components/ui/Card";
import { Badge } from "@/components/ui/Badge";
import { PageLoader } from "@/components/ui/Spinner";
import { formatMoneyMinor, formatDate } from "@/lib/format";
import type { WalletEntryType } from "@/types/api";

const CREDIT: WalletEntryType[] = ["TOPUP", "REVERSAL"];

export function PortalWalletPage() {
  const { t } = useTranslation();
  const walletQ = useQuery({ queryKey: ["t-wallet"], queryFn: getWallet });
  const ledgerQ = useQuery({ queryKey: ["t-wallet-ledger"], queryFn: () => getWalletLedger(0, 50) });

  const balance = walletQ.data?.balanceMinor ?? 0;
  const currency = walletQ.data?.currency ?? "YER";

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold">{t("portal.nav.wallet")}</h1>

      <Card>
        <CardBody className="p-6">
          <div className="text-xs text-text-muted mb-1">{t("wallet.balance")}</div>
          <div className={`text-4xl font-bold ${balance <= 0 ? "text-accent-rose" : ""}`}>
            {formatMoneyMinor(balance, currency)}
          </div>
          <p className="text-xs text-text-dim mt-2">{t("portal.wallet.topUpHint")}</p>
        </CardBody>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>{t("wallet.recentActivity")}</CardTitle>
        </CardHeader>
        <CardBody className="p-0">
          {ledgerQ.isLoading ? (
            <div className="p-6"><PageLoader /></div>
          ) : ledgerQ.data?.content?.length ? (
            <div className="divide-y divide-border/10">
              {ledgerQ.data.content.map((e) => {
                const credit = CREDIT.includes(e.entryType);
                return (
                  <div key={e.id} className="flex items-center justify-between px-4 py-3">
                    <div className="flex items-center gap-2 min-w-0">
                      {credit ? (
                        <ArrowUpRight className="h-4 w-4 text-accent-emerald shrink-0" />
                      ) : (
                        <ArrowDownRight className="h-4 w-4 text-accent-rose shrink-0" />
                      )}
                      <div className="min-w-0">
                        <Badge tone={credit ? "emerald" : "neutral"}>
                          {t(`wallet.entryType.${e.entryType}`, e.entryType)}
                        </Badge>
                        <span className="text-xs text-text-dim ms-2">{formatDate(e.createdAt)}</span>
                      </div>
                    </div>
                    <span className={`text-sm ${credit ? "text-accent-emerald" : "text-text-muted"}`}>
                      {credit ? "+" : ""}
                      {formatMoneyMinor(e.amountMinor, e.currency)}
                    </span>
                  </div>
                );
              })}
            </div>
          ) : (
            <div className="px-6 py-8 text-center text-xs text-text-muted">{t("wallet.noActivity")}</div>
          )}
        </CardBody>
      </Card>
    </div>
  );
}
