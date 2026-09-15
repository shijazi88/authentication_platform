import { api } from "@/lib/api";
import type { ImageValidationSettings, ResilienceSettings, BreakerStatus } from "@/types/api";

export type SettingEnvelope<T> = {
  value: T;
  updatedBy: string | null;
  updatedAt: string | null;
};

export async function getImageValidationSettings(): Promise<SettingEnvelope<ImageValidationSettings>> {
  const { data } = await api.get<SettingEnvelope<ImageValidationSettings>>(
    "/admin/settings/image-validation",
  );
  return data;
}

export async function updateImageValidationSettings(
  value: ImageValidationSettings,
): Promise<SettingEnvelope<ImageValidationSettings>> {
  const { data } = await api.put<SettingEnvelope<ImageValidationSettings>>(
    "/admin/settings/image-validation",
    value,
  );
  return data;
}

// ── Service protection (circuit breaker + retry) ─────────────────────────────


export async function getResilienceSettings(): Promise<SettingEnvelope<ResilienceSettings>> {
  const { data } = await api.get<SettingEnvelope<ResilienceSettings>>("/admin/settings/resilience");
  return data;
}

export async function updateResilienceSettings(
  value: ResilienceSettings,
): Promise<SettingEnvelope<ResilienceSettings>> {
  const { data } = await api.put<SettingEnvelope<ResilienceSettings>>("/admin/settings/resilience", value);
  return data;
}

export async function getResilienceStatus(): Promise<BreakerStatus[]> {
  const { data } = await api.get<BreakerStatus[]>("/admin/settings/resilience/status");
  return data;
}

export async function resetCircuitBreaker(connector?: string): Promise<BreakerStatus[]> {
  const { data } = await api.post<BreakerStatus[]>(
    "/admin/settings/resilience/reset",
    null,
    { params: connector ? { connector } : {} },
  );
  return data;
}
