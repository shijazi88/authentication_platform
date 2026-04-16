// Type definitions matching the Spring Boot DTOs.

export type TenantStatus =
  | "PENDING"
  | "ACTIVE"
  | "SUSPENDED"
  | "TERMINATED";

export type SubscriptionStatus =
  | "PENDING"
  | "ACTIVE"
  | "SUSPENDED"
  | "CANCELED"
  | "EXPIRED";

export type TransactionStatus =
  | "INITIATED"
  | "SUCCESS"
  | "FAILED"
  | "TIMEOUT"
  | "REJECTED";

export type AdminRole =
  | "SUPER_ADMIN"
  | "PLATFORM_OPS"
  | "FINANCE"
  | "AUDITOR";

export type LoginRequest = { email: string; password: string };
export type LoginResponse = {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
  role: AdminRole;
};

export type Tenant = {
  id: string;
  code: string;
  legalName: string;
  contactEmail: string | null;
  status: TenantStatus;
  createdAt: string;
};

export type CreateTenantRequest = {
  code: string;
  legalName: string;
  contactEmail?: string;
};

export type ApiCredential = {
  id: string;
  tenantId: string;
  clientId: string;
  clientSecret: string;
  label: string | null;
  createdAt: string;
};

export type CreateCredentialRequest = {
  label?: string;
  ipAllowlist?: string;
};

export type ServiceDefinition = {
  id: string;
  code: string;
  name: string;
  description: string | null;
  connectorKey: string;
  active: boolean;
  createdAt: string;
};

export type ServiceOperation = {
  id: string;
  serviceId: string;
  code: string;
  name: string;
  description: string | null;
  defaultUnitPriceMinor: number;
  currency: string;
  active: boolean;
  createdAt: string;
};

export type FieldDefinition = {
  id: string;
  operationId: string;
  path: string;
  dataClass: string | null;
  description: string | null;
};

export type Plan = {
  id: string;
  code: string;
  name: string;
  description: string | null;
  baseFeeMinor: number;
  currency: string;
  active: boolean;
  createdAt: string;
};

export type CreatePlanRequest = {
  code: string;
  name: string;
  description?: string;
  baseFeeMinor: number;
  currency: string;
};

export type PlanEntitlement = {
  id: string;
  planId: string;
  operationId: string;
  unitPriceOverrideMinor: number | null;
  monthlyQuota: number | null;
  rateLimitPerMinute: number | null;
};

export type PlanFieldEntitlement = {
  id: string;
  planId: string;
  fieldId: string;
  fieldPath: string;
};

export type AddPlanEntitlementRequest = {
  serviceCode: string;
  operationCode: string;
  unitPriceOverrideMinor?: number;
  monthlyQuota?: number;
  rateLimitPerMinute?: number;
  visibleFieldPaths?: string[];
};

export type Subscription = {
  id: string;
  tenantId: string;
  planId: string;
  startDate: string;
  endDate: string | null;
  status: SubscriptionStatus;
  createdAt: string;
};

export type CreateSubscriptionRequest = {
  tenantId: string;
  planId: string;
  startDate: string;
  endDate?: string;
};

export type Transaction = {
  id: string;
  tenantId: string;
  credentialId: string;
  subscriptionId: string | null;
  serviceId: string;
  operationId: string;
  status: TransactionStatus;
  providerRequestId: string | null;
  latencyMs: number | null;
  errorCode: number | null;
  errorMessage: string | null;
  unitPriceMinor: number | null;
  currency: string | null;
  billable: boolean;
  createdAt: string;
};

export type Page<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
};

export type ReportRow = {
  period: string;            // YYYY-MM-DD (daily) or YYYY-MM (monthly)
  total: number;
  successCount: number;
  failedCount: number;
  amountMinor: number;
  currency: string;
};

export type ReportTotals = {
  totalTransactions: number;
  successCount: number;
  failedCount: number;
  amountMinor: number;
  currency: string;
  successRate: number;       // 0..1
};

export type ReportSummary = {
  groupBy: "daily" | "monthly";
  from: string;
  to: string;
  rows: ReportRow[];
  totals: ReportTotals;
};

export type ReportGroupBy = "daily" | "monthly";

export type BillingEvent = {
  id: string;
  transactionId: string;
  tenantId: string;
  subscriptionId: string;
  serviceId: string;
  operationId: string;
  unitPriceMinor: number;
  amountMinor: number;
  currency: string;
  period: string;
  occurredAt: string;
};

export type PeriodSummary = {
  tenantId: string;
  period: string;
  currency: string;
  totalAmountMinor: number;
  transactionCount: number;
};
