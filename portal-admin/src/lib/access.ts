import type { AdminRole } from "@/lib/auth";

/**
 * Role-based access control for the portal. Mirrors the backend access matrix
 * in SecurityConfig — the backend is the real gate; this layer hides routes,
 * nav items and write actions so users don't hit surprise 403s.
 *
 * PLATFORM_OPS is surfaced to users as "Operation". AUDITOR is a legacy
 * read-only role that is no longer assignable.
 */

/** Roles offered in the user-management dropdown (AUDITOR is retired). */
export const OFFERED_ROLES: AdminRole[] = [
  "SUPER_ADMIN",
  "PLATFORM_OPS",
  "FINANCE",
];

const ALL_ROLES: AdminRole[] = [
  "SUPER_ADMIN",
  "PLATFORM_OPS",
  "FINANCE",
  "AUDITOR",
];

/** First path segment → roles allowed to open that route. */
const routeAccess: Record<string, AdminRole[]> = {
  dashboard: ALL_ROLES,
  tenants: ALL_ROLES,
  plans: ALL_ROLES,
  subscriptions: ALL_ROLES,
  transactions: ALL_ROLES,
  reports: ALL_ROLES,
  catalog: ["SUPER_ADMIN", "PLATFORM_OPS"],
  billing: ["SUPER_ADMIN", "FINANCE"],
  users: ["SUPER_ADMIN"],
};

function segmentOf(path: string): string {
  return path.replace(/^\/+/, "").split("/")[0] ?? "";
}

/** May this role open this route path? Unknown routes default to allowed. */
export function canAccess(role: AdminRole | null, path: string): boolean {
  if (!role) return false;
  const allowed = routeAccess[segmentOf(path)];
  return allowed ? allowed.includes(role) : true;
}

/**
 * May this role perform operational writes (create/edit/delete on tenants,
 * plans, subscriptions, catalog)? FINANCE and AUDITOR are read-only there.
 */
export function canWrite(role: AdminRole | null): boolean {
  return role === "SUPER_ADMIN" || role === "PLATFORM_OPS";
}
