import { create } from "zustand";
import { persist } from "zustand/middleware";

export type AdminRole = "SUPER_ADMIN" | "PLATFORM_OPS" | "FINANCE" | "AUDITOR";

export type AuthState = {
  token: string | null;
  email: string | null;
  role: AdminRole | null;
  expiresAt: number | null;
  setSession: (session: {
    token: string;
    email: string;
    role: AdminRole;
    expiresInSeconds: number;
  }) => void;
  clear: () => void;
  isAuthenticated: () => boolean;
};

export const useAuth = create<AuthState>()(
  persist(
    (set, get) => ({
      token: null,
      email: null,
      role: null,
      expiresAt: null,
      setSession: ({ token, email, role, expiresInSeconds }) =>
        set({
          token,
          email,
          role,
          expiresAt: Date.now() + expiresInSeconds * 1000,
        }),
      clear: () =>
        set({ token: null, email: null, role: null, expiresAt: null }),
      isAuthenticated: () => {
        const s = get();
        if (!s.token || !s.expiresAt) return false;
        return s.expiresAt > Date.now();
      },
    }),
    {
      name: "middleware-portal-auth",
    },
  ),
);
