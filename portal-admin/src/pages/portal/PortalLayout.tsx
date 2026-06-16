import { NavLink, Navigate, Outlet, useLocation, useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { LayoutDashboard, ScrollText, ListChecks, Wallet, LogOut } from "lucide-react";
import { useTenantAuth } from "@/lib/tenantAuth";
import { cn } from "@/lib/cn";
import { Button } from "@/components/ui/Button";
import { Badge } from "@/components/ui/Badge";
import { BrandLogo } from "@/components/ui/BrandLogo";
import { ThemeToggle } from "@/components/ui/ThemeToggle";
import { LangToggle } from "@/components/ui/LangToggle";

const navItems = [
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
    <div className="min-h-screen flex bg-bg text-text relative">
      {/* Background decoration — same as the admin shell */}
      <div className="pointer-events-none fixed inset-0 bg-gradient-mesh opacity-60" />
      <div className="pointer-events-none fixed inset-0 bg-grid-pattern bg-grid-32 opacity-[0.4] [mask-image:radial-gradient(ellipse_at_top,black_30%,transparent_75%)]" />

      {/* Sidebar */}
      <aside className="w-64 shrink-0 h-screen sticky top-0 glass border-e border-border/10 flex flex-col">
        <div className="px-5 py-4 border-b border-border/10 flex items-center gap-3">
          <BrandLogo size={36} />
          <div className="min-w-0">
            <div className="text-base font-bold text-gradient leading-tight truncate">
              {tenantName ?? "MOTABIQ"}
            </div>
            <div className="text-[10px] uppercase tracking-wider text-text-muted mt-0.5 truncate">
              {t("portal.title")}
            </div>
          </div>
        </div>

        <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
          {navItems.map(({ to, labelKey, icon: Icon, end }) => (
            <NavLink
              key={to}
              to={to}
              end={end}
              className={({ isActive }) =>
                cn(
                  "group flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-all relative",
                  isActive
                    ? "text-text bg-bg-hover/60 border border-border/15"
                    : "text-text-muted hover:text-text hover:bg-bg-hover/40 border border-transparent",
                )
              }
            >
              {({ isActive }) => (
                <>
                  {isActive && (
                    <span className="absolute start-0 top-1/2 -translate-y-1/2 h-5 w-[3px] rounded-e bg-gradient-to-b from-accent-violet to-accent-cyan" />
                  )}
                  <Icon
                    className={cn(
                      "h-4 w-4 shrink-0",
                      isActive ? "text-accent-cyan" : "text-text-dim group-hover:text-text-muted",
                    )}
                  />
                  <span>{t(labelKey)}</span>
                </>
              )}
            </NavLink>
          ))}
        </nav>

        <div className="px-5 py-4 border-t border-border/10">
          <div className="text-[10px] text-text-dim">{t("portal.title")}</div>
        </div>
      </aside>

      {/* Main column */}
      <div className="flex-1 flex flex-col min-w-0 relative">
        <div className="h-16 sticky top-0 z-30 border-b border-border/10 glass flex items-center justify-end px-6 gap-2">
          <LangToggle />
          <ThemeToggle />
          <div className="h-8 w-px bg-border/15 mx-1" />
          <Badge tone="violet">{t("portal.clientBadge")}</Badge>
          <div className="text-xs text-text-muted hidden sm:block ms-2">{email}</div>
          <div className="h-8 w-px bg-border/15 mx-1" />
          <Button variant="ghost" size="sm" leftIcon={<LogOut className="h-4 w-4" />} onClick={logout}>
            {t("portal.logout")}
          </Button>
        </div>
        <main className="flex-1 px-8 py-8 page-enter">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
