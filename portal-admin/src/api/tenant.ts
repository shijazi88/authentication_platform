import { tenantApi } from "@/lib/tenantApi";
import type {
  FingerprintDevice,
  WalletTopUpRequest,
  Subscription,
  Transaction,
  TransactionStatus,
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

/** Update the signed-in user's own display name. */
export async function updateProfile(displayName: string | null): Promise<TenantMe> {
  const { data } = await tenantApi.put<TenantMe>("/portal-api/me", { displayName });
  return data;
}

/** Change the signed-in user's own password (requires the current one). */
export async function changePassword(
  currentPassword: string,
  newPassword: string,
): Promise<void> {
  await tenantApi.post("/portal-api/me/password", { currentPassword, newPassword });
}

export type TransactionFilters = {
  status?: TransactionStatus;
  errorCode?: number;
  billable?: boolean;
  exception?: boolean;
  q?: string;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
};

export async function listTransactions(
  params: TransactionFilters = {},
): Promise<Page<Transaction>> {
  const { data } = await tenantApi.get<Page<Transaction>>("/portal-api/transactions", {
    params: {
      status: params.status,
      errorCode: params.errorCode,
      billable: params.billable,
      exception: params.exception,
      q: params.q || undefined,
      from: params.from || undefined,
      to: params.to || undefined,
      page: params.page ?? 0,
      size: params.size ?? 20,
    },
  });
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

// ── API credentials (keys) ────────────────────────────────────────────────

export type CredentialView = {
  id: string;
  clientId: string;
  label: string | null;
  ipAllowlist: string | null;
  active: boolean;
  createdAt: string;
  lastUsedAt: string | null;
  expiresAt: string | null;
};

export type IssuedCredential = {
  id: string;
  tenantId: string;
  clientId: string;
  clientSecret: string;
  label: string | null;
  createdAt: string;
};

export async function listCredentials(): Promise<CredentialView[]> {
  const { data } = await tenantApi.get<CredentialView[]>("/portal-api/credentials");
  return data;
}

export async function createCredential(
  label?: string,
  ipAllowlist?: string,
): Promise<IssuedCredential> {
  const { data } = await tenantApi.post<IssuedCredential>("/portal-api/credentials", {
    label,
    ipAllowlist,
  });
  return data;
}

export async function revokeCredential(id: string): Promise<void> {
  await tenantApi.post(`/portal-api/credentials/${id}/revoke`);
}

// ── Subscription / plan details ─────────────────────────────────────────────

export type PlanOperationView = {
  operationCode: string;
  operationName: string | null;
  rateLimitPerMinute: number | null;
  monthlyQuota: number | null;
  unitPriceMinor: number;
  currency: string;
};

export type SubscriptionDetail = {
  subscriptionId: string;
  status: string;
  startDate: string | null;
  endDate: string | null;
  planCode: string;
  planName: string;
  planDescription: string | null;
  baseFeeMinor: number;
  currency: string;
  operations: PlanOperationView[];
  visibleFields: string[];
};

export async function getSubscriptionDetails(id: string): Promise<SubscriptionDetail> {
  const { data } = await tenantApi.get<SubscriptionDetail>(
    `/portal-api/subscriptions/${id}/details`,
  );
  return data;
}

// ── Encryption certificate ──────────────────────────────────────────────────

export type EncryptionCertificate = {
  kid: string;
  algorithm: string;
  encryption: string;
  certificatePem: string;
  fingerprintSha256: string;
  expiresAt: string | null;
};

export async function getCertificate(): Promise<EncryptionCertificate> {
  const { data } = await tenantApi.get<EncryptionCertificate>("/portal-api/crypto/certificate");
  return data;
}

export async function rotateCertificate(): Promise<EncryptionCertificate> {
  const { data } = await tenantApi.post<EncryptionCertificate>(
    "/portal-api/crypto/certificate/rotate",
  );
  return data;
}

// ── Fingerprint devices (read-only; managed by admins) ──────────────────────

export async function listMyDevices(): Promise<FingerprintDevice[]> {
  const { data } = await tenantApi.get<FingerprintDevice[]>("/portal-api/devices");
  return data;
}

// ── Wallet top-up requests (client asks, admin approves) ────────────────────

export async function requestTopUp(amountMinor: number, note?: string): Promise<WalletTopUpRequest> {
  const { data } = await tenantApi.post<WalletTopUpRequest>("/portal-api/wallet/topup-request", {
    amountMinor,
    note,
  });
  return data;
}

export async function listMyTopUpRequests(): Promise<WalletTopUpRequest[]> {
  const { data } = await tenantApi.get<WalletTopUpRequest[]>("/portal-api/wallet/topup-requests");
  return data;
}
