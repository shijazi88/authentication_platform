import { api } from "@/lib/api";
import type { DeviceImportResult, FingerprintDevice } from "@/types/api";

export async function listDevices(tenantId: string): Promise<FingerprintDevice[]> {
  const { data } = await api.get<FingerprintDevice[]>(`/admin/tenants/${tenantId}/devices`);
  return data;
}

export async function createDevice(
  tenantId: string,
  req: { name: string; model?: string; type?: string; serialNumber: string },
): Promise<FingerprintDevice> {
  const { data } = await api.post<FingerprintDevice>(
    `/admin/tenants/${tenantId}/devices`,
    req,
  );
  return data;
}

export async function updateDevice(
  tenantId: string,
  id: string,
  req: { name: string; model?: string; type?: string; serialNumber: string },
): Promise<FingerprintDevice> {
  const { data } = await api.put<FingerprintDevice>(
    `/admin/tenants/${tenantId}/devices/${id}`,
    req,
  );
  return data;
}

export async function deleteDevice(tenantId: string, id: string): Promise<void> {
  await api.delete(`/admin/tenants/${tenantId}/devices/${id}`);
}

export async function importDevices(
  tenantId: string,
  file: File,
): Promise<DeviceImportResult> {
  const form = new FormData();
  form.append("file", file);
  const { data } = await api.postForm<DeviceImportResult>(
    `/admin/tenants/${tenantId}/devices/import`,
    form,
  );
  return data;
}

export async function downloadDeviceTemplate(tenantId: string): Promise<void> {
  const res = await api.get(`/admin/tenants/${tenantId}/devices/template`, {
    responseType: "blob",
  });
  const url = URL.createObjectURL(res.data as Blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = "fingerprint-devices-template.xlsx";
  a.click();
  URL.revokeObjectURL(url);
}
