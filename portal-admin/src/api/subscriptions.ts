import { api } from "@/lib/api";
import type {
  CreateSubscriptionRequest,
  Subscription,
  SubscriptionStatus,
} from "@/types/api";

export async function listSubscriptionsByTenant(
  tenantId: string,
): Promise<Subscription[]> {
  const { data } = await api.get<Subscription[]>("/admin/subscriptions", {
    params: { tenantId },
  });
  return data;
}

export async function createSubscription(
  req: CreateSubscriptionRequest,
): Promise<Subscription> {
  const { data } = await api.post<Subscription>("/admin/subscriptions", req);
  return data;
}

export async function setSubscriptionStatus(
  id: string,
  status: SubscriptionStatus,
): Promise<Subscription> {
  const { data } = await api.post<Subscription>(
    `/admin/subscriptions/${id}/status?status=${status}`,
  );
  return data;
}
