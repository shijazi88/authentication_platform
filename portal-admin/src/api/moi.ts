import { api } from "@/lib/api";
import type { MoiApiCallSummary, MoiApiCallDetail } from "@/types/api";

/**
 * List MOI API audit rows. When {@code transactionId} is set, returns only
 * the calls (AUTH + VERIFY) tied to that single Sanad transaction, oldest
 * first — used by the transaction detail page.
 */
export async function listMoiCalls(params: {
  transactionId?: string;
  kind?: "AUTH" | "VERIFY";
  limit?: number;
}): Promise<MoiApiCallSummary[]> {
  const { data } = await api.get<MoiApiCallSummary[]>(
    "/admin/moi-credentials/calls",
    {
      params: {
        ...(params.transactionId ? { transactionId: params.transactionId } : {}),
        ...(params.kind ? { kind: params.kind } : {}),
        ...(params.limit ? { limit: params.limit } : {}),
      },
    },
  );
  return data;
}

/** Full audit row including request/response headers and bodies. */
export async function getMoiCall(id: string): Promise<MoiApiCallDetail> {
  const { data } = await api.get<MoiApiCallDetail>(
    `/admin/moi-credentials/calls/${id}`,
  );
  return data;
}
