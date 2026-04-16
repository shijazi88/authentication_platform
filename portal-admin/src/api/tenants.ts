import { api } from "@/lib/api";
import type {
  ApiCredential,
  CreateCredentialRequest,
  CreateTenantRequest,
  Tenant,
  TenantStatus,
} from "@/types/api";

export async function listTenants(): Promise<Tenant[]> {
  const { data } = await api.get<Tenant[]>("/admin/tenants");
  return data;
}

export async function getTenant(id: string): Promise<Tenant> {
  const { data } = await api.get<Tenant>(`/admin/tenants/${id}`);
  return data;
}

export async function createTenant(req: CreateTenantRequest): Promise<Tenant> {
  const { data } = await api.post<Tenant>("/admin/tenants", req);
  return data;
}

export async function setTenantStatus(
  id: string,
  status: TenantStatus,
): Promise<Tenant> {
  const { data } = await api.post<Tenant>(
    `/admin/tenants/${id}/status?status=${status}`,
  );
  return data;
}

export async function issueCredential(
  tenantId: string,
  req: CreateCredentialRequest,
): Promise<ApiCredential> {
  const { data } = await api.post<ApiCredential>(
    `/admin/tenants/${tenantId}/credentials`,
    req,
  );
  return data;
}

export async function revokeCredential(credentialId: string): Promise<void> {
  await api.delete(`/admin/tenants/credentials/${credentialId}`);
}
