import { api } from "@/lib/api";
import type { Page, Transaction } from "@/types/api";

export async function listTransactions(params: {
  tenantId: string;
  page?: number;
  size?: number;
}): Promise<Page<Transaction>> {
  const { data } = await api.get<Page<Transaction>>("/admin/transactions", {
    params: {
      tenantId: params.tenantId,
      page: params.page ?? 0,
      size: params.size ?? 50,
    },
  });
  return data;
}

export async function getTransaction(id: string): Promise<Transaction> {
  const { data } = await api.get<Transaction>(`/admin/transactions/${id}`);
  return data;
}
