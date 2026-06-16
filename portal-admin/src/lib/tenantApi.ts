import axios, { type AxiosError } from "axios";
import { toast } from "sonner";
import { useTenantAuth } from "./tenantAuth";

/** Axios instance for the tenant portal — carries the tenant JWT, not the admin one. */
const API_BASE = (import.meta.env.VITE_API_URL || "").replace(/\/+$/, "");

export const tenantApi = axios.create({
  baseURL: API_BASE,
  headers: { "Content-Type": "application/json" },
  timeout: 30_000,
});

tenantApi.interceptors.request.use((config) => {
  const token = useTenantAuth.getState().token;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

tenantApi.interceptors.response.use(
  (response) => response,
  (error: AxiosError<{ message?: string }>) => {
    const status = error.response?.status;
    const message =
      error.response?.data?.message || error.message || "Unexpected error";
    if (status === 401) {
      useTenantAuth.getState().clear();
      if (typeof window !== "undefined" && window.location.pathname !== "/portal/login") {
        window.location.href = "/portal/login";
      }
    } else if (status && status >= 500) {
      toast.error(`Server error (${status})`, { description: message });
    } else if (status && status >= 400) {
      toast.error(`Request failed (${status})`, { description: message });
    } else if (!error.response) {
      toast.error("Network error", { description: message });
    }
    return Promise.reject(error);
  },
);
