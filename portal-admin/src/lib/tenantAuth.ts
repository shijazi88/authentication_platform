import { create } from "zustand";
import { persist } from "zustand/middleware";

/** Auth store for the tenant-facing portal — fully separate from the admin store. */
export type TenantAuthState = {
  token: string | null;
  email: string | null;
  tenantId: string | null;
  tenantName: string | null;
  expiresAt: number | null;
  setSession: (s: {
    token: string;
    email: string;
    tenantId: string;
    tenantName: string;
    expiresInSeconds: number;
  }) => void;
  clear: () => void;
  isAuthenticated: () => boolean;
};

export const useTenantAuth = create<TenantAuthState>()(
  persist(
    (set, get) => ({
      token: null,
      email: null,
      tenantId: null,
      tenantName: null,
      expiresAt: null,
      setSession: ({ token, email, tenantId, tenantName, expiresInSeconds }) =>
        set({
          token,
          email,
          tenantId,
          tenantName,
          expiresAt: Date.now() + expiresInSeconds * 1000,
        }),
      clear: () =>
        set({
          token: null,
          email: null,
          tenantId: null,
          tenantName: null,
          expiresAt: null,
        }),
      isAuthenticated: () => {
        const s = get();
        if (!s.token || !s.expiresAt) return false;
        return s.expiresAt > Date.now();
      },
    }),
    { name: "sanad-tenant-auth" },
  ),
);
