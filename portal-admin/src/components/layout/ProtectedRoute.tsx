import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "@/lib/auth";
import { canAccess } from "@/lib/access";
import { type ReactNode } from "react";

export function ProtectedRoute({ children }: { children: ReactNode }) {
  const isAuthenticated = useAuth((s) => s.isAuthenticated());
  const role = useAuth((s) => s.role);
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }
  // Role gate: send users who lack access to a page they can always see.
  // Dashboard is open to every role, so it's a safe fallback.
  if (!canAccess(role, location.pathname)) {
    return <Navigate to="/dashboard" replace />;
  }
  return <>{children}</>;
}
