import { api } from "@/lib/api";
import type { Page, Transaction, TransactionDetail } from "@/types/api";

export async function listTransactions(params: {
  tenantId?: string;
  page?: number;
  size?: number;
}): Promise<Page<Transaction>> {
  const { data } = await api.get<Page<Transaction>>("/admin/transactions", {
    params: {
      ...(params.tenantId ? { tenantId: params.tenantId } : {}),
      page: params.page ?? 0,
      size: params.size ?? 50,
    },
  });
  return data;
}

/**
 * Single transaction joined with its bank-facing and provider-facing JSON
 * payloads (from {@code transaction_payloads}). Used by the detail page.
 */
export async function getTransaction(id: string): Promise<TransactionDetail> {
  const { data } = await api.get<TransactionDetail>(`/admin/transactions/${id}`);
  return data;
}
