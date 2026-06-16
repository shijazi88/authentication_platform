import { api } from "@/lib/api";
import type { Wallet, WalletLedgerEntry } from "@/types/api";

type Page<T> = { content: T[]; totalElements: number; number: number; size: number };

export async function getWallet(tenantId: string): Promise<Wallet> {
  const { data } = await api.get<Wallet>(`/admin/wallets/${tenantId}`);
  return data;
}

export async function getWalletLedger(
  tenantId: string,
  page = 0,
  size = 20,
): Promise<Page<WalletLedgerEntry>> {
  const { data } = await api.get<Page<WalletLedgerEntry>>(
    `/admin/wallets/${tenantId}/ledger?page=${page}&size=${size}`,
  );
  return data;
}

export async function topUpWallet(
  tenantId: string,
  amountMinor: number,
  note?: string,
): Promise<Wallet> {
  const { data } = await api.post<Wallet>(`/admin/wallets/${tenantId}/topup`, {
    amountMinor,
    note,
  });
  return data;
}
