import { api } from "@/lib/api";
import type { ImageValidationSettings } from "@/types/api";

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
