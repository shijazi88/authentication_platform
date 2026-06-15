import { api } from "@/lib/api";
import type {
  AdminUser,
  CreateUserRequest,
  UpdateUserRequest,
} from "@/types/api";

export async function listUsers(): Promise<AdminUser[]> {
  const { data } = await api.get<AdminUser[]>("/admin/users");
  return data;
}

export async function createUser(req: CreateUserRequest): Promise<AdminUser> {
  const { data } = await api.post<AdminUser>("/admin/users", req);
  return data;
}

export async function updateUser(
  id: string,
  req: UpdateUserRequest,
): Promise<AdminUser> {
  const { data } = await api.put<AdminUser>(`/admin/users/${id}`, req);
  return data;
}

export async function setUserStatus(
  id: string,
  active: boolean,
): Promise<AdminUser> {
  const { data } = await api.put<AdminUser>(
    `/admin/users/${id}/status?active=${active}`,
  );
  return data;
}

export async function resetUserPassword(
  id: string,
  password: string,
): Promise<void> {
  await api.put(`/admin/users/${id}/password`, { password });
}
