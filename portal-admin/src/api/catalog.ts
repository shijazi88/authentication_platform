import { api } from "@/lib/api";
import type {
  FieldDefinition,
  ServiceDefinition,
  ServiceOperation,
} from "@/types/api";

export async function listServices(): Promise<ServiceDefinition[]> {
  const { data } = await api.get<ServiceDefinition[]>("/admin/catalog/services");
  return data;
}

export async function listOperations(
  serviceId: string,
): Promise<ServiceOperation[]> {
  const { data } = await api.get<ServiceOperation[]>(
    `/admin/catalog/services/${serviceId}/operations`,
  );
  return data;
}

export async function listFields(
  operationId: string,
): Promise<FieldDefinition[]> {
  const { data } = await api.get<FieldDefinition[]>(
    `/admin/catalog/operations/${operationId}/fields`,
  );
  return data;
}
