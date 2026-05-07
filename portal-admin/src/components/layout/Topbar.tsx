import { LogOut } from "lucide-react";
import { useTranslation } from "react-i18next";
import { useAuth } from "@/lib/auth";
import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/Button";
import { Badge } from "@/components/ui/Badge";
import { ThemeToggle } from "@/components/ui/ThemeToggle";
import { LangToggle } from "@/components/ui/LangToggle";
import { GlobalSearch } from "@/components/layout/GlobalSearch";

export function Topbar() {
  const { t } = useTranslation();
  const { email, role, clear } = useAuth();
  const navigate = useNavigate();

  function logout() {
    clear();
    navigate("/login");
  }

  return (
    <div className="h-16 sticky top-0 z-30 border-b border-border/10 glass flex items-center justify-between px-6 gap-4">
      <GlobalSearch />

      <div className="flex items-center gap-2">
        <LangToggle />
        <ThemeToggle />
        <div className="h-8 w-px bg-border/15 mx-1" />
        <Badge tone="violet">{role ?? "—"}</Badge>
        <div className="text-xs text-text-muted hidden sm:block ms-2">
          {email}
        </div>
        <div className="h-8 w-px bg-border/15 mx-1" />
        <Button
          variant="ghost"
          size="sm"
          onClick={logout}
          leftIcon={<LogOut className="h-4 w-4" />}
        >
          {t("topbar.signOut")}
        </Button>
      </div>
    </div>
  );
}
