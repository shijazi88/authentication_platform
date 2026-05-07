import { api } from "@/lib/api";
import type { Tenant, TransactionStatus } from "@/types/api";

export type TransactionHit = {
  id: string;
  status: TransactionStatus;
  providerRequestId: string | null;
  createdAt: string;
  tenantId: string;
};

export type SearchResults = {
  tenants: Tenant[];
  transactions: TransactionHit[];
};

export async function searchAll(q: string): Promise<SearchResults> {
  const { data } = await api.get<SearchResults>("/admin/search", {
    params: { q },
  });
  return data;
}
