import { api } from "@/lib/api";
import type { Ticket, TicketCounts, TicketPriority, TicketStatus } from "@/types/api";

type Page<T> = { content: T[]; totalElements: number; number: number; size: number };

export type TicketFilters = {
  /** A specific status, or "ACTIVE" (default) for everything not resolved/closed, or "ALL". */
  status?: TicketStatus | "ACTIVE" | "ALL";
  tenantId?: string;
  q?: string;
  page?: number;
  size?: number;
};

export async function listTickets(f: TicketFilters = {}): Promise<Page<Ticket>> {
  const params = new URLSearchParams();
  const status = f.status ?? "ACTIVE";
  if (status === "ALL") params.set("active", "false");
  else if (status !== "ACTIVE") params.set("status", status);
  if (f.tenantId) params.set("tenantId", f.tenantId);
  if (f.q?.trim()) params.set("q", f.q.trim());
  params.set("page", String(f.page ?? 0));
  params.set("size", String(f.size ?? 25));
  const { data } = await api.get<Page<Ticket>>(`/admin/tickets?${params}`);
  return data;
}

export async function getTicketCounts(): Promise<TicketCounts> {
  const { data } = await api.get<TicketCounts>("/admin/tickets/counts");
  return data;
}

export async function getTicket(id: string): Promise<Ticket> {
  const { data } = await api.get<Ticket>(`/admin/tickets/${id}`);
  return data;
}

/** Public reply (status defaults to WAITING_ON_CLIENT) or an internal note. */
export async function replyToTicket(
  id: string,
  message: string,
  internal: boolean,
  status?: TicketStatus,
): Promise<Ticket> {
  const { data } = await api.post<Ticket>(`/admin/tickets/${id}/messages`, { message, internal, status });
  return data;
}

/** Omitted fields stay as they are; assignedTo "" unassigns. */
export async function updateTicket(
  id: string,
  patch: { status?: TicketStatus; priority?: TicketPriority; assignedTo?: string },
): Promise<Ticket> {
  const { data } = await api.put<Ticket>(`/admin/tickets/${id}`, patch);
  return data;
}
