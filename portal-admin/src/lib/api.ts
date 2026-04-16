import axios, { type AxiosError } from "axios";
import { toast } from "sonner";
import { useAuth } from "./auth";

/**
 * Single shared axios instance.
 *
 * <p>Base URL resolution:
 * - Dev: empty — calls use relative paths (`/admin/...`, `/api/...`) which
 *   Vite proxies to the Spring Boot backend on localhost:8080.
 * - Prod: reads {@code VITE_API_URL} baked in at build time (e.g. on Railway:
 *   https://sannad-backend.up.railway.app). Frontend and backend live on
 *   separate origins, so the backend's CORS config must allow this origin.
 *
 * <p>Other behavior:
 * - Adds the JWT bearer token from the auth store on every request.
 * - On 401 responses, clears the auth store and redirects to /login.
 */
const API_BASE = (import.meta.env.VITE_API_URL || "").replace(/\/+$/, "");

export const api = axios.create({
  baseURL: API_BASE,
  headers: { "Content-Type": "application/json" },
  timeout: 30_000,
});

api.interceptors.request.use((config) => {
  const token = useAuth.getState().token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error: AxiosError<{ message?: string; error?: string }>) => {
    const status = error.response?.status;
    const data = error.response?.data;
    const message =
      (typeof data === "object" && data?.message) ||
      error.message ||
      "Unexpected error";

    if (status === 401) {
      useAuth.getState().clear();
      if (
        typeof window !== "undefined" &&
        window.location.pathname !== "/login"
      ) {
        window.location.href = "/login";
      }
    } else if (status && status >= 500) {
      toast.error(`Server error (${status})`, { description: message });
    } else if (status && status >= 400 && status !== 401) {
      toast.error(`Request failed (${status})`, { description: message });
    } else if (!error.response) {
      toast.error("Network error", { description: message });
    }

    return Promise.reject(error);
  },
);
