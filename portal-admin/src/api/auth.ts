import { api } from "@/lib/api";
import type { LoginRequest, LoginResponse } from "@/types/api";

export async function login(req: LoginRequest): Promise<LoginResponse> {
  const { data } = await api.post<LoginResponse>("/admin/auth/login", req);
  return data;
}
