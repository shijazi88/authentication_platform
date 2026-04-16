import { api } from "@/lib/api";
import type {
  AddPlanEntitlementRequest,
  CreatePlanRequest,
  Plan,
  PlanEntitlement,
  PlanFieldEntitlement,
} from "@/types/api";

export async function listPlans(): Promise<Plan[]> {
  const { data } = await api.get<Plan[]>("/admin/plans");
  return data;
}

export async function getPlan(id: string): Promise<Plan> {
  const { data } = await api.get<Plan>(`/admin/plans/${id}`);
  return data;
}

export async function createPlan(req: CreatePlanRequest): Promise<Plan> {
  const { data } = await api.post<Plan>("/admin/plans", req);
  return data;
}

export async function listPlanEntitlements(
  planId: string,
): Promise<PlanEntitlement[]> {
  const { data } = await api.get<PlanEntitlement[]>(
    `/admin/plans/${planId}/entitlements`,
  );
  return data;
}

export async function listPlanFieldEntitlements(
  planId: string,
): Promise<PlanFieldEntitlement[]> {
  const { data } = await api.get<PlanFieldEntitlement[]>(
    `/admin/plans/${planId}/field-entitlements`,
  );
  return data;
}

export async function addPlanEntitlement(
  planId: string,
  req: AddPlanEntitlementRequest,
): Promise<PlanEntitlement> {
  const { data } = await api.post<PlanEntitlement>(
    `/admin/plans/${planId}/entitlements`,
    req,
  );
  return data;
}
