import { NavLink, Navigate, Outlet, useLocation, useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { LayoutDashboard, ScrollText, ListChecks, Wallet, LogOut } from "lucide-react";
import { useTenantAuth } from "@/lib/tenantAuth";
import { cn } from "@/lib/cn";
import { Button } from "@/components/ui/Button";
import { BrandLogo } from "@/components/ui/BrandLogo";
import { ThemeToggle } from "@/components/ui/ThemeToggle";
import { LangToggle } from "@/components/ui/LangToggle";

const nav = [
  { to: "/portal", labelKey: "portal.nav.dashboard", icon: LayoutDashboard, end: true },
  { to: "/portal/transactions", labelKey: "portal.nav.transactions", icon: ScrollText },
  { to: "/portal/subscriptions", labelKey: "portal.nav.subscriptions", icon: ListChecks },
  { to: "/portal/wallet", labelKey: "portal.nav.wallet", icon: Wallet },
];

export function PortalLayout() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const isAuthed = useTenantAuth((s) => s.isAuthenticated());
  const tenantName = useTenantAuth((s) => s.tenantName);
  const email = useTenantAuth((s) => s.email);
  const clear = useTenantAuth((s) => s.clear);

  if (!isAuthed) {
    return <Navigate to="/portal/login" replace state={{ from: location }} />;
  }

  function logout() {
    clear();
    navigate("/portal/login");
  }

  return (
    <div className="min-h-screen bg-bg text-text">
      <header className="h-16 sticky top-0 z-30 border-b border-border/10 glass flex items-center justify-between px-4 sm:px-6 gap-4">
        <div className="flex items-center gap-3 min-w-0">
          <BrandLogo size={32} />
          <div className="min-w-0">
            <div className="text-sm font-bold truncate">{tenantName ?? "MOTABIQ"}</div>
            <div className="text-[10px] uppercase tracking-wider text-text-muted">
              {t("portal.title")}
            </div>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <LangToggle />
          <ThemeToggle />
          <div className="h-8 w-px bg-border/15 mx-1" />
          <span className="text-xs text-text-muted hidden sm:block">{email}</span>
          <Button variant="ghost" size="sm" leftIcon={<LogOut className="h-4 w-4" />} onClick={logout}>
            {t("portal.logout")}
          </Button>
        </div>
      </header>

      <nav className="sticky top-16 z-20 border-b border-border/10 bg-bg/80 backdrop-blur px-2 sm:px-4 flex gap-1 overflow-x-auto">
        {nav.map(({ to, labelKey, icon: Icon, end }) => (
          <NavLink
            key={to}
            to={to}
            end={end}
            className={({ isActive }) =>
              cn(
                "flex items-center gap-2 px-3 py-3 text-sm font-medium border-b-2 whitespace-nowrap transition-colors",
                isActive
                  ? "border-accent-violet text-text"
                  : "border-transparent text-text-muted hover:text-text",
              )
            }
          >
            <Icon className="h-4 w-4" />
            {t(labelKey)}
          </NavLink>
        ))}
      </nav>

      <main className="max-w-6xl mx-auto p-4 sm:p-6">
        <Outlet />
      </main>
    </div>
  );
}
