import { api } from "@/lib/api";
import type { BillingEvent, Page, PeriodSummary } from "@/types/api";

export async function listBillingEvents(params: {
  tenantId: string;
  period: string;
  page?: number;
  size?: number;
}): Promise<Page<BillingEvent>> {
  const { data } = await api.get<Page<BillingEvent>>("/admin/billing/events", {
    params: {
      tenantId: params.tenantId,
      period: params.period,
      page: params.page ?? 0,
      size: params.size ?? 50,
    },
  });
  return data;
}

export async function getBillingSummary(params: {
  tenantId: string;
  period: string;
}): Promise<PeriodSummary[]> {
  const { data } = await api.get<PeriodSummary[]>("/admin/billing/summary", {
    params,
  });
  return data;
}
