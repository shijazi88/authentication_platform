import { NavLink } from "react-router-dom";
import { useTranslation } from "react-i18next";
import {
  LayoutDashboard,
  Building2,
  Layers,
  ListChecks,
  ScrollText,
  Receipt,
  Library,
  BarChart3,
} from "lucide-react";
import { cn } from "@/lib/cn";

const navItems = [
  { to: "/", labelKey: "nav.dashboard", icon: LayoutDashboard, end: true },
  { to: "/tenants", labelKey: "nav.tenants", icon: Building2 },
  { to: "/plans", labelKey: "nav.plans", icon: Layers },
  { to: "/subscriptions", labelKey: "nav.subscriptions", icon: ListChecks },
  { to: "/catalog", labelKey: "nav.catalog", icon: Library },
  { to: "/transactions", labelKey: "nav.transactions", icon: ScrollText },
  { to: "/reports", labelKey: "nav.reports", icon: BarChart3 },
  { to: "/billing", labelKey: "nav.billing", icon: Receipt },
];

export function Sidebar() {
  const { t } = useTranslation();

  return (
    <aside className="w-64 shrink-0 h-screen sticky top-0 glass border-e border-border/10 flex flex-col">
      {/* Brand */}
      <div className="px-5 py-5 border-b border-border/10">
        <div className="text-base font-bold text-gradient">{t("brand.name")}</div>
        <div className="text-[10px] uppercase tracking-wider text-text-muted mt-0.5">
          {t("brand.tagline")}
        </div>
      </div>

      {/* Nav */}
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

      {/* Footer */}
      <div className="px-5 py-4 border-t border-border/10">
        <div className="text-[10px] text-text-dim">v0.1.0 · dev</div>
      </div>
    </aside>
  );
}
