import { tenantApi } from "@/lib/tenantApi";
import type {
  Subscription,
  Transaction,
  Wallet,
  WalletLedgerEntry,
} from "@/types/api";

type Page<T> = { content: T[]; totalElements: number; number: number; size: number };

export type TenantLoginResponse = {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
  tenantId: string;
  tenantName: string;
  email: string;
  displayName: string | null;
};

export type TenantMe = {
  email: string;
  displayName: string | null;
  tenantId: string;
  tenantCode: string;
  tenantName: string;
};

export async function tenantLogin(
  email: string,
  password: string,
): Promise<TenantLoginResponse> {
  const { data } = await tenantApi.post<TenantLoginResponse>("/portal-api/auth/login", {
    email,
    password,
  });
  return data;
}

export async function getMe(): Promise<TenantMe> {
  const { data } = await tenantApi.get<TenantMe>("/portal-api/me");
  return data;
}

export async function listTransactions(page = 0, size = 20): Promise<Page<Transaction>> {
  const { data } = await tenantApi.get<Page<Transaction>>(
    `/portal-api/transactions?page=${page}&size=${size}`,
  );
  return data;
}

export async function listSubscriptions(): Promise<Subscription[]> {
  const { data } = await tenantApi.get<Subscription[]>("/portal-api/subscriptions");
  return data;
}

export async function getWallet(): Promise<Wallet> {
  const { data } = await tenantApi.get<Wallet>("/portal-api/wallet");
  return data;
}

export async function getWalletLedger(page = 0, size = 20): Promise<Page<WalletLedgerEntry>> {
  const { data } = await tenantApi.get<Page<WalletLedgerEntry>>(
    `/portal-api/wallet/ledger?page=${page}&size=${size}`,
  );
  return data;
}
