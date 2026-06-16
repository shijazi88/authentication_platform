import { api } from "@/lib/api";

export async function getPinStatus(): Promise<{ pinSet: boolean }> {
  const { data } = await api.get<{ pinSet: boolean }>("/admin/auth/pin/status");
  return data;
}

export async function setPin(pin: string, currentPin?: string): Promise<void> {
  await api.put("/admin/auth/pin", { pin, currentPin });
}

export async function verifyPin(
  pin: string,
): Promise<{ unlockToken: string; expiresInSeconds: number }> {
  const { data } = await api.post<{ unlockToken: string; expiresInSeconds: number }>(
    "/admin/auth/pin/verify",
    { pin },
  );
  return data;
}
