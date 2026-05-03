import axios, { type AxiosError } from "axios";
import type { VerifyRequest, VerifyResponse } from "./types";

export function makeClient(baseUrl: string, clientId: string, clientSecret: string) {
  const instance = axios.create({
    baseURL: baseUrl.replace(/\/+$/, ""),
    timeout: 20_000,
    auth: { username: clientId, password: clientSecret },
    headers: { "Content-Type": "application/json" },
  });
  return instance;
}

export async function verifyIdentity(
  baseUrl: string,
  clientId: string,
  clientSecret: string,
  req: VerifyRequest,
): Promise<{ ok: true; data: VerifyResponse; http: number } | { ok: false; status: number; message: string; body?: unknown }> {
  try {
    const client = makeClient(baseUrl, clientId, clientSecret);
    const res = await client.post<VerifyResponse>("/api/v1/verify/identity", req);
    return { ok: true, data: res.data, http: res.status };
  } catch (e) {
    const err = e as AxiosError<any>;
    return {
      ok: false,
      status: err.response?.status ?? 0,
      message:
        err.response?.data?.message ??
        err.response?.data?.error ??
        err.message ??
        "Network error",
      body: err.response?.data,
    };
  }
}
