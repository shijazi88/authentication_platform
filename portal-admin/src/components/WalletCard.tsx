import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";
import { Wallet as WalletIcon, Plus, ArrowDownRight, ArrowUpRight } from "lucide-react";
import { getWallet, getWalletLedger, topUpWallet } from "@/api/wallet";
import { Card, CardBody, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Dialog } from "@/components/ui/Dialog";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { Badge } from "@/components/ui/Badge";
import { formatMoneyMinor, formatDate } from "@/lib/format";
import type { WalletEntryType } from "@/types/api";

const CREDIT_TYPES: WalletEntryType[] = ["TOPUP", "REVERSAL"];

/** Wallet panel for a tenant: balance, top-up, and recent ledger entries. */
export function WalletCard({ tenantId }: { tenantId: string }) {
  const { t } = useTranslation();
  const qc = useQueryClient();
  const [open, setOpen] = useState(false);
  const [amount, setAmount] = useState("");
  const [note, setNote] = useState("");

  const walletQ = useQuery({
    queryKey: ["wallet", tenantId],
    queryFn: () => getWallet(tenantId),
  });
  const ledgerQ = useQuery({
    queryKey: ["wallet-ledger", tenantId],
    queryFn: () => getWalletLedger(tenantId, 0, 8),
  });

  const currency = walletQ.data?.currency ?? "YER";

  const topUpMut = useMutation({
    mutationFn: () => topUpWallet(tenantId, Math.round(Number(amount) * 100), note || undefined),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["wallet", tenantId] });
      qc.invalidateQueries({ queryKey: ["wallet-ledger", tenantId] });
      toast.success(t("wallet.toppedUp"));
      setOpen(false);
      setAmount("");
      setNote("");
    },
    onError: (e: any) =>
      toast.error(e?.response?.data?.message ?? t("wallet.error")),
  });

  const balance = walletQ.data?.balanceMinor ?? 0;

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center gap-2">
          <WalletIcon className="h-4 w-4 text-text-muted" />
          <CardTitle>{t("wallet.title")}</CardTitle>
        </div>
        <Button
          variant="ghost"
          size="sm"
          leftIcon={<Plus className="h-3.5 w-3.5" />}
          onClick={() => setOpen(true)}
        >
          {t("wallet.topUp")}
        </Button>
      </CardHeader>
      <CardBody>
        <div className="mb-1 text-xs text-text-muted">{t("wallet.balance")}</div>
        <div
          className={`text-3xl font-bold ${balance <= 0 ? "text-accent-rose" : "text-text"}`}
        >
          {formatMoneyMinor(balance, currency)}
        </div>

        <div className="mt-4 border-t border-border/10 pt-3">
          <div className="text-xs text-text-muted mb-2">{t("wallet.recentActivity")}</div>
          {ledgerQ.data?.content?.length ? (
            <div className="space-y-1.5">
              {ledgerQ.data.content.map((e) => {
                const credit = CREDIT_TYPES.includes(e.entryType);
                return (
                  <div key={e.id} className="flex items-center justify-between text-xs">
                    <div className="flex items-center gap-2 min-w-0">
                      {credit ? (
                        <ArrowUpRight className="h-3.5 w-3.5 text-accent-emerald shrink-0" />
                      ) : (
                        <ArrowDownRight className="h-3.5 w-3.5 text-accent-rose shrink-0" />
                      )}
                      <Badge tone={credit ? "emerald" : "neutral"}>
                        {t(`wallet.entryType.${e.entryType}`, e.entryType)}
                      </Badge>
                      <span className="text-text-dim truncate">{formatDate(e.createdAt)}</span>
                    </div>
                    <span className={credit ? "text-accent-emerald" : "text-text-muted"}>
                      {credit ? "+" : ""}
                      {formatMoneyMinor(e.amountMinor, e.currency)}
                    </span>
                  </div>
                );
              })}
            </div>
          ) : (
            <div className="text-xs text-text-dim py-2">{t("wallet.noActivity")}</div>
          )}
        </div>
      </CardBody>

      <Dialog
        open={open}
        onClose={() => setOpen(false)}
        title={t("wallet.topUp")}
        footer={
          <>
            <Button variant="ghost" onClick={() => setOpen(false)}>
              {t("common.cancel")}
            </Button>
            <Button
              loading={topUpMut.isPending}
              disabled={!(Number(amount) > 0)}
              onClick={() => topUpMut.mutate()}
            >
              {t("wallet.topUp")}
            </Button>
          </>
        }
      >
        <div className="space-y-4">
          <div>
            <Label htmlFor="amount">{t("wallet.amount", { currency })}</Label>
            <Input
              id="amount"
              type="number"
              min={1}
              placeholder="100000"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
            />
            {Number(amount) > 0 && (
              <p className="mt-1 text-xs text-text-dim">
                {t("wallet.willAdd", {
                  value: formatMoneyMinor(Math.round(Number(amount) * 100), currency),
                })}
              </p>
            )}
          </div>
          <div>
            <Label htmlFor="note">{t("common.description")}</Label>
            <Input
              id="note"
              placeholder={t("wallet.notePlaceholder")}
              value={note}
              onChange={(e) => setNote(e.target.value)}
            />
          </div>
        </div>
      </Dialog>
    </Card>
  );
}
