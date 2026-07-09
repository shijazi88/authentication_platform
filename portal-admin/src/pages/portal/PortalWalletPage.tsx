import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";
import { ArrowDownRight, ArrowUpRight, Plus } from "lucide-react";
import {
  getWallet,
  getWalletLedger,
  requestTopUp,
  listMyTopUpRequests,
} from "@/api/tenant";
import { Card, CardBody, CardHeader, CardTitle } from "@/components/ui/Card";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Dialog } from "@/components/ui/Dialog";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { PageLoader } from "@/components/ui/Spinner";
import { formatMoneyMinor, formatDate } from "@/lib/format";
import type { TopUpRequestStatus, WalletEntryType } from "@/types/api";

const CREDIT: WalletEntryType[] = ["TOPUP", "REVERSAL"];
const REQ_TONE: Record<TopUpRequestStatus, "amber" | "emerald" | "rose"> = {
  PENDING: "amber",
  APPROVED: "emerald",
  REJECTED: "rose",
};

export function PortalWalletPage() {
  const { t } = useTranslation();
  const qc = useQueryClient();
  const walletQ = useQuery({ queryKey: ["t-wallet"], queryFn: getWallet });
  const ledgerQ = useQuery({ queryKey: ["t-wallet-ledger"], queryFn: () => getWalletLedger(0, 50) });
  const reqQ = useQuery({ queryKey: ["t-topup-requests"], queryFn: listMyTopUpRequests });

  const [open, setOpen] = useState(false);
  const [amount, setAmount] = useState("");
  const [note, setNote] = useState("");

  const balance = walletQ.data?.balanceMinor ?? 0;
  const currency = walletQ.data?.currency ?? "YER";

  const reqMut = useMutation({
    mutationFn: () => requestTopUp(Math.round(Number(amount) * 100), note || undefined),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["t-topup-requests"] });
      toast.success(t("portal.topup.submitted"));
      setOpen(false);
      setAmount("");
      setNote("");
    },
    onError: (e: any) => toast.error(e?.response?.data?.message ?? t("portal.topup.error")),
  });

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold">{t("portal.nav.wallet")}</h1>

      <Card>
        <CardBody className="p-6 flex items-start justify-between gap-4">
          <div>
            <div className="text-xs text-text-muted mb-1">{t("wallet.balance")}</div>
            <div className={`text-4xl font-bold ${balance <= 0 ? "text-accent-rose" : ""}`}>
              {formatMoneyMinor(balance, currency)}
            </div>
            <p className="text-xs text-text-dim mt-2">{t("portal.topup.hint")}</p>
          </div>
          <Button leftIcon={<Plus className="h-4 w-4" />} onClick={() => setOpen(true)}>
            {t("portal.topup.request")}
          </Button>
        </CardBody>
      </Card>

      {/* My top-up requests */}
      {reqQ.data && reqQ.data.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>{t("portal.topup.myRequests")}</CardTitle>
          </CardHeader>
          <CardBody className="p-0">
            <div className="divide-y divide-border/10">
              {reqQ.data.map((r) => (
                <div key={r.id} className="flex items-center justify-between px-4 py-3 gap-2">
                  <div className="min-w-0">
                    <div className="text-sm font-medium">
                      {formatMoneyMinor(r.amountMinor, r.currency)}
                    </div>
                    <div className="text-xs text-text-muted truncate">
                      {formatDate(r.createdAt)}
                      {r.note ? ` · ${r.note}` : ""}
                      {r.status === "REJECTED" && r.decidedNote ? ` · ${r.decidedNote}` : ""}
                    </div>
                  </div>
                  <Badge tone={REQ_TONE[r.status]}>
                    {t(`portal.topup.status.${r.status}`, r.status)}
                  </Badge>
                </div>
              ))}
            </div>
          </CardBody>
        </Card>
      )}

      {/* Ledger */}
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

      {/* Request dialog */}
      <Dialog
        open={open}
        onClose={() => setOpen(false)}
        title={t("portal.topup.request")}
        footer={
          <>
            <Button variant="ghost" onClick={() => setOpen(false)}>
              {t("common.cancel")}
            </Button>
            <Button
              loading={reqMut.isPending}
              disabled={!(Number(amount) > 0)}
              onClick={() => reqMut.mutate()}
            >
              {t("portal.topup.submit")}
            </Button>
          </>
        }
      >
        <div className="space-y-4">
          <p className="text-xs text-text-muted">{t("portal.topup.dialogHint")}</p>
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
              placeholder={t("portal.topup.notePlaceholder")}
              value={note}
              onChange={(e) => setNote(e.target.value)}
            />
          </div>
        </div>
      </Dialog>
    </div>
  );
}
