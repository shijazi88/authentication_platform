import { api } from "@/lib/api";
import type { TenantPortalUser } from "@/types/api";

export async function listTenantUsers(tenantId: string): Promise<TenantPortalUser[]> {
  const { data } = await api.get<TenantPortalUser[]>(`/admin/tenants/${tenantId}/portal-users`);
  return data;
}

export async function createTenantUser(
  tenantId: string,
  req: { email: string; password: string; displayName?: string },
): Promise<TenantPortalUser> {
  const { data } = await api.post<TenantPortalUser>(
    `/admin/tenants/${tenantId}/portal-users`,
    req,
  );
  return data;
}

export async function setTenantUserStatus(
  tenantId: string,
  id: string,
  active: boolean,
): Promise<TenantPortalUser> {
  const { data } = await api.put<TenantPortalUser>(
    `/admin/tenants/${tenantId}/portal-users/${id}/status?active=${active}`,
  );
  return data;
}

export async function resetTenantUserPassword(
  tenantId: string,
  id: string,
  password: string,
): Promise<void> {
  await api.put(`/admin/tenants/${tenantId}/portal-users/${id}/password`, { password });
}
