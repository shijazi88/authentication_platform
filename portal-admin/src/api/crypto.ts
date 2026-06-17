import { api } from "@/lib/api";
import type { EncryptionKey, EncryptionCertificate, Tenant } from "@/types/api";

export async function listEncryptionKeys(tenantId: string): Promise<EncryptionKey[]> {
  const { data } = await api.get<EncryptionKey[]>(
    `/admin/tenants/${tenantId}/encryption-keys`,
  );
  return data;
}

export async function getActiveCertificate(tenantId: string): Promise<EncryptionCertificate> {
  const { data } = await api.get<EncryptionCertificate>(
    `/admin/tenants/${tenantId}/encryption-keys/active`,
  );
  return data;
}

export async function rotateEncryptionKey(tenantId: string): Promise<EncryptionCertificate> {
  const { data } = await api.post<EncryptionCertificate>(
    `/admin/tenants/${tenantId}/encryption-keys/rotate`,
  );
  return data;
}

export async function revokeEncryptionKey(tenantId: string, kid: string): Promise<void> {
  await api.post(`/admin/tenants/${tenantId}/encryption-keys/${kid}/revoke`);
}

export async function setEncryptionPolicy(
  tenantId: string,
  required: boolean,
): Promise<Tenant> {
  const { data } = await api.put<Tenant>(
    `/admin/tenants/${tenantId}/encryption-policy?required=${required}`,
  );
  return data;
}
